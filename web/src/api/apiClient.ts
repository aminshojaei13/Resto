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
      return await res.json();
    } catch {
      // Return fallback mock data if backend server is not running
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
        {
          id: 'org_braveboy',
          name: 'BraveBoy Electronics',
          code: 'BBE',
          currencySymbol: '$',
          currencyCode: 'USD',
          subscriptionTier: 'PRO',
          stores: [
            {
              id: 'store_bb_1',
              organizationId: 'org_braveboy',
              name: 'Tech Hub Metro',
              code: 'BBE-MTR',
              warehouses: [
                { id: 'wh_bb_1', storeId: 'store_bb_1', organizationId: 'org_braveboy', name: 'Metro Depot', code: 'WH-BB1' },
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
      return await res.json();
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
        {
          id: 'prod_3',
          organizationId: activeOrgId,
          sku: 'APX-MOB-003',
          barcode: '880609123403',
          name: 'Apex Phone 15 Pro',
          description: 'Flagship smartphone with triple lens camera and 120Hz AMOLED display.',
          price: 999.0,
          costPrice: 620.0,
          category: 'Smartphones',
          unit: 'pcs',
          stockQuantityByWarehouse: { wh_apex_1a: 18, wh_apex_1b: 9 },
        },
      ];
    }
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
    try {
      const res = await fetch(`${BASE_URL}/orders/checkout`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ ...payload, org_id: activeOrgId, store_id: activeStoreId }),
      });
      return await res.json();
    } catch {
      return {
        id: 'ord_' + Date.now(),
        orderNumber: 'ORD-2025-' + Math.floor(Math.random() * 9000 + 1000),
        organizationId: activeOrgId,
        storeId: activeStoreId,
        warehouseId: 'wh_apex_1a',
        customerName: payload.customer_name || 'Walk-in Customer',
        items: payload.items,
        subtotal: 1000,
        discountAmount: 0,
        taxAmount: 80,
        totalAmount: 1080,
        paymentMethod: payload.payment_method,
        paymentStatus: 'PAID',
        fulfillmentStatus: 'COMPLETED',
        createdAt: new Date().toISOString(),
      };
    }
  },

  getCustomers: async (): Promise<Customer[]> => {
    try {
      const res = await fetch(`${BASE_URL}/customers?org_id=${activeOrgId}`, { headers: getHeaders() });
      return await res.json();
    } catch {
      return [
        { id: 'cust_1', organizationId: activeOrgId, name: 'Sarah Connor', email: 'sarah@example.com', phone: '+1 (555) 234-5678', totalPurchases: 3548.5, loyaltyPoints: 350 },
        { id: 'cust_2', organizationId: activeOrgId, name: 'John Smith', email: 'john@techcorp.io', phone: '+1 (555) 987-6543', totalPurchases: 1899.9, loyaltyPoints: 180 },
      ];
    }
  },

  getJournalEntries: async (): Promise<LedgerEntry[]> => {
    try {
      const res = await fetch(`${BASE_URL}/accounting/journal?org_id=${activeOrgId}`, { headers: getHeaders() });
      return await res.json();
    } catch {
      return [
        { id: 'leg_1', organizationId: activeOrgId, storeId: activeStoreId, entryNumber: 'LEDG-2025-001', type: 'CREDIT', category: 'SALES', amount: 1603.78, description: 'Sales Order #ORD-2025-1001 payment', createdAt: new Date().toISOString() },
        { id: 'leg_2', organizationId: activeOrgId, storeId: activeStoreId, entryNumber: 'LEDG-2025-002', type: 'DEBIT', category: 'EXPENSE', amount: 350.0, description: 'Store Utility Bill', createdAt: new Date().toISOString() },
      ];
    }
  },

  getAccountingSummary: async (): Promise<AccountingSummary> => {
    try {
      const res = await fetch(`${BASE_URL}/accounting/summary?org_id=${activeOrgId}`, { headers: getHeaders() });
      return await res.json();
    } catch {
      return { totalRevenue: 2940.82, todayRevenue: 1603.78, totalSalesCount: 14, todaySalesCount: 3 };
    }
  },
};
