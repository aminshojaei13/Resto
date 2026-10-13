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
  /** Canonical unit code, e.g. "piece". The label is what a person reads. */
  unit: string;
  unitLabel?: string;
  imageUrl?: string;
  stockByWarehouse?: StockInWarehouse[];
}

export interface StockInWarehouse {
  warehouseId: string;
  warehouseName: string;
  quantity: number;
  reservedQuantity: number;
}

export interface StockRow {
  id: string;
  warehouseId: string;
  warehouseName: string;
  productId: string;
  productName: string;
  sku: string;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  unit: string;
  unitLabel: string;
}

export interface StockMovement {
  id: string;
  warehouse_id: string;
  product_id: string;
  type: string;
  quantity: number;
  unit_cost: number;
  reason: string;
  created_at: string;
}

export interface BusinessWarehouse {
  id: string;
  name: string;
  code: string;
  storeId: string;
  storeName: string;
}

export interface UnitOption {
  code: string;
  fa: string;
  en: string;
  label: string;
}

export interface BusinessSettings {
  defaultTaxRate: number;
  taxInclusivePricing: boolean;
  canOverrideTaxPerOrder: boolean;
  minTaxRate: number;
  maxTaxRate: number;
}

export interface ExpenseCategory {
  code: string;
  fa: string;
  en: string;
  label: string;
}

export interface ExpenseSummary {
  monthTotal: number;
  todayTotal: number;
  previousMonthTotal: number;
  byCategory: { category: string; label: string; total: number; entries: number }[];
  trend: { month: string; label: string; total: number }[];
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
  /** The unit the product is counted in; never a separate choice. */
  unit?: string;
  quantity: number;
  receivedQuantity: number;
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
  status: 'ORDERED' | 'PARTIALLY_RECEIVED' | 'RECEIVED' | 'CANCELLED';
  paymentStatus: 'UNPAID' | 'PARTIAL' | 'PAID';
  purchaseDate?: string;
  receivedAt?: string | null;
  createdAt?: string;
}

export interface Expense {
  id: string;
  organizationId: string;
  storeId: string;
  /** Human title, e.g. "This month's electricity bill". */
  title: string;
  category: string;
  categoryLabel?: string;
  amount: number;
  paymentMethod: string;
  date: string;
  notes?: string;
  attachmentUrl?: string | null;
  userId?: string;
  createdAt?: string;
}

export interface OrderItem {
  productId: string;
  productName: string;
  sku: string;
  /** The product's own unit, frozen at the time the order was registered. */
  unit?: string;
  unitPrice: number;
  quantity: number;
  subtotal?: number;
  discountAmount?: number;
  taxAmount?: number;
  totalPrice: number;
}

export interface OrderTimelineEvent {
  status: string;
  at: string;
  label: string;
}

export interface Paginated<T> {
  current_page: number;
  data: T[];
  last_page: number;
  per_page: number;
  total: number;
}

export interface SalesOrder {
  id: string;
  orderNumber: string;
  /** The rate actually applied to this order. */
  taxRate?: number;
  source?: string;
  itemsCount?: number;
  timeline?: OrderTimelineEvent[];
  availableActions?: string[];
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
  totalExpenses?: number;
  netProfit?: number;
  totalSalesCount: number;
  todaySalesCount: number;
}

export interface BusinessMembership {
  id: string;
  name: string;
  role: string;
  status: string;
  stores: {
    id: string;
    name: string;
    warehouses: { id: string; name: string }[];
  }[];
}

/**
 * The person who is signed in, exactly as the server describes them.
 * `name` is the server-composed full name; the UI never rebuilds it.
 */
export interface AuthUser {
  id: string;
  firstName: string | null;
  lastName: string | null;
  name: string;
  displayName: string;
  email: string;
  phone: string | null;
  status: string;
  role: string | null;
  isPlatformAdmin: boolean;
  permissions: string[];
  memberships: BusinessMembership[];
  token?: string;
}

export interface StaffMember {
  membershipId: string;
  id: string;
  firstName: string | null;
  lastName: string | null;
  name: string;
  displayName: string;
  email: string;
  phone: string | null;
  role: string;
  status: string;
  joinedAt: string | null;
  isSelf: boolean;
}

export interface ImportDatasetItem {
  id: string;
  name: string;
  sheet_index: number;
  row_count: number;
  headers: string[];
  sample_rows: string[][];
  status: string;
}

