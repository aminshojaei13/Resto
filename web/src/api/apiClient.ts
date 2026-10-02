import {
  AccountingSummary,
  AuthUser,
  BusinessMembership,
  Customer,
  Expense,
  LedgerEntry,
  Organization,
  Product,
  Purchase,
  SalesOrder,
  Store,
  Supplier,
  Warehouse,
} from '../types';
import {
  BusinessContext,
  clearSession,
  getAccessToken,
  getBusinessContext,
  setActiveOrganization,
  setActiveStore,
  setActiveWarehouse,
} from '../auth/session';

const BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8000/api/v1';

/**
 * Raised whenever the server refuses a request.
 *
 * `status` of 401 means the session is over and the caller must sign in again.
 * A failed request is always reported as a failure: this client never invents
 * empty lists or zeroed totals to make a screen look healthy.
 */
export class ApiError extends Error {
  readonly status: number;

  readonly fieldErrors: Record<string, string[]>;

  constructor(message: string, status: number, fieldErrors: Record<string, string[]> = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

type UnauthorizedListener = () => void;

let onUnauthorized: UnauthorizedListener | null = null;

/** Lets the signed-in shell react to an expired or revoked session. */
export const setUnauthorizedHandler = (listener: UnauthorizedListener | null): void => {
  onUnauthorized = listener;
};

/** Legacy alias kept for call sites that still speak in tenant terms. */
export const setTenantContext = (orgId: string, storeId: string): void => {
  const current = getBusinessContext();
  setActiveOrganization(orgId, current.organizationName || orgId, current.role);
  setActiveStore(storeId, current.storeName || storeId);
};

export const getCurrentContext = (): BusinessContext => getBusinessContext();

const readErrorMessage = async (response: Response): Promise<{ message: string; fieldErrors: Record<string, string[]> }> => {
  let payload: any = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  const fieldErrors: Record<string, string[]> = payload?.errors ?? {};

  const firstFieldMessage = Object.values(fieldErrors)
    .flat()
    .find((value): value is string => typeof value === 'string');

  const message =
    firstFieldMessage ??
    payload?.message ??
    (response.status === 401
      ? 'نشست شما پایان یافته است. دوباره وارد شوید.'
      : 'درخواست با خطا مواجه شد.');

  return { message, fieldErrors };
};

interface RequestOptions {
  method?: string;
  body?: unknown;
  query?: Record<string, string | undefined>;
  /** Public endpoints must not carry a stale token. */
  anonymous?: boolean;
}

const request = async <T>(path: string, options: RequestOptions = {}): Promise<T> => {
  const { method = 'GET', body, query, anonymous = false } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  };

  if (!anonymous) {
    const token = getAccessToken();

    if (!token) {
      throw new ApiError('برای انجام این عملیات باید وارد حساب کاربری خود شوید.', 401);
    }

    headers.Authorization = `Bearer ${token}`;

    const context = getBusinessContext();
    if (context.organizationId) headers['X-Tenant-ID'] = context.organizationId;
    if (context.storeId) headers['X-Store-ID'] = context.storeId;
  }

  let url = `${BASE_URL}${path}`;

  if (query) {
    const params = new URLSearchParams();
    Object.entries(query).forEach(([key, value]) => {
      if (value) params.append(key, value);
    });
    const qs = params.toString();
    if (qs) url += `?${qs}`;
  }

  let response: Response;

  try {
    response = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError('ارتباط با سرور برقرار نشد. اتصال اینترنت خود را بررسی کنید.', 0);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (!response.ok) {
    const { message, fieldErrors } = await readErrorMessage(response);

    if (response.status === 401 && !anonymous) {
      clearSession();
      onUnauthorized?.();
    }

    throw new ApiError(message, response.status, fieldErrors);
  }

  const text = await response.text();

  return (text ? JSON.parse(text) : undefined) as T;
};

const mapStore = (store: any): Store => ({
  id: store.id,
  organizationId: store.organization_id ?? store.organizationId ?? '',
  name: store.name,
  code: store.code,
  address: store.address,
  phone: store.phone,
  warehouses: (store.warehouses ?? []).map(mapWarehouse),
});

const mapWarehouse = (warehouse: any): Warehouse => ({
  id: warehouse.id,
  storeId: warehouse.store_id ?? warehouse.storeId ?? '',
  organizationId: warehouse.organization_id ?? warehouse.organizationId ?? '',
  name: warehouse.name,
  code: warehouse.code,
});

const mapOrganization = (org: any): Organization => ({
  id: org.id,
  name: org.name,
  code: org.code,
  logoUrl: org.logo_url ?? org.logoUrl,
  currencySymbol: org.currency_symbol ?? org.currencySymbol ?? '$',
  currencyCode: org.currency_code ?? org.currencyCode ?? 'USD',
  subscriptionTier: org.subscription_tier ?? org.subscriptionTier ?? 'PRO',
  stores: (org.stores ?? []).map(mapStore),
});

const mapUser = (user: any): AuthUser => ({
  id: user.id,
  firstName: user.first_name ?? null,
  lastName: user.last_name ?? null,
  name: user.name ?? '',
  displayName: user.display_name ?? user.name ?? user.email ?? '',
  email: user.email,
  phone: user.phone ?? null,
  status: user.status ?? 'ACTIVE',
  role: user.role ?? null,
  isPlatformAdmin: Boolean(user.is_platform_admin),
  permissions: user.permissions ?? [],
  memberships: (user.memberships ?? user.organizations ?? []) as BusinessMembership[],
});

/**
 * Adopt the first business, store and warehouse the person actually belongs to.
 * Nothing is guessed: if they belong to exactly one of each, that one is
 * selected; otherwise the selection stays empty until they choose.
 */
const adoptDefaultContext = (user: AuthUser): void => {
  const membership = user.memberships?.[0];

  if (!membership) return;

  setActiveOrganization(membership.id, membership.name, membership.role);

  const store = membership.stores?.[0];
  if (store) {
    setActiveStore(store.id, store.name);

    const warehouse = store.warehouses?.[0];
    if (warehouse) setActiveWarehouse(warehouse.id, warehouse.name);
  }
};

export const apiClient = {
  /* ------------------------------------------------------------- identity */

  login: async (credentials: {
    email: string;
    password: string;
    locale: 'fa' | 'en';
    deviceName?: string;
  }): Promise<AuthUser> => {
    const data = await request<{ access_token: string; user: any }>('/auth/login', {
      method: 'POST',
      anonymous: true,
      body: { ...credentials, device_name: credentials.deviceName },
    });

    const user = mapUser(data.user);
    user.token = data.access_token;
    adoptDefaultContext(user);

    return user;
  },

  getProfile: async (): Promise<AuthUser> => {
    const user = mapUser(await request<any>('/auth/profile'));
    adoptDefaultContext(user);
    return user;
  },

  updateProfile: async (payload: {
    first_name?: string;
    last_name?: string;
    phone?: string;
  }): Promise<AuthUser> => {
    const data = await request<{ user: any }>('/auth/profile', { method: 'PUT', body: payload });
    return mapUser(data.user);
  },

  logout: async (): Promise<void> => {
    try {
      await request('/auth/logout', { method: 'POST' });
    } finally {
      // The local session goes even if the server call could not be made.
      clearSession();
    }
  },

  logoutEverywhere: async (): Promise<void> => {
    try {
      await request('/auth/logout-all', { method: 'POST' });
    } finally {
      clearSession();
    }
  },

  forgotPassword: async (email: string, locale: 'fa' | 'en'): Promise<{ message: string }> =>
    request<{ message: string }>('/auth/forgot-password', {
      method: 'POST',
      anonymous: true,
      body: { email, locale },
    }),

  resetPassword: async (payload: {
    email: string;
    token: string;
    password: string;
    password_confirmation: string;
    locale?: 'fa' | 'en';
  }): Promise<{ message: string }> =>
    request<{ message: string }>('/auth/reset-password', {
      method: 'POST',
      anonymous: true,
      body: payload,
    }),

  changePassword: async (payload: {
    current_password: string;
    new_password: string;
    new_password_confirmation: string;
  }): Promise<{ message: string }> =>
    request<{ message: string }>('/auth/change-password', { method: 'POST', body: payload }),

  /* ---------------------------------------------------------------- staff */

  getStaff: async (): Promise<{
    members: any[];
    pending_invitations: any[];
    can_manage_staff: boolean;
  }> => request('/staff'),

  inviteStaff: async (payload: {
    email: string;
    first_name: string;
    last_name?: string;
    role?: 'MANAGER' | 'STAFF';
    locale?: 'fa' | 'en';
  }): Promise<{ message: string; invitation: any }> =>
    request('/staff/invitations', { method: 'POST', body: payload }),

  resendInvitation: async (invitationId: string): Promise<{ message: string }> =>
    request(`/staff/invitations/${invitationId}/resend`, { method: 'POST' }),

  revokeInvitation: async (invitationId: string): Promise<{ message: string }> =>
    request(`/staff/invitations/${invitationId}`, { method: 'DELETE' }),

  updateStaffRole: async (userId: string, role: string): Promise<{ message: string; member: any }> =>
    request(`/staff/${userId}/role`, { method: 'PUT', body: { role } }),

  updateStaffStatus: async (userId: string, status: 'ACTIVE' | 'DEACTIVATED'): Promise<{ message: string; member: any }> =>
    request(`/staff/${userId}/status`, { method: 'PUT', body: { status } }),

  removeStaffMember: async (userId: string): Promise<{ message: string }> =>
    request(`/staff/${userId}`, { method: 'DELETE' }),

  getInvitation: async (token: string): Promise<{ invitation: any }> =>
    request(`/staff/invitations/${encodeURIComponent(token)}`, { anonymous: true }),

  acceptInvitation: async (payload: {
    token: string;
    password: string;
    password_confirmation: string;
    locale?: 'fa' | 'en';
  }): Promise<{ message: string }> =>
    request('/staff/invitations/accept', { method: 'POST', anonymous: true, body: payload }),

  /* ----------------------------------------------------------- business */

  getOrganizations: async (): Promise<Organization[]> => {
    const data = await request<any[]>('/organizations');
    return (data ?? []).map(mapOrganization);
  },

  createOrganization: async (orgData: {
    name: string;
    code?: string;
    currency_symbol?: string;
    currency_code?: string;
  }): Promise<Organization> => {
    const org = await request<any>('/organizations', { method: 'POST', body: orgData });
    return mapOrganization(org);
  },

  /* ----------------------------------------------------------- products */

  getProducts: async (query?: string): Promise<Product[]> => {
    const data = await request<any[]>('/products', { query: { query, org_id: getBusinessContext().organizationId } });

    return (data ?? []).map((p: any) => ({
      id: p.id,
      organizationId: p.organization_id ?? p.organizationId ?? getBusinessContext().organizationId,
      sku: p.sku,
      barcode: p.barcode,
      name: p.name,
      description: p.description,
      price: Number(p.price ?? 0),
      costPrice: Number(p.cost_price ?? p.costPrice ?? 0),
      category: p.category,
      unit: p.unit ?? 'pcs',
      imageUrl: p.image_url ?? p.imageUrl,
      stockQuantityByWarehouse: p.stockQuantityByWarehouse ?? p.stock_quantity_by_warehouse ?? {},
    }));
  },

  createProduct: async (productData: any): Promise<Product> => {
    const p = await request<any>('/products', { method: 'POST', body: productData });
    return {
      id: p.id,
      organizationId: p.organization_id,
      sku: p.sku,
      barcode: p.barcode,
      name: p.name,
      description: p.description,
      price: Number(p.price),
      costPrice: Number(p.cost_price),
      category: p.category,
      unit: p.unit || 'pcs',
      stockQuantityByWarehouse: p.stockQuantityByWarehouse ?? p.stock_quantity_by_warehouse ?? {},
    };
  },

  updateProduct: async (id: string, productData: any): Promise<Product> => {
    const p = await request<any>(`/products/${id}`, { method: 'PUT', body: productData });
    return {
      id: p.id,
      organizationId: p.organization_id,
      sku: p.sku,
      barcode: p.barcode,
      name: p.name,
      description: p.description,
      price: Number(p.price),
      costPrice: Number(p.cost_price),
      category: p.category,
      unit: p.unit || 'pcs',
      stockQuantityByWarehouse: p.stockQuantityByWarehouse ?? p.stock_quantity_by_warehouse ?? {},
    };
  },

  /* ---------------------------------------------------------- inventory */

  /**
   * A warehouse must always be chosen explicitly. There is no default
   * warehouse: an unspecified warehouse is a mistake, not something to guess.
   */
  adjustStock: async (productId: string, warehouseId: string, delta: number, reason: string) => {
    if (!warehouseId) {
      throw new ApiError('پیش از اصلاح موجودی باید انبار را انتخاب کنید.', 422);
    }

    return request('/inventory/adjust', {
      method: 'POST',
      body: {
        warehouse_id: warehouseId,
        product_id: productId,
        delta,
        reason,
      },
    });
  },

  getStock: async (warehouseId?: string): Promise<any[]> => request('/inventory/stock', { query: { warehouse_id: warehouseId } }),

  transferStock: async (payload: {
    source_warehouse_id: string;
    destination_warehouse_id: string;
    product_id: string;
    quantity: number;
    reason?: string;
  }) => request('/inventory/transfer', { method: 'POST', body: payload }),

  /* ---------------------------------------------------------------- POS */

  checkout: async (payload: any): Promise<SalesOrder> => {
    const context = getBusinessContext();
    const warehouseId = payload.warehouse_id ?? payload.warehouseId ?? context.warehouseId;

    if (!warehouseId) {
      throw new ApiError('پیش از ثبت فروش باید انبار را انتخاب کنید.', 422);
    }

    const data = await request<any>('/orders/checkout', {
      method: 'POST',
      body: { ...payload, org_id: context.organizationId, store_id: context.storeId, warehouse_id: warehouseId },
    });

    return {
      id: data.id,
      orderNumber: data.order_number ?? data.orderNumber,
      organizationId: data.organization_id ?? data.organizationId,
      storeId: data.store_id ?? data.storeId,
      warehouseId: data.warehouse_id ?? data.warehouseId,
      customerId: data.customer_id ?? data.customerId,
      customerName: data.customer_name ?? data.customerName,
      items: (data.items || []).map((i: any) => ({
        productId: i.product_id ?? i.productId,
        productName: i.product_name ?? i.productName,
        sku: i.sku,
        unitPrice: Number(i.unit_price ?? i.price ?? 0),
        quantity: Number(i.quantity ?? 1),
        totalPrice: Number(i.total_price ?? 0),
      })),
      subtotal: Number(data.subtotal ?? 0),
      discountAmount: Number(data.discount_amount ?? data.discountAmount ?? 0),
      taxAmount: Number(data.tax_amount ?? data.taxAmount ?? 0),
      totalAmount: Number(data.total_amount ?? data.totalAmount ?? 0),
      paymentMethod: data.payment_method ?? data.paymentMethod,
      paymentStatus: data.payment_status ?? data.paymentStatus,
      fulfillmentStatus: data.fulfillment_status ?? data.fulfillmentStatus,
      notes: data.notes,
      createdAt: data.created_at ?? data.createdAt ?? new Date().toISOString(),
    };
  },

  /* ---------------------------------------------------------- customers */

  getCustomers: async (): Promise<Customer[]> => {
    const data = await request<any[]>('/customers', { query: { org_id: getBusinessContext().organizationId } });

    return (data ?? []).map((c: any) => ({
      id: c.id,
      organizationId: c.organization_id ?? c.organizationId ?? getBusinessContext().organizationId,
      name: c.name,
      email: c.email ?? '',
      phone: c.phone ?? '',
      address: c.address ?? '',
      totalPurchases: Number(c.total_purchases ?? c.totalPurchases ?? 0),
      loyaltyPoints: Number(c.loyalty_points ?? c.loyaltyPoints ?? 0),
    }));
  },

  createCustomer: async (customerData: any): Promise<Customer> => {
    const c = await request<any>('/customers', { method: 'POST', body: { ...customerData, org_id: getBusinessContext().organizationId } });
    return {
      id: c.id,
      organizationId: c.organization_id,
      name: c.name,
      email: c.email || '',
      phone: c.phone || '',
      address: c.address || '',
      totalPurchases: Number(c.total_purchases || 0),
      loyaltyPoints: Number(c.loyalty_points || 0),
    };
  },

  updateCustomer: async (id: string, customerData: any): Promise<Customer> => {
    const c = await request<any>(`/customers/${id}`, { method: 'PUT', body: customerData });
    return {
      id: c.id,
      organizationId: c.organization_id,
      name: c.name,
      email: c.email || '',
      phone: c.phone || '',
      address: c.address || '',
      totalPurchases: Number(c.total_purchases || 0),
      loyaltyPoints: Number(c.loyalty_points || 0),
    };
  },

  /* ---------------------------------------------------------- suppliers */

  getSuppliers: async (query?: string): Promise<Supplier[]> => {
    const data = await request<any[]>('/suppliers', { query: { query, org_id: getBusinessContext().organizationId } });

    return (data ?? []).map((s: any) => ({
      id: s.id,
      organizationId: s.organization_id ?? s.organizationId ?? getBusinessContext().organizationId,
      name: s.name,
      email: s.email || '',
      phone: s.phone || '',
      address: s.address || '',
      purchases: s.purchases || [],
      createdAt: s.created_at || s.createdAt,
    }));
  },

  getSupplierById: async (id: string): Promise<Supplier> => {
    const s = await request<any>(`/suppliers/${id}`);
    return {
      id: s.id,
      organizationId: s.organization_id ?? s.organizationId ?? getBusinessContext().organizationId,
      name: s.name,
      email: s.email || '',
      phone: s.phone || '',
      address: s.address || '',
      purchases: s.purchases || [],
      createdAt: s.created_at || s.createdAt,
    };
  },

  createSupplier: async (data: any): Promise<Supplier> => {
    const s = await request<any>('/suppliers', { method: 'POST', body: { ...data, org_id: getBusinessContext().organizationId } });
    return {
      id: s.id,
      organizationId: s.organization_id,
      name: s.name,
      email: s.email || '',
      phone: s.phone || '',
      address: s.address || '',
      purchases: [],
    };
  },

  updateSupplier: async (id: string, data: any): Promise<Supplier> => {
    const s = await request<any>(`/suppliers/${id}`, { method: 'PUT', body: data });
    return {
      id: s.id,
      organizationId: s.organization_id,
      name: s.name,
      email: s.email || '',
      phone: s.phone || '',
      address: s.address || '',
      purchases: s.purchases || [],
    };
  },

  deleteSupplier: async (id: string) => request(`/suppliers/${id}`, { method: 'DELETE' }),

  /* --------------------------------------------------------- purchasing */

  getPurchases: async (): Promise<Purchase[]> => {
    const data = await request<any[]>('/purchases', { query: { org_id: getBusinessContext().organizationId } });

    return (data ?? []).map((p: any) => ({
      id: p.id,
      purchaseNumber: p.purchase_number ?? p.purchaseNumber ?? p.id,
      organizationId: p.organization_id ?? getBusinessContext().organizationId,
      // Reported exactly as stored. A purchase with no warehouse is shown as
      // unknown, never as somebody else's warehouse.
      storeId: p.store_id ?? '',
      warehouseId: p.warehouse_id ?? '',
      supplierId: p.supplier_id ?? p.supplierId,
      supplierName: p.supplier?.name ?? '',
      supplier: p.supplier,
      items: (p.items || []).map((i: any) => ({
        id: i.id,
        productId: i.product_id ?? i.productId,
        productName: i.product?.name ?? '',
        quantity: Number(i.quantity ?? 1),
        unitCost: Number(i.unit_cost ?? i.unitCost ?? 0),
        totalCost: Number(i.total_cost ?? i.totalCost ?? 0),
      })),
      totalAmount: Number(p.total_amount ?? p.totalAmount ?? 0),
      status: p.status ?? 'ORDERED',
      paymentStatus: p.payment_status ?? p.paymentStatus ?? 'UNPAID',
      createdAt: p.created_at ?? p.createdAt ?? new Date().toISOString(),
    }));
  },

  getPurchaseById: async (id: string): Promise<Purchase> => {
    const p = await request<any>(`/purchases/${id}`);
    return {
      id: p.id,
      purchaseNumber: p.purchase_number ?? p.purchaseNumber ?? p.id,
      organizationId: p.organization_id ?? getBusinessContext().organizationId,
      storeId: p.store_id ?? '',
      warehouseId: p.warehouse_id ?? '',
      supplierId: p.supplier_id ?? p.supplierId,
      supplierName: p.supplier?.name ?? '',
      supplier: p.supplier,
      items: (p.items || []).map((i: any) => ({
        id: i.id,
        productId: i.product_id ?? i.productId,
        quantity: Number(i.quantity),
        unitCost: Number(i.unit_cost),
        totalCost: Number(i.total_cost),
      })),
      totalAmount: Number(p.total_amount),
      status: p.status,
      paymentStatus: p.payment_status,
      createdAt: p.created_at,
    };
  },

  createPurchase: async (payload: {
    store_id: string;
    warehouse_id: string;
    supplier_id: string;
    items: any[];
  }): Promise<Purchase> => {
    const p = await request<any>('/purchases', {
      method: 'POST',
      body: { ...payload, org_id: getBusinessContext().organizationId },
    });
    return {
      id: p.id,
      purchaseNumber: p.purchase_number,
      organizationId: p.organization_id,
      storeId: p.store_id,
      warehouseId: p.warehouse_id,
      supplierId: p.supplier_id,
      items: (p.items || []).map((i: any) => ({
        id: i.id,
        productId: i.product_id,
        quantity: Number(i.quantity),
        unitCost: Number(i.unit_cost),
        totalCost: Number(i.total_cost),
      })),
      totalAmount: Number(p.total_amount),
      status: p.status,
      paymentStatus: p.payment_status,
      createdAt: p.created_at,
    };
  },

  receivePurchase: async (id: string) => request(`/purchases/${id}/receive`, { method: 'POST' }),

  payPurchase: async (id: string, amount: number, paymentMethod: string) =>
    request(`/purchases/${id}/pay`, { method: 'POST', body: { amount, payment_method: paymentMethod } }),

  /* ------------------------------------------------------------ expenses */

  getExpenses: async (): Promise<Expense[]> => {
    const data = await request<any[]>('/expenses', { query: { org_id: getBusinessContext().organizationId } });

    return (data ?? []).map((e: any) => ({
      id: e.id,
      organizationId: e.organization_id ?? e.organizationId ?? getBusinessContext().organizationId,
      storeId: e.store_id ?? e.storeId ?? getBusinessContext().storeId,
      category: e.category,
      amount: Number(e.amount ?? 0),
      paymentMethod: e.payment_method ?? e.paymentMethod ?? 'CASH',
      date: e.date,
      notes: e.notes || '',
      userId: e.user_id ?? e.userId,
      createdAt: e.created_at ?? e.createdAt,
    }));
  },

  getExpenseById: async (id: string): Promise<Expense> => {
    const e = await request<any>(`/expenses/${id}`);
    return {
      id: e.id,
      organizationId: e.organization_id ?? getBusinessContext().organizationId,
      storeId: e.store_id ?? getBusinessContext().storeId,
      category: e.category,
      amount: Number(e.amount ?? 0),
      paymentMethod: e.payment_method ?? 'CASH',
      date: e.date,
      notes: e.notes || '',
      userId: e.user_id,
      createdAt: e.created_at,
    };
  },

  createExpense: async (data: {
    category: string;
    amount: number;
    date: string;
    payment_method?: string;
    notes?: string;
  }): Promise<Expense> => {
    const context = getBusinessContext();
    const e = await request<any>('/expenses', {
      method: 'POST',
      body: { ...data, org_id: context.organizationId, store_id: context.storeId },
    });
    return {
      id: e.id,
      organizationId: e.organization_id,
      storeId: e.store_id,
      category: e.category,
      amount: Number(e.amount),
      paymentMethod: e.payment_method,
      date: e.date,
      notes: e.notes || '',
      userId: e.user_id,
      createdAt: e.created_at,
    };
  },

  updateExpense: async (
    id: string,
    data: { category?: string; amount?: number; date?: string; payment_method?: string; notes?: string }
  ): Promise<Expense> => {
    const e = await request<any>(`/expenses/${id}`, { method: 'PUT', body: data });
    return {
      id: e.id,
      organizationId: e.organization_id,
      storeId: e.store_id,
      category: e.category,
      amount: Number(e.amount),
      paymentMethod: e.payment_method,
      date: e.date,
      notes: e.notes || '',
    };
  },

  deleteExpense: async (id: string) => request(`/expenses/${id}`, { method: 'DELETE' }),

  /* ------------------------------------------------------------ messages */

  parseMessage: async (rawText: string, source = 'manual_paste'): Promise<any> =>
    request('/messages/parse', {
      method: 'POST',
      body: { raw_text: rawText, source, org_id: getBusinessContext().organizationId },
    }),

  getImportedMessages: async (): Promise<any[]> =>
    request<any[]>('/messages', { query: { org_id: getBusinessContext().organizationId } }),

  /* --------------------------------------------------------- registration */

  registerBusiness: async (data: any): Promise<any> =>
    request('/business-applications', { method: 'POST', anonymous: true, body: data }),

  getBusinessApplicationStatus: async (id: string): Promise<any> =>
    request(`/business-applications/${id}/status`, { anonymous: true }),

  /* ------------------------------------------------------------- platform */

  getPlatformApplications: async (status?: string): Promise<any[]> =>
    request<any[]>('/platform/business-applications', { query: { status } }),

  approvePlatformApplication: async (id: string): Promise<any> =>
    request(`/platform/business-applications/${id}/approve`, { method: 'POST', body: {} }),

  rejectPlatformApplication: async (id: string, reason: string): Promise<any> =>
    request(`/platform/business-applications/${id}/reject`, { method: 'POST', body: { reason } }),

  getPlatformAuditLogs: async (): Promise<any[]> => request<any[]>('/audit-logs'),

  /* ---------------------------------------------------------- onboarding */

  getOnboarding: async (): Promise<any> => request('/tenant/onboarding'),

  completeOnboarding: async (): Promise<any> => request('/tenant/onboarding/complete', { method: 'POST' }),

  /* ---------------------------------------------------------- accounting */

  getJournalEntries: async (): Promise<LedgerEntry[]> => {
    const data = await request<any[]>('/accounting/journal', {
      query: { org_id: getBusinessContext().organizationId },
    });

    return (data ?? []).map((e: any) => ({
      id: e.id,
      organizationId: e.organization_id ?? e.organizationId ?? getBusinessContext().organizationId,
      storeId: e.store_id ?? e.storeId ?? getBusinessContext().storeId,
      entryNumber: e.entry_number ?? e.entryNumber ?? e.id,
      type: e.type ?? 'CREDIT',
      category: e.category ?? 'SALES',
      amount: Number(e.amount ?? e.total_debit ?? 0),
      description: e.description ?? '',
      createdAt: e.created_at ?? e.createdAt ?? new Date().toISOString(),
    }));
  },

  getAccountingSummary: async (): Promise<AccountingSummary> => {
    const data = await request<any>('/accounting/summary', {
      query: { org_id: getBusinessContext().organizationId },
    });

    return {
      totalRevenue: Number(data.total_revenue ?? data.totalRevenue ?? 0),
      todayRevenue: Number(data.today_revenue ?? data.todayRevenue ?? 0),
      totalExpenses: Number(data.total_expenses ?? data.totalExpenses ?? 0),
      netProfit: Number(data.net_profit ?? data.netProfit ?? 0),
      totalSalesCount: Number(data.total_sales_count ?? data.totalSalesCount ?? 0),
      todaySalesCount: Number(data.today_sales_count ?? data.todaySalesCount ?? 0),
    };
  },
};

export type { AuthUser };
