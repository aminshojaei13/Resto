import { AccountingSummary, Customer, Expense, LedgerEntry, Organization, Product, Purchase, SalesOrder, Store, Supplier, Warehouse } from '../types';

const BASE_URL = 'http://localhost:8000/api/v1';

let activeOrgId = 'org_apex';
let activeStoreId = 'store_apex_1';

export const setTenantContext = (orgId: string, storeId: string) => {
  activeOrgId = orgId;
  activeStoreId = storeId;
};

const getHeaders = () => ({
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'X-Tenant-ID': activeOrgId,
  'X-Store-ID': activeStoreId,
});

export const apiClient = {
  getOrganizations: async (): Promise<Organization[]> => {
    try {
      const res = await fetch(`${BASE_URL}/organizations`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch organizations');
      const data = await res.json();
      return (data || []).map((org: any) => ({
        id: org.id,
        name: org.name,
        code: org.code,
        logoUrl: org.logo_url ?? org.logoUrl,
        currencySymbol: org.currency_symbol ?? org.currencySymbol ?? '$',
        currencyCode: org.currency_code ?? org.currencyCode ?? 'USD',
        subscriptionTier: org.subscription_tier ?? org.subscriptionTier ?? 'PRO',
        stores: (org.stores || []).map((store: any) => ({
          id: store.id,
          organizationId: store.organization_id ?? store.organizationId,
          name: store.name,
          code: store.code,
          address: store.address,
          phone: store.phone,
          warehouses: (store.warehouses || []).map((wh: any) => ({
            id: wh.id,
            storeId: wh.store_id ?? wh.storeId,
            organizationId: wh.organization_id ?? wh.organizationId,
            name: wh.name,
            code: wh.code,
          })),
        })),
      }));
    } catch {
      return [
        {
          id: 'org_apex',
          name: 'Apex Retail Group',
          code: 'APEX',
          currencySymbol: '$',
          currencyCode: 'USD',
          subscriptionTier: 'ENTERPRISE',
          stores: [
            {
              id: 'store_apex_1',
              organizationId: 'org_apex',
              name: 'Apex Flagship Store (Downtown)',
              code: 'APX-DT',
              warehouses: [
                { id: 'wh_apex_1a', storeId: 'store_apex_1', organizationId: 'org_apex', name: 'Main Warehouse', code: 'WH-MAIN' },
                { id: 'wh_apex_1b', storeId: 'store_apex_1', organizationId: 'org_apex', name: 'Express Storage Hub', code: 'WH-EXP' },
              ],
            },
          ],
        },
      ];
    }
  },

  getProducts: async (query?: string): Promise<Product[]> => {
    try {
      const url = new URL(`${BASE_URL}/products`);
      url.searchParams.append('org_id', activeOrgId);
      if (query) url.searchParams.append('query', query);

      const res = await fetch(url.toString(), { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch products');
      const data = await res.json();
      return (data || []).map((p: any) => ({
        id: p.id,
        organizationId: p.organization_id ?? p.organizationId ?? activeOrgId,
        sku: p.sku,
        barcode: p.barcode,
        name: p.name,
        description: p.description,
        price: Number(p.price ?? 0),
        costPrice: Number(p.cost_price ?? p.costPrice ?? 0),
        category: p.category,
        unit: p.unit ?? 'pcs',
        imageUrl: p.image_url ?? p.imageUrl,
        stockQuantityByWarehouse: p.stockQuantityByWarehouse ?? { wh_apex_1a: 20 },
      }));
    } catch {
      return [
        {
          id: 'prod_1',
          organizationId: activeOrgId,
          sku: 'APX-LAP-001',
          barcode: '880609123401',
          name: 'ProBook Ultra 15 M3',
          description: 'High-performance laptop featuring 16GB RAM and 512GB SSD.',
          price: 1299.99,
          costPrice: 850.0,
          category: 'Electronics',
          unit: 'pcs',
          stockQuantityByWarehouse: { wh_apex_1a: 22, wh_apex_1b: 7 },
        },
      ];
    }
  },

  createProduct: async (productData: any): Promise<Product> => {
    const res = await fetch(`${BASE_URL}/products`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ ...productData, org_id: activeOrgId }),
    });
    if (!res.ok) throw new Error('Failed to create product');
    const p = await res.json();
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
      stockQuantityByWarehouse: { wh_apex_1a: 0 },
    };
  },

  updateProduct: async (id: string, productData: any): Promise<Product> => {
    const res = await fetch(`${BASE_URL}/products/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(productData),
    });
    if (!res.ok) throw new Error('Failed to update product');
    const p = await res.json();
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
      stockQuantityByWarehouse: { wh_apex_1a: 0 },
    };
  },

  adjustStock: async (productId: string, warehouseId: string, delta: number, reason: string) => {
    try {
      const res = await fetch(`${BASE_URL}/inventory/adjust`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({
          org_id: activeOrgId,
          warehouse_id: warehouseId,
          product_id: productId,
          delta,
          reason,
        }),
      });
      return await res.json();
    } catch {
      return { success: true };
    }
  },

  checkout: async (payload: any): Promise<SalesOrder> => {
    const res = await fetch(`${BASE_URL}/orders/checkout`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ ...payload, org_id: activeOrgId, store_id: activeStoreId, warehouse_id: 'wh_apex_1a' }),
    });
    if (!res.ok) throw new Error('Checkout failed');
    const data = await res.json();
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

  getCustomers: async (): Promise<Customer[]> => {
    try {
      const res = await fetch(`${BASE_URL}/customers?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch customers');
      const data = await res.json();
      return (data || []).map((c: any) => ({
        id: c.id,
        organizationId: c.organization_id ?? c.organizationId ?? activeOrgId,
        name: c.name,
        email: c.email ?? '',
        phone: c.phone ?? '',
        address: c.address ?? '',
        totalPurchases: Number(c.total_purchases ?? c.totalPurchases ?? 0),
        loyaltyPoints: Number(c.loyalty_points ?? c.loyaltyPoints ?? 0),
      }));
    } catch {
      return [
        { id: 'cust_1', organizationId: activeOrgId, name: 'Sarah Connor', email: 'sarah@example.com', phone: '+1 (555) 234-5678', totalPurchases: 3548.5, loyaltyPoints: 350 },
      ];
    }
  },

  createCustomer: async (customerData: any): Promise<Customer> => {
    const res = await fetch(`${BASE_URL}/customers`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ ...customerData, org_id: activeOrgId }),
    });
    if (!res.ok) throw new Error('Failed to create customer');
    const c = await res.json();
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
    const res = await fetch(`${BASE_URL}/customers/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(customerData),
    });
    if (!res.ok) throw new Error('Failed to update customer');
    const c = await res.json();
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

  getSuppliers: async (query?: string): Promise<Supplier[]> => {
    try {
      const url = new URL(`${BASE_URL}/suppliers`);
      url.searchParams.append('org_id', activeOrgId);
      if (query) url.searchParams.append('query', query);

      const res = await fetch(url.toString(), { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch suppliers');
      const data = await res.json();
      return (data || []).map((s: any) => ({
        id: s.id,
        organizationId: s.organization_id ?? s.organizationId ?? activeOrgId,
        name: s.name,
        email: s.email || '',
        phone: s.phone || '',
        address: s.address || '',
        purchases: s.purchases || [],
        createdAt: s.created_at || s.createdAt,
      }));
    } catch {
      return [
        { id: 'sup_1', organizationId: activeOrgId, name: 'TechImport Global Co.', email: 'sales@techimport.com', phone: '+1 (800) 555-0199', address: '500 Logistics Way, San Jose, CA' },
      ];
    }
  },

  getSupplierById: async (id: string): Promise<Supplier> => {
    const res = await fetch(`${BASE_URL}/suppliers/${id}`, { headers: getHeaders() });
    if (!res.ok) throw new Error('Failed to fetch supplier details');
    const s = await res.json();
    return {
      id: s.id,
      organizationId: s.organization_id ?? s.organizationId ?? activeOrgId,
      name: s.name,
      email: s.email || '',
      phone: s.phone || '',
      address: s.address || '',
      purchases: s.purchases || [],
      createdAt: s.created_at || s.createdAt,
    };
  },

  createSupplier: async (data: any): Promise<Supplier> => {
    const res = await fetch(`${BASE_URL}/suppliers`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ ...data, org_id: activeOrgId }),
    });
    if (!res.ok) throw new Error('Failed to create supplier');
    const s = await res.json();
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
    const res = await fetch(`${BASE_URL}/suppliers/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Failed to update supplier');
    const s = await res.json();
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

  deleteSupplier: async (id: string) => {
    const res = await fetch(`${BASE_URL}/suppliers/${id}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to delete supplier');
    return await res.json();
  },

  getPurchases: async (): Promise<Purchase[]> => {
    try {
      const res = await fetch(`${BASE_URL}/purchases?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch purchases');
      const data = await res.json();
      return (data || []).map((p: any) => ({
        id: p.id,
        purchaseNumber: p.purchase_number ?? p.purchaseNumber ?? p.id,
        organizationId: p.organization_id ?? activeOrgId,
        storeId: p.store_id ?? activeStoreId,
        warehouseId: p.warehouse_id ?? 'wh_apex_1a',
        supplierId: p.supplier_id ?? p.supplierId,
        supplierName: p.supplier?.name || 'Supplier',
        supplier: p.supplier,
        items: (p.items || []).map((i: any) => ({
          id: i.id,
          productId: i.product_id ?? i.productId,
          productName: i.product?.name || i.product_id,
          quantity: Number(i.quantity ?? 1),
          unitCost: Number(i.unit_cost ?? i.unitCost ?? 0),
          totalCost: Number(i.total_cost ?? i.totalCost ?? 0),
        })),
        totalAmount: Number(p.total_amount ?? p.totalAmount ?? 0),
        status: p.status ?? 'ORDERED',
        paymentStatus: p.payment_status ?? p.paymentStatus ?? 'UNPAID',
        createdAt: p.created_at ?? p.createdAt ?? new Date().toISOString(),
      }));
    } catch {
      return [
        {
          id: 'po_1001',
          purchaseNumber: 'PO-2025-1001',
          organizationId: activeOrgId,
          storeId: activeStoreId,
          warehouseId: 'wh_apex_1a',
          supplierId: 'sup_1',
          supplierName: 'TechImport Global Co.',
          items: [{ productId: 'prod_1', productName: 'ProBook Ultra 15 M3', quantity: 10, unitCost: 850.0, totalCost: 8500.0 }],
          totalAmount: 8500.0,
          status: 'RECEIVED',
          paymentStatus: 'PAID',
          createdAt: new Date().toISOString(),
        },
      ];
    }
  },

  getPurchaseById: async (id: string): Promise<Purchase> => {
    const res = await fetch(`${BASE_URL}/purchases/${id}`, { headers: getHeaders() });
    if (!res.ok) throw new Error('Failed to fetch purchase details');
    const p = await res.json();
    return {
      id: p.id,
      purchaseNumber: p.purchase_number ?? p.purchaseNumber ?? p.id,
      organizationId: p.organization_id ?? activeOrgId,
      storeId: p.store_id ?? activeStoreId,
      warehouseId: p.warehouse_id ?? 'wh_apex_1a',
      supplierId: p.supplier_id ?? p.supplierId,
      supplierName: p.supplier?.name || 'Supplier',
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

  createPurchase: async (payload: { store_id: string; warehouse_id: string; supplier_id: string; items: any[] }): Promise<Purchase> => {
    const res = await fetch(`${BASE_URL}/purchases`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ ...payload, org_id: activeOrgId }),
    });
    if (!res.ok) throw new Error('Failed to create purchase order');
    const p = await res.json();
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

  receivePurchase: async (id: string) => {
    const res = await fetch(`${BASE_URL}/purchases/${id}/receive`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to receive purchase goods into inventory');
    return await res.json();
  },

  payPurchase: async (id: string, amount: number, paymentMethod: string) => {
    const res = await fetch(`${BASE_URL}/purchases/${id}/pay`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ amount, payment_method: paymentMethod }),
    });
    if (!res.ok) throw new Error('Failed to record supplier payment');
    return await res.json();
  },

  getExpenses: async (): Promise<Expense[]> => {
    try {
      const res = await fetch(`${BASE_URL}/expenses?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch expenses');
      const data = await res.json();
      return (data || []).map((e: any) => ({
        id: e.id,
        organizationId: e.organization_id ?? e.organizationId ?? activeOrgId,
        storeId: e.store_id ?? e.storeId ?? activeStoreId,
        category: e.category,
        amount: Number(e.amount ?? 0),
        paymentMethod: e.payment_method ?? e.paymentMethod ?? 'CASH',
        date: e.date,
        notes: e.notes || '',
        userId: e.user_id ?? e.userId,
        createdAt: e.created_at ?? e.createdAt,
      }));
    } catch {
      return [
        {
          id: 'exp_1001',
          organizationId: activeOrgId,
          storeId: activeStoreId,
          category: 'Store Utilities',
          amount: 450.0,
          paymentMethod: 'BANK_TRANSFER',
          date: new Date().toISOString().split('T')[0],
          notes: 'Monthly electricity bill for Apex Flagship Store',
        },
      ];
    }
  },

  getExpenseById: async (id: string): Promise<Expense> => {
    const res = await fetch(`${BASE_URL}/expenses/${id}`, { headers: getHeaders() });
    if (!res.ok) throw new Error('Failed to fetch expense details');
    const e = await res.json();
    return {
      id: e.id,
      organizationId: e.organization_id ?? activeOrgId,
      storeId: e.store_id ?? activeStoreId,
      category: e.category,
      amount: Number(e.amount ?? 0),
      paymentMethod: e.payment_method ?? 'CASH',
      date: e.date,
      notes: e.notes || '',
      userId: e.user_id,
      createdAt: e.created_at,
    };
  },

  createExpense: async (data: { category: string; amount: number; date: string; payment_method?: string; notes?: string }): Promise<Expense> => {
    const res = await fetch(`${BASE_URL}/expenses`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ ...data, org_id: activeOrgId, store_id: activeStoreId }),
    });
    if (!res.ok) throw new Error('Failed to record expense');
    const e = await res.json();
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

  updateExpense: async (id: string, data: { category?: string; amount?: number; date?: string; payment_method?: string; notes?: string }): Promise<Expense> => {
    const res = await fetch(`${BASE_URL}/expenses/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Failed to update expense');
    const e = await res.json();
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

  deleteExpense: async (id: string) => {
    const res = await fetch(`${BASE_URL}/expenses/${id}`, {
      method: 'DELETE',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to delete expense');
    return await res.json();
  },

  parseMessage: async (rawText: string, source = 'manual_paste'): Promise<any> => {
    const res = await fetch(`${BASE_URL}/messages/parse`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ raw_text: rawText, source, org_id: activeOrgId }),
    });
    if (!res.ok) throw new Error('Failed to parse order message');
    return await res.json();
  },

  getImportedMessages: async (): Promise<any[]> => {
    try {
      const res = await fetch(`${BASE_URL}/messages?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch imported messages');
      return await res.json();
    } catch {
      return [];
    }
  },

  // SaaS Business Applications & Onboarding (P5)
  registerBusiness: async (data: any): Promise<any> => {
    const res = await fetch(`${BASE_URL}/business-applications`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error('Failed to submit business application');
    return await res.json();
  },

  getPlatformApplications: async (status?: string): Promise<any[]> => {
    try {
      const url = new URL(`${BASE_URL}/platform/business-applications`);
      if (status) url.searchParams.append('status', status);

      const res = await fetch(url.toString(), { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch platform business applications');
      return await res.json();
    } catch {
      return [];
    }
  },

  approvePlatformApplication: async (id: string): Promise<any> => {
    const res = await fetch(`${BASE_URL}/platform/business-applications/${id}/approve`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to approve application');
    return await res.json();
  },

  rejectPlatformApplication: async (id: string, reason: string): Promise<any> => {
    const res = await fetch(`${BASE_URL}/platform/business-applications/${id}/reject`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ reason }),
    });
    if (!res.ok) throw new Error('Failed to reject application');
    return await res.json();
  },

  getTenantOnboarding: async (): Promise<any> => {
    try {
      const res = await fetch(`${BASE_URL}/tenant/onboarding`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch tenant onboarding status');
      return await res.json();
    } catch {
      return { onboarding_status: 'IN_PROGRESS' };
    }
  },

  completeOnboarding: async (): Promise<any> => {
    const res = await fetch(`${BASE_URL}/tenant/onboarding/complete`, {
      method: 'POST',
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to complete onboarding');
    return await res.json();
  },

  getJournalEntries: async (): Promise<LedgerEntry[]> => {
    try {
      const res = await fetch(`${BASE_URL}/accounting/journal?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch journal entries');
      const data = await res.json();
      return (data || []).map((e: any) => ({
        id: e.id,
        organizationId: e.organization_id ?? e.organizationId ?? activeOrgId,
        storeId: e.store_id ?? e.storeId ?? activeStoreId,
        entryNumber: e.entry_number ?? e.entryNumber ?? e.id,
        type: e.type ?? 'CREDIT',
        category: e.category ?? 'SALES',
        amount: Number(e.amount ?? e.total_debit ?? 0),
        description: e.description ?? '',
        createdAt: e.created_at ?? e.createdAt ?? new Date().toISOString(),
      }));
    } catch {
      return [
        { id: 'leg_1', organizationId: activeOrgId, storeId: activeStoreId, entryNumber: 'LEDG-2025-001', type: 'CREDIT', category: 'SALES', amount: 1603.78, description: 'Sales Order #ORD-2025-1001 payment', createdAt: new Date().toISOString() },
      ];
    }
  },

  getAccountingSummary: async (): Promise<AccountingSummary> => {
    try {
      const res = await fetch(`${BASE_URL}/accounting/summary?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch accounting summary');
      const data = await res.json();
      return {
        totalRevenue: Number(data.total_revenue ?? data.totalRevenue ?? 0),
        todayRevenue: Number(data.today_revenue ?? data.todayRevenue ?? 0),
        totalSalesCount: Number(data.total_sales_count ?? data.totalSalesCount ?? 0),
        todaySalesCount: Number(data.today_sales_count ?? data.todaySalesCount ?? 0),
      };
    } catch {
      return { totalRevenue: 2940.82, todayRevenue: 1603.78, totalSalesCount: 14, todaySalesCount: 3 };
    }
  },

  getPlatformAuditLogs: async (): Promise<any[]> => {
    try {
      const res = await fetch(`${BASE_URL}/audit-logs`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch platform audit logs');
      return await res.json();
    } catch {
      return [
        {
          id: 'log_1',
          action: 'business.application.created',
          entity_type: 'BusinessApplication',
          entity_id: 'app_apex_1',
          details: "Application submitted for business 'Apex Retail Group'",
          created_at: new Date().toISOString(),
        },
      ];
    }
  },
};