export interface ImportSourceItem {
  id: string;
  original_filename: string;
  storage_path: string;
  file_format: string;
  file_size: number;
  checksum: string;
  upload_status: string;
  created_at: string;
}

export interface ImportJobItem {
  id: string;
  organization_id: string;
  user_id: string | null;
  import_type: string;
  status: string;
  original_filename: string;
  detected_format: string;
  file_size: number;
  total_rows: number;
  processed_rows: number;
  valid_rows: number;
  invalid_rows: number;
  imported_rows: number;
  skipped_rows: number;
  failed_rows: number;
  error_summary: string | null;
  idempotency_key: string | null;
  created_at: string;
  started_at: string | null;
  completed_at: string | null;
  sources?: ImportSourceItem[];
  datasets?: ImportDatasetItem[];
}

export interface ImportTargetField {
  name: string;
  label_fa: string;
  label_en: string;
  type: string;
  required: boolean;
  description_fa: string;
  description_en: string;
  aliases: string[];
}

export type ImportMatchCategory =
  | 'exact_canonical'
  | 'exact_alias'
  | 'normalized_alias'
  | 'fuzzy_high'
  | 'ambiguous'
  | 'conflict'
  | 'unmapped';

export interface ImportMappingSuggestion {
  source_column: string;
  source_index: number;
  target_field: string | null;
  confidence: number | null;
  match_category?: ImportMatchCategory;
  is_ambiguous?: boolean;
  has_conflict?: boolean;
  matched_alias?: string | null;
  candidate_fields?: string[];
  competing_columns?: string[];
  reason_fa?: string;
  reason_en?: string;
}

export interface ImportMappingConflict {
  target_field: string;
  target_label_fa: string;
  target_label_en: string;
  competing_columns: string[];
  dominant_column: string | null;
  message_fa: string;
  message_en: string;
}

export interface ImportMappingAmbiguousHeader {
  source_column: string;
  source_index: number;
  candidate_fields: string[];
  candidate_labels_fa: string[];
  confidence: number;
  message_fa: string;
  message_en: string;
}

export interface ImportUnmappedRequiredField {
  name: string;
  label_fa: string;
  label_en: string;
  type: string;
  description_fa: string;
  description_en: string;
}

export interface ImportMappingResponse {
  job_id: string;
  import_type: string;
  status: string;
  target_fields: ImportTargetField[];
  datasets: ImportDatasetItem[];
  warehouses?: Array<{ id: string; name: string; code?: string; address?: string }>;
  stores?: Array<{ id: string; name: string; code?: string; address?: string }>;
  suppliers?: Array<{ id: string; name: string; code?: string; phone?: string }>;
  categories?: Array<{ code: string; fa: string; en: string; label?: string }>;
  active_sheet_index: number;
  headers: string[];
  sample_rows: string[][];
  suggested_mappings: ImportMappingSuggestion[];
  conflicts?: ImportMappingConflict[];
  ambiguous_headers?: ImportMappingAmbiguousHeader[];
  unmapped_required_fields?: ImportUnmappedRequiredField[];
  current_mapping: {
    sheet_index?: number;
    warehouse_id?: string | null;
    store_id?: string | null;
    supplier_id?: string | null;
    fields?: Record<string, string>;
  } | null;
}

export interface ImportPreviewRowItem {
  row_number: number;
  is_valid: boolean;
  has_match: boolean;
  errors: Array<{ field: string; code: string; message: string; value: string }>;
  warnings: Array<{ field: string; code: string; message: string; value: string }>;
  mapped_data: Record<string, any>;
}

export interface ImportPreviewResponse {
  job_id: string;
  status: string;
  total_rows: number;
  valid_rows: number;
  invalid_rows: number;
  matched_existing_rows: number;
  preview_rows: ImportPreviewRowItem[];
  can_execute: boolean;
}

export interface ImportValidationResultItem {
  id: string;
  import_job_id: string;
  row_number: number;
  field_name: string | null;
  original_value: string | null;
  error_code: string;
  message: string;
  suggested_action: string | null;
  severity: 'error' | 'warning';
  is_eligible: boolean;
}

export interface ImportTemplateInfo {
  type: string;
  filename: string;
  title_fa: string;
  title_en: string;
  description_fa: string;
  description_en: string;
  is_multiline: boolean;
  headers: string[];
  field_keys: string[];
  required: boolean[];
  sample_rows: string[][];
  domain_notes_fa: string[];
  domain_notes_en: string[];
}


