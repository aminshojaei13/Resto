export interface Organization {
  id: string;
  name: string;
  code: string;
  logoUrl?: string;
  currencySymbol: string;
  currencyCode: string;
  subscriptionTier: string;
  stores?: Store[];
}

export interface Store {
  id: string;
  organizationId: string;
  name: string;
  code: string;
  address?: string;
  phone?: string;
  warehouses?: Warehouse[];
}

export interface Warehouse {
  id: string;
  storeId: string;
  organizationId: string;
  name: string;
  code: string;
}

export interface Product {
  id: string;
  organizationId: string;
  sku: string;
  barcode: string;
  name: string;
  description?: string;
  price: number;
  costPrice: number;
  category: string;
  unit: string;
  imageUrl?: string;
  stockQuantityByWarehouse?: Record<string, number>;
}

export interface CartItem {
  id: string;
  productId: string;
  variantId?: string;
  productName: string;
  sku: string;
  barcode: string;
  price: number;
  quantity: number;
  discountPercent: number;
  taxRate: number;
}

export interface Customer {
  id: string;
  organizationId: string;
  name: string;
  email?: string;
  phone?: string;
  address?: string;
  totalPurchases: number;
  loyaltyPoints: number;
}

export interface Supplier {
  id: string;
  organizationId: string;
  name: string;
  email?: string;
  phone?: string;
  address?: string;
  purchases?: any[];
  createdAt?: string;
}

export interface PurchaseItem {
  id?: string;
  purchaseId?: string;
  productId: string;
  productName?: string;
  quantity: number;
  unitCost: number;
  totalCost: number;
}

export interface Purchase {
  id: string;
  purchaseNumber: string;
  organizationId: string;
  storeId: string;
  warehouseId: string;
  supplierId: string;
  supplierName?: string;
  supplier?: Supplier;
  items: PurchaseItem[];
  totalAmount: number;
  status: 'ORDERED' | 'RECEIVED' | 'CANCELLED';
  paymentStatus: 'UNPAID' | 'PARTIAL' | 'PAID';
  createdAt?: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sku: string;
  unitPrice: number;
  quantity: number;
  totalPrice: number;
}

export interface SalesOrder {
  id: string;
  orderNumber: string;
  organizationId: string;
  storeId: string;
  warehouseId: string;
  customerId?: string;
  customerName: string;
  items: OrderItem[];
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  paymentMethod: string;
  paymentStatus: string;
  fulfillmentStatus: string;
  notes?: string;
  createdAt: string;
}

export interface LedgerEntry {
  id: string;
  organizationId: string;
  storeId: string;
  entryNumber: string;
  type: 'DEBIT' | 'CREDIT';
  category: string;
  amount: number;
  description: string;
  createdAt: string;
}

export interface AccountingSummary {
  totalRevenue: number;
  todayRevenue: number;
  totalSalesCount: number;
  todaySalesCount: number;
}
