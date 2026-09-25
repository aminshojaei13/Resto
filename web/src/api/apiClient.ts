import { AccountingSummary, Customer, LedgerEntry, Organization, Product, SalesOrder, Store, Warehouse } from '../types';

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
        {
          id: 'prod_2',
          organizationId: activeOrgId,
          sku: 'APX-AUD-002',
          barcode: '880609123402',
          name: 'NoiseCancel Studio Headphones',
          description: 'Active noise cancelling wireless headphones with 30-hour battery life.',
          price: 249.99,
          costPrice: 120.0,
          category: 'Audio',
          unit: 'pcs',
          stockQuantityByWarehouse: { wh_apex_1a: 45, wh_apex_1b: 18 },
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

  getSuppliers: async (): Promise<any[]> => {
    try {
      const res = await fetch(`${BASE_URL}/suppliers?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch suppliers');
      return await res.json();
    } catch {
      return [
        { id: 'sup_1', name: 'TechImport Global Co.', email: 'sales@techimport.com', phone: '+1 (800) 555-0199', address: '500 Logistics Way, San Jose, CA' },
      ];
    }
  },

  createSupplier: async (data: any) => {
    const res = await fetch(`${BASE_URL}/suppliers`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ ...data, org_id: activeOrgId }),
    });
    return await res.json();
  },

  updateSupplier: async (id: string, data: any) => {
    const res = await fetch(`${BASE_URL}/suppliers/${id}`, {
      method: 'PUT',
      headers: getHeaders(),
      body: JSON.stringify(data),
    });
    return await res.json();
  },

  getPurchases: async (): Promise<any[]> => {
    try {
      const res = await fetch(`${BASE_URL}/purchases?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch purchases');
      return await res.json();
    } catch {
      return [];
    }
  },

  getExpenses: async (): Promise<any[]> => {
    try {
      const res = await fetch(`${BASE_URL}/expenses?org_id=${activeOrgId}`, { headers: getHeaders() });
      if (!res.ok) throw new Error('Failed to fetch expenses');
      return await res.json();
    } catch {
      return [];
    }
  },

  createExpense: async (data: any) => {
    const res = await fetch(`${BASE_URL}/expenses`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({ ...data, store_id: activeStoreId }),
    });
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
};
