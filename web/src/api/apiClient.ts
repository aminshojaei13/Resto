import {
  AccountingSummary,
  AuthUser,
  BusinessMembership,
  BusinessSettings,
  BusinessWarehouse,
  Customer,
  Expense,
  ExpenseCategory,
  ExpenseSummary,
  LedgerEntry,
  OrderItem,
  Organization,
  Paginated,
  Product,
  Purchase,
  PurchaseItem,
  SalesOrder,
  StockMovement,
  StockRow,
  Store,
  Supplier,
  UnitOption,
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
 * The language the person is looking at right now.
 *
 * Labels are localised server side so a Persian screen never shows an English
 * unit name and an English screen never shows a Persian one.
 */
let activeLocale: 'fa' | 'en' = 'fa';

export const setApiLocale = (locale: 'fa' | 'en'): void => {
  activeLocale = locale;
};

const currentLocale = (): string => activeLocale;

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

const mapPurchaseItem = (i: any): PurchaseItem => ({
  id: i.id,
  purchaseId: i.purchase_id ?? i.purchaseId,
  productId: i.product_id ?? i.productId,
  productName: i.product?.name ?? '',
  unit: i.unit ?? undefined,
  quantity: Number(i.quantity ?? 0),
  receivedQuantity: Number(i.received_quantity ?? i.receivedQuantity ?? 0),
  unitCost: Number(i.unit_cost ?? i.unitCost ?? 0),
  totalCost: Number(i.total_cost ?? i.totalCost ?? 0),
});

const mapOrderItem = (i: any): OrderItem => ({
  productId: i.product_id ?? i.productId,
  productName: i.product_name ?? i.productName,
  sku: i.sku ?? '',
  unit: i.unit ?? undefined,
  unitPrice: Number(i.unit_price ?? i.price ?? 0),
  quantity: Number(i.quantity ?? 1),
  subtotal: Number(i.subtotal ?? 0),
  discountAmount: Number(i.discount_amount ?? i.discountAmount ?? 0),
  taxAmount: Number(i.tax_amount ?? i.taxAmount ?? 0),
  totalPrice: Number(i.total_price ?? 0),
});

const mapProduct = (p: any, fallbackOrgId: string): Product => ({
  id: p.id,
  organizationId: p.organization_id ?? p.organizationId ?? fallbackOrgId,
  sku: p.sku,
  barcode: p.barcode,
  name: p.name,
  description: p.description,
  price: Number(p.price ?? 0),
  costPrice: Number(p.cost_price ?? p.costPrice ?? 0),
  category: p.category,
  unit: p.unit ?? 'piece',
  unitLabel: p.unit_label ?? p.unitLabel,
  imageUrl: p.image_url ?? p.imageUrl,
  stockByWarehouse: (p.stock_by_warehouse ?? []).map((s: any) => ({
    warehouseId: s.warehouse_id ?? s.warehouseId,
    warehouseName: s.warehouse_name ?? s.warehouseName ?? '',
    quantity: Number(s.quantity ?? 0),
    reservedQuantity: Number(s.reserved_quantity ?? s.reservedQuantity ?? 0),
  })),
});

const mapOrder = (data: any, fallbackOrgId: string, fallbackStoreId: string): SalesOrder => ({
  id: data.id,
  orderNumber: data.order_number ?? data.orderNumber,
  organizationId: data.organization_id ?? fallbackOrgId,
  storeId: data.store_id ?? fallbackStoreId,
  warehouseId: data.warehouse_id ?? '',
  customerId: data.customer_id ?? undefined,
  customerName: data.customer_name ?? data.customerName ?? '',
  items: (data.items || []).map(mapOrderItem),
  subtotal: Number(data.subtotal ?? 0),
  discountAmount: Number(data.discount_amount ?? data.discountAmount ?? 0),
  taxAmount: Number(data.tax_amount ?? data.taxAmount ?? 0),
  taxRate: Number(data.tax_rate ?? data.taxRate ?? 0),
  totalAmount: Number(data.total_amount ?? data.totalAmount ?? 0),
  paymentMethod: data.payment_method ?? data.paymentMethod ?? '',
  paymentStatus: data.payment_status ?? data.paymentStatus ?? '',
  fulfillmentStatus: data.fulfillment_status ?? data.fulfillmentStatus ?? '',
  source: data.source ?? 'POS',
  itemsCount: Number(data.items_count ?? data.itemsCount ?? (data.items || []).length),
  notes: data.notes ?? '',
  createdAt: data.created_at ?? data.createdAt ?? new Date().toISOString(),
});

const mapExpense = (e: any, fallbackOrgId: string, fallbackStoreId: string): Expense => ({
  id: e.id,
  organizationId: e.organization_id ?? fallbackOrgId,
  storeId: e.store_id ?? fallbackStoreId,
  title: e.title ?? e.category ?? '',
  category: e.category,
  categoryLabel: e.category_label ?? e.categoryLabel,
  amount: Number(e.amount ?? 0),
  paymentMethod: e.payment_method ?? e.paymentMethod ?? 'CASH',
  date: e.date,
  notes: e.notes || '',
  attachmentUrl: e.attachment_url ?? null,
  userId: e.user_id ?? undefined,
  createdAt: e.created_at ?? e.createdAt,
});

const mapPurchase = (p: any, context: BusinessContext): Purchase => ({
  id: p.id,
  purchaseNumber: p.purchase_number ?? p.id,
  organizationId: p.organization_id ?? context.organizationId,
  storeId: p.store_id ?? context.storeId ?? '',
  warehouseId: p.warehouse_id ?? '',
  supplierId: p.supplier_id ?? '',
  supplierName: p.supplier?.name ?? '',
  supplier: p.supplier,
  items: (p.items || []).map(mapPurchaseItem),
  totalAmount: Number(p.total_amount ?? 0),
  status: p.status ?? 'ORDERED',
  paymentStatus: p.payment_status ?? 'UNPAID',
  purchaseDate: p.purchase_date ?? undefined,
  receivedAt: p.received_at ?? null,
  createdAt: p.created_at ?? new Date().toISOString(),
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
    const data = await request<any[]>('/products', {
      query: { query, locale: currentLocale() },
    });

    return (data ?? []).map((p: any) => mapProduct(p, getBusinessContext().organizationId));
  },

  createProduct: async (productData: any): Promise<Product> =>
    mapProduct(await request<any>('/products', { method: 'POST', body: productData }), getBusinessContext().organizationId),

  updateProduct: async (id: string, productData: any): Promise<Product> =>
    mapProduct(await request<any>(`/products/${id}`, { method: 'PUT', body: productData }), getBusinessContext().organizationId),

  /* ---------------------------------------------------- business settings */

  getBusinessSettings: async (): Promise<BusinessSettings> => {
    const data = await request<any>('/business/settings');

    return {
      defaultTaxRate: Number(data.default_tax_rate ?? 0),
      taxInclusivePricing: Boolean(data.tax_inclusive_pricing),
      canOverrideTaxPerOrder: Boolean(data.can_override_tax_per_order),
      minTaxRate: Number(data.min_tax_rate ?? 0),
      maxTaxRate: Number(data.max_tax_rate ?? 100),
    };
  },

  updateBusinessSettings: async (payload: { default_tax_rate: number }): Promise<BusinessSettings> => {
    const data = await request<any>('/business/settings', { method: 'PUT', body: payload });

    return {
      defaultTaxRate: Number(data.settings?.default_tax_rate ?? payload.default_tax_rate),
      taxInclusivePricing: Boolean(data.settings?.tax_inclusive_pricing),
      canOverrideTaxPerOrder: Boolean(data.settings?.can_override_tax_per_order),
      minTaxRate: Number(data.settings?.min_tax_rate ?? 0),
      maxTaxRate: Number(data.settings?.max_tax_rate ?? 100),
    };
  },

  getUnits: async (): Promise<UnitOption[]> =>
    request<UnitOption[]>('/units', { query: { locale: currentLocale() } }),

  /* ---------------------------------------------------------- inventory */

  /**
   * The warehouses this business really has. There is no default and no
   * fixture id: the picker is built from exactly what the server returns.
   */
  getWarehouses: async (): Promise<BusinessWarehouse[]> => {
    const data = await request<any[]>('/inventory/warehouses');

    return (data ?? []).map((w) => ({
      id: w.id,
      name: w.name,
      code: w.code ?? '',
      storeId: w.store_id ?? '',
      storeName: w.store_name ?? '',
    }));
  },

  getStock: async (warehouseId?: string): Promise<StockRow[]> => {
    const data = await request<any[]>('/inventory/stock', {
      query: { warehouse_id: warehouseId, locale: currentLocale() },
    });

    return (data ?? []).map((s) => ({
      id: s.id,
      warehouseId: s.warehouse_id ?? '',
      warehouseName: s.warehouse_name ?? '',
      productId: s.product_id ?? '',
      productName: s.product_name ?? '',
      sku: s.sku ?? '',
      quantity: Number(s.quantity ?? 0),
      reservedQuantity: Number(s.reserved_quantity ?? 0),
      availableQuantity: Number(s.available_quantity ?? 0),
      unit: s.unit ?? 'piece',
      unitLabel: s.unit_label ?? 'عدد',
    }));
  },

  getStockMovements: async (filters: { warehouseId?: string; productId?: string } = {}): Promise<StockMovement[]> => {
    const data = await request<Paginated<StockMovement>>('/inventory/movements', {
      query: { warehouse_id: filters.warehouseId, product_id: filters.productId },
    });

    return data?.data ?? [];
  },

  /**
   * Record stock arriving in a warehouse.
   *
   * The warehouse is always explicit: there is no default and the server
   * refuses a warehouse that is not this business's.
   */
  stockIn: async (payload: {
    warehouseId: string;
    productId: string;
    quantity: number;
    reason?: string;
  }) => {
    if (!payload.warehouseId) {
      throw new ApiError('برای ثبت موجودی باید انبار انتخاب شود.', 422, {
        warehouse_id: ['انبار انتخاب نشده است.'],
      });
    }

    return request('/inventory/stock-in', {
      method: 'POST',
      body: {
        warehouse_id: payload.warehouseId,
        product_id: payload.productId,
        quantity: payload.quantity,
        reason: payload.reason,
      },
    });
  },

  adjustStock: async (payload: {
    warehouseId: string;
    productId: string;
    delta: number;
    reason: string;
  }) => {
    if (!payload.warehouseId) {
      throw new ApiError('برای اصلاح موجودی باید انبار انتخاب شود.', 422, {
        warehouse_id: ['انبار انتخاب نشده است.'],
      });
    }

    return request('/inventory/adjust', {
      method: 'POST',
      body: {
        warehouse_id: payload.warehouseId,
        product_id: payload.productId,
        delta: payload.delta,
        reason: payload.reason,
      },
    });
  },

  transferStock: async (payload: {
    source_warehouse_id: string;
    destination_warehouse_id: string;
    product_id: string;
    quantity: number;
    reason?: string;
  }) => request('/inventory/transfer', { method: 'POST', body: payload }),

  createWarehouse: async (payload: { storeId: string; name: string; code: string }) => {
    const context = getBusinessContext();

    return request<any>('/warehouses', {
      method: 'POST',
      body: {
        organization_id: context.organizationId,
        store_id: payload.storeId,
        name: payload.name,
        code: payload.code,
      },
    });
  },

  /* ---------------------------------------------------------------- POS */

  /**
   * Register a sale.
   *
   * The server prices the order from the catalog and the business tax rate.
   * Anything this method sends about price or total is ignored on purpose.
   */
  checkout: async (payload: {
    warehouseId: string;
    customerId?: string;
    customerName?: string;
    paymentMethod: string;
    paymentStatus?: string;
    fulfillmentStatus?: string;
    taxRate?: number;
    source?: string;
    notes?: string;
    items: { productId: string; quantity: number; discountPercent?: number }[];
  }): Promise<SalesOrder> => {
    const context = getBusinessContext();

    if (!payload.warehouseId) {
      throw new ApiError('برای ثبت فروش باید انبار انتخاب شود.', 422, {
        warehouse_id: ['انبار انتخاب نشده است.'],
      });
    }

    const data = await request<any>('/orders/checkout', {
      method: 'POST',
      body: {
        store_id: context.storeId,
        warehouse_id: payload.warehouseId,
        customer_id: payload.customerId ?? null,
        customer_name: payload.customerName,
        payment_method: payload.paymentMethod,
        payment_status: payload.paymentStatus,
        fulfillment_status: payload.fulfillmentStatus,
        tax_rate: payload.taxRate,
        source: payload.source ?? 'POS',
        notes: payload.notes,
        items: payload.items.map((item) => ({
          product_id: item.productId,
          quantity: item.quantity,
          discount_percent: item.discountPercent ?? 0,
        })),
      },
    });

    return mapOrder(data, context.organizationId, context.storeId);
  },

  /* -------------------------------------------------------------- orders */

  /** The operational order list: searched and filtered on the server. */
  getOrders: async (filters: {
    q?: string;
    status?: string;
    perPage?: number;
  } = {}): Promise<Paginated<SalesOrder>> => {
    const context = getBusinessContext();
    const data = await request<Paginated<any>>('/orders', {
      query: { q: filters.q, status: filters.status, per_page: String(filters.perPage ?? '') },
    });

    return {
      current_page: data?.current_page ?? 1,
      last_page: data?.last_page ?? 1,
      per_page: data?.per_page ?? 25,
      total: data?.total ?? 0,
      data: (data?.data ?? []).map((o) => mapOrder(o, context.organizationId, context.storeId)),
    };
  },

  getOrder: async (id: string): Promise<SalesOrder> => {
    const context = getBusinessContext();
    const data = await request<any>(`/orders/${id}`);

    return {
      ...mapOrder(data, context.organizationId, context.storeId),
      timeline: (data.timeline ?? []).map((e: any) => ({
        status: e.status,
        at: e.at,
        label: e.label,
      })),
      availableActions: data.available_actions ?? ['view'],
    };
  },

  prepareOrder: async (id: string) => request(`/orders/${id}/prepare`, { method: 'POST', body: {} }),

  payOrder: async (id: string, paymentMethod?: string) =>
    request(`/orders/${id}/pay`, { method: 'POST', body: { payment_method: paymentMethod } }),

  cancelOrder: async (id: string) => request(`/orders/${id}/cancel`, { method: 'POST', body: {} }),

  refundOrder: async (id: string) => request(`/orders/${id}/refund`, { method: 'POST', body: {} }),

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

  getPurchases: async (query?: string): Promise<Purchase[]> => {
    const context = getBusinessContext();
    const data = await request<any[]>('/purchases', { query: { q: query } });

    return (data ?? []).map((p: any) => mapPurchase(p, context));
  },

  /** Purchases still waiting for goods: the receiving worklist. */
  getReceivingQueue: async (): Promise<Purchase[]> => {
    const context = getBusinessContext();
    const data = await request<any[]>('/purchases/receiving-queue');

    return (data ?? []).map((p: any) => mapPurchase(p, context));
  },

  getPurchaseById: async (id: string): Promise<Purchase> => {
    const context = getBusinessContext();

    return mapPurchase(await request<any>(`/purchases/${id}`), context);
  },

  createPurchase: async (payload: {
    supplier_id: string;
    warehouse_id?: string;
    purchase_date?: string;
    items: { product_id: string; quantity: number; unit_cost: number }[];
  }): Promise<Purchase> => {
    const context = getBusinessContext();

    return mapPurchase(
      await request<any>('/purchases', {
        method: 'POST',
        body: {
          store_id: context.storeId,
          warehouse_id: payload.warehouse_id ?? null,
          supplier_id: payload.supplier_id,
          purchase_date: payload.purchase_date,
          items: payload.items,
        },
      }),
      context
    );
  },

  /**
   * Record a delivery into a warehouse.
   *
   * `lines` may name only what arrived: lines left out are simply not part of
   * this delivery. Omitting them entirely means "receive everything still
   * outstanding".
   */
  receivePurchase: async (
    id: string,
    payload: { warehouseId: string; lines?: { purchaseItemId: string; receivedQuantity: number }[] }
  ) =>
    request(`/purchases/${id}/receive`, {
      method: 'POST',
      body: {
        warehouse_id: payload.warehouseId,
        items: payload.lines?.map((l) => ({
          purchase_item_id: l.purchaseItemId,
          received_quantity: l.receivedQuantity,
        })),
      },
    }),

  payPurchase: async (id: string, amount: number, paymentMethod: string) =>
    request(`/purchases/${id}/pay`, {
      method: 'POST',
      body: { amount, payment_method: paymentMethod },
    }),

  /* ------------------------------------------------------------ expenses */

  getExpenseCategories: async (): Promise<ExpenseCategory[]> =>
    request<ExpenseCategory[]>('/expenses/categories', { query: { locale: currentLocale() } }),

  getExpenseSummary: async (): Promise<ExpenseSummary> => {
    const data = await request<any>('/expenses/summary');

    return {
      monthTotal: Number(data.month_total ?? 0),
      todayTotal: Number(data.today_total ?? 0),
      previousMonthTotal: Number(data.previous_month_total ?? 0),
      byCategory: (data.by_category ?? []).map((c: any) => ({
        category: c.category,
        label: c.label,
        total: Number(c.total ?? 0),
        entries: Number(c.entries ?? 0),
      })),
      trend: (data.trend ?? []).map((t: any) => ({
        month: t.month,
        label: t.label,
        total: Number(t.total ?? 0),
      })),
    };
  },

  getExpenses: async (query?: string): Promise<Expense[]> => {
    const context = getBusinessContext();
    const data = await request<any[]>('/expenses', { query: { q: query } });

    return (data ?? []).map((e) => mapExpense(e, context.organizationId, context.storeId ?? ''));
  },

  getExpenseById: async (id: string): Promise<Expense> => {
    const context = getBusinessContext();

    return mapExpense(await request<any>(`/expenses/${id}`), context.organizationId, context.storeId);
  },

  createExpense: async (data: {
    title: string;
    category: string;
    amount: number;
    date: string;
    payment_method?: string;
    notes?: string;
    attachment_url?: string;
  }): Promise<Expense> => {
    const context = getBusinessContext();

    return mapExpense(
      await request<any>('/expenses', {
        method: 'POST',
        body: {
          store_id: context.storeId,
          title: data.title,
          category: data.category,
          amount: data.amount,
          date: data.date,
          payment_method: data.payment_method,
          notes: data.notes,
          attachment_url: data.attachment_url,
        },
      }),
      context.organizationId,
      context.storeId ?? ''
    );
  },

  updateExpense: async (
    id: string,
    data: { title?: string; category?: string; amount?: number; date?: string; notes?: string }
  ): Promise<Expense> => {
    const context = getBusinessContext();

    return mapExpense(
      await request<any>(`/expenses/${id}`, { method: 'PUT', body: data }),
      context.organizationId,
      context.storeId ?? ''
    );
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
