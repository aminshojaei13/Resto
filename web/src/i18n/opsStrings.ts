/**
 * Every user-facing string for the operational screens: units, stock,
 * purchasing, receiving, orders, message import, tax and expenses.
 *
 * Persian and English are kept strictly apart, as in authStrings.ts. Nothing
 * here exposes an implementation term (unit_code, base_unit, tenant, enum, …).
 */

export type Language = 'fa' | 'en';

export interface OpsStrings {
  /* ------------------------------------------------------------- units */
  unitOfProduct: string;
  chooseUnit: string;
  unitRequired: string;
  unitInvalid: string;
  quantityWithUnit: string;

  /* --------------------------------------------------------- inventory */
  currentStockTitle: string;
  currentStockSubtitle: string;
  increaseStockTitle: string;
  increaseStockSubtitle: string;
  warehouse: string;
  selectWarehouse: string;
  noWarehouseTitle: string;
  noWarehouseBody: string;
  createWarehouse: string;
  quantity: string;
  notes: string;
  reasons: string;
  submitStockIn: string;
  submitting: string;
  stockInDone: string;
  availableStock: string;
  reservedStock: string;
  movementHistory: string;
  stockMovementsTitle: string;
  stockMovementsSubtitle: string;
  inStock: string;
  outOfStock: string;
  adjustment: string;
  transfer: string;

  /* -------------------------------------------------------- purchasing */
  purchasesTitle: string;
  purchasesSubtitle: string;
  newPurchase: string;
  purchaseNumber: string;
  purchaseDate: string;
  supplier: string;
  purchaseTotal: string;
  paymentStatus: string;
  receivingStatus: string;
  markPaid: string;
  viewPurchase: string;
  ordered: string;
  partiallyReceived: string;
  received: string;
  unpaid: string;
  partialPaid: string;
  paid: string;

  /* --------------------------------------------------------- receiving */
  receivingTitle: string;
  receivingSubtitle: string;
  receivingQueueEmpty: string;
  receivingQueueEmptyBody: string;
  destinationWarehouse: string;
  orderedQty: string;
  receivedQty: string;
  outstandingQty: string;
  submitReceiving: string;
  receivingDone: string;
  receiveAll: string;
  nothingAwaiting: string;

  /* ------------------------------------------------------------ orders */
  ordersTitle: string;
  ordersSubtitle: string;
  searchOrders: string;
  filterAll: string;
  filterNew: string;
  filterPendingPayment: string;
  filterPaid: string;
  filterPreparing: string;
  filterCompleted: string;
  filterCancelled: string;
  orderNumber: string;
  itemsCount: string;
  source: string;
  registerOrder: string;
  orderDetail: string;
  customer: string;
  timeline: string;
  subtotal: string;
  discount: string;
  tax: string;
  total: string;
  pay: string;
  prepare: string;
  cancel: string;
  refund: string;
  view: string;
  noOrders: string;
  noOrdersBody: string;
  sourcePos: string;
  sourceWeb: string;
  sourceMessage: string;
  statusNew: string;
  statusPreparing: string;
  statusCompleted: string;
  statusCancelled: string;
  statusPending: string;
  statusRefunded: string;

  /* ----------------------------------------------------------- message */
  messageTitle: string;
  messageSubtitle: string;
  messageStep1: string;
  messageStep2: string;
  messageStep3: string;
  messageStep4: string;
  messageStep5: string;
  messageStep6: string;
  messageInputLabel: string;
  messageInputPlaceholder: string;
  reviewMessage: string;
  showExamples: string;
  hideExamples: string;
  examplesTitle: string;
  parsedDraft: string;
  unmatchedLines: string;
  unmatchedExplain: string;
  fixAndCheckout: string;
  nothingToReview: string;

  /* --------------------------------------------------------------- tax */
  businessSettingsTitle: string;
  businessSettingsSubtitle: string;
  defaultOrderTax: string;
  defaultOrderTaxHint: string;
  saveSettings: string;
  saving: string;
  settingsSaved: string;
  taxFromBusiness: string;
  taxOverridable: string;
  taxNotOverridable: string;
  orderTaxRate: string;
  useBusinessRate: string;

  /* ---------------------------------------------------------- expense */
  expensesTitle: string;
  expensesSubtitle: string;
  expensesPurpose: string;
  expensesVsPurchase: string;
  newExpense: string;
  expenseTitle: string;
  expenseTitlePlaceholder: string;
  category: string;
  paymentMethod: string;
  amount: string;
  date: string;
  attachment: string;
  monthTotal: string;
  todayTotal: string;
  byCategory: string;
  trend: string;
  vsLastMonth: string;
  automationNoteTitle: string;
  automationNoteBody: string;
}

const fa: OpsStrings = {
  unitOfProduct: 'واحد کالا',
  chooseUnit: 'انتخاب واحد',
  unitRequired: 'واحد کالا را انتخاب کنید.',
  unitInvalid: 'واحد انتخاب‌شده معتبر نیست.',
  quantityWithUnit: 'مقدار',

  currentStockTitle: 'موجودی فعلی انبار',
  currentStockSubtitle: 'الان چه مقدار از هر کالا در هر انبار دارید.',
  increaseStockTitle: 'ورود / افزایش موجودی',
  increaseStockSubtitle: 'ثبت کالایی که بدون خرید وارد انبار شده است.',
  warehouse: 'انبار',
  selectWarehouse: 'انبار را انتخاب کنید',
  noWarehouseTitle: 'برای ثبت موجودی ابتدا باید یک انبار داشته باشید.',
  noWarehouseBody: 'موجودی هر کالا به یک انبار تعلق دارد. برای شروع، یک انبار برای این کسب‌وکار بسازید.',
  createWarehouse: 'ایجاد انبار',
  quantity: 'مقدار',
  notes: 'توضیحات',
  reasons: 'علت',
  submitStockIn: 'ثبت افزایش موجودی',
  submitting: 'در حال ثبت...',
  stockInDone: 'موجودی با موفقیت افزایش یافت.',
  availableStock: 'موجودی قابل فروش',
  reservedStock: 'رزرو شده',
  movementHistory: 'گردش موجودی',
  stockMovementsTitle: 'گردش موجودی',
  stockMovementsSubtitle: 'هر ورود و خروج کالا با علت و زمان ثبت شده است.',
  inStock: 'ورود',
  outOfStock: 'خروج',
  adjustment: 'اصلاح',
  transfer: 'انتقال',

  purchasesTitle: 'سفارش خرید',
  purchasesSubtitle: 'آنچه از تأمین‌کننده خریده‌اید. ثبت خرید، موجودی انبار را تغییر نمی‌دهد.',
  newPurchase: 'ثبت سفارش خرید',
  purchaseNumber: 'شماره سفارش خرید',
  purchaseDate: 'تاریخ خرید',
  supplier: 'تأمین‌کننده',
  purchaseTotal: 'مبلغ خرید',
  paymentStatus: 'وضعیت پرداخت',
  receivingStatus: 'وضعیت دریافت',
  markPaid: 'ثبت پرداخت',
  viewPurchase: 'مشاهده',
  ordered: 'ثبت شده',
  partiallyReceived: 'دریافت جزئی',
  received: 'دریافت کامل',
  unpaid: 'پرداخت نشده',
  partialPaid: 'پرداخت جزئی',
  paid: 'پرداخت شده',

  receivingTitle: 'دریافت کالا',
  receivingSubtitle: 'ثبت کالایی که از تأمین‌کننده رسیده و به انبار اضافه شده است.',
  receivingQueueEmpty: 'سفارش خرید در انتظار دریافت نیست',
  receivingQueueEmptyBody: 'همه سفارش‌های خرید دریافت شده‌اند.',
  destinationWarehouse: 'انبار مقصد',
  orderedQty: 'سفارش‌داده‌شده',
  receivedQty: 'دریافت‌شده',
  outstandingQty: 'باقی‌مانده',
  submitReceiving: 'ثبت دریافت',
  receivingDone: 'دریافت کالا ثبت شد و به موجودی انبار اضافه گردید.',
  receiveAll: 'دریافت همه اقلام باقی‌مانده',
  nothingAwaiting: 'کالایی برای دریافت باقی نمانده است.',

  ordersTitle: 'سفارش‌ها',
  ordersSubtitle: 'همه سفارش‌های ثبت‌شده با وضعیت پرداخت، وضعیت انجام و منبع ثبت.',
  searchOrders: 'جستجو با شماره سفارش، نام یا شماره تماس مشتری',
  filterAll: 'همه',
  filterNew: 'جدید',
  filterPendingPayment: 'در انتظار پرداخت',
  filterPaid: 'پرداخت شده',
  filterPreparing: 'آماده‌سازی',
  filterCompleted: 'تکمیل شده',
  filterCancelled: 'لغو شده',
  orderNumber: 'شماره سفارش',
  itemsCount: 'تعداد اقلام',
  source: 'منبع',
  registerOrder: 'ثبت سفارش',
  orderDetail: 'جزئیات سفارش',
  customer: 'مشتری',
  timeline: 'روند سفارش',
  subtotal: 'جمع کالاها',
  discount: 'تخفیف',
  tax: 'مالیات',
  total: 'مبلغ نهایی',
  pay: 'پرداخت',
  prepare: 'آماده‌سازی',
  cancel: 'لغو',
  refund: 'استرداد',
  view: 'مشاهده',
  noOrders: 'هنوز سفارشی ثبت نشده است',
  noOrdersBody: 'سفارش‌های ثبت‌شده از فروشگاه یا از پیام مشتری اینجا فهرست می‌شوند.',
  sourcePos: 'فروشگاه',
  sourceWeb: 'وب',
  sourceMessage: 'پیام مشتری',
  statusNew: 'جدید',
  statusPreparing: 'آماده‌سازی',
  statusCompleted: 'تکمیل شده',
  statusCancelled: 'لغو شده',
  statusPending: 'در انتظار پرداخت',
  statusRefunded: 'مسترد شده',

  messageTitle: 'ثبت سفارش از پیام',
  messageSubtitle: 'متن پیام مشتری را وارد کنید تا سفارش پیش‌نویس شود؛ سپس آن را بررسی و اصلاح کنید.',
  messageStep1: 'ورود پیام',
  messageStep2: 'شناسایی مشتری',
  messageStep3: 'شناسایی کالاها',
  messageStep4: 'بررسی سفارش',
  messageStep5: 'اصلاح در صورت نیاز',
  messageStep6: 'ثبت سفارش',
  messageInputLabel: 'متن پیام مشتری را وارد کنید',
  messageInputPlaceholder: 'سلام\n۲ عدد قهوه اسپرسو\n۱ بسته شیر\nلطفاً برای من ثبت کنید.',
  reviewMessage: 'بررسی پیام',
  showExamples: 'نمایش نمونه پیام',
  hideExamples: 'پنهان کردن نمونه',
  examplesTitle: 'پیام‌هایی که درست خوانده می‌شوند',
  parsedDraft: 'پیش‌نویس سفارش',
  unmatchedLines: 'قالب‌هایی که خوانده نشدند',
  unmatchedExplain: 'این سطرها به کالایی در فروشگاه نخوردند. آن‌ها را اصلاح کنید یا حذف کنید.',
  fixAndCheckout: 'اصلاح و ثبت سفارش',
  nothingToReview: 'هنوز پیامی بررسی نشده است.',

  businessSettingsTitle: 'تنظیمات کسب‌وکار',
  businessSettingsSubtitle: 'تنظیماتی که روی همه سفارش‌های جدید اعمال می‌شود.',
  defaultOrderTax: 'مالیات پیش‌فرض سفارش',
  defaultOrderTaxHint: 'این نرخ روی هر سفارش جدید اعمال می‌شود، مگر مدیر کسب‌وکار برای یک سفارش نرخ دیگری تعیین کند.',
  saveSettings: 'ذخیره تنظیمات',
  saving: 'در حال ذخیره...',
  settingsSaved: 'تنظیمات کسب‌وکار ذخیره شد.',
  taxFromBusiness: 'نرخ پیش‌فرض کسب‌وکار',
  taxOverridable: 'مدیران می‌توانند برای یک سفارش نرخ دیگری تعیین کنند.',
  taxNotOverridable: 'کارمندان فروش نرخ مالیات را تغییر نمی‌دهند؛ همیشه نرخ کسب‌وکار اعمال می‌شود.',
  orderTaxRate: 'نرخ مالیات',
  useBusinessRate: 'استفاده از نرخ کسب‌وکار',

  expensesTitle: 'هزینه‌ها',
  expensesSubtitle: 'خرج‌های جاری کسب‌وکار، به‌جز خرید کالا از تأمین‌کننده.',
  expensesPurpose:
    'هزینه جاری یعنی پولی که کسب‌وکار برای ادامه کار خرج می‌کند: اجاره، اینترنت، برق، حمل‌ونقل، تبلیغات، حقوق و تعمیرات.',
  expensesVsPurchase:
    'خرید کالا از تأمین‌کننده «سفارش خرید» است و موجودی انبار را زیاد می‌کند. هزینه جاری این کار را نمی‌کند و فقط هزینه دوره است.',
  newExpense: 'ثبت هزینه',
  expenseTitle: 'عنوان هزینه',
  expenseTitlePlaceholder: 'مثال: قبض برق این ماه',
  category: 'دسته‌بندی',
  paymentMethod: 'روش پرداخت',
  amount: 'مبلغ',
  date: 'تاریخ',
  attachment: 'پیوست رسید',
  monthTotal: 'هزینه این ماه',
  todayTotal: 'هزینه امروز',
  byCategory: 'هزینه بر اساس دسته‌بندی',
  trend: 'روند هزینه‌ها',
  vsLastMonth: 'نسبت به ماه گذشته',
  automationNoteTitle: 'کدام هزینه‌ها خودکار ثبت می‌شوند؟',
  automationNoteBody:
    'هیچ هزینه‌ای به‌صورت خودکار ثبت نمی‌شود، چون مبلغ واقعی اجاره، برق و اینترنت فقط با خود شما معلوم است. اگر روزی ارتباطی با درگاه پرداخت یا شرکت حمل‌ونقل داشته باشیم، همان مبلغ واقعی را از همان سند می‌خوانیم و هزینه را خودکار ثبت می‌کنیم — نه یک عدد تخمینی.',
};

const en: OpsStrings = {
  unitOfProduct: 'Unit',
  chooseUnit: 'Select a unit',
  unitRequired: 'Choose a unit for this product.',
  unitInvalid: 'That unit is not valid.',
  quantityWithUnit: 'Quantity',

  currentStockTitle: 'Current Stock',
  currentStockSubtitle: 'What you hold right now, in each warehouse.',
  increaseStockTitle: 'Stock In',
  increaseStockSubtitle: 'Record goods that arrived without a purchase order.',
  warehouse: 'Warehouse',
  selectWarehouse: 'Choose a warehouse',
  noWarehouseTitle: 'You need a warehouse before you can record stock.',
  noWarehouseBody:
    'Stock always belongs to a warehouse. Create one for this business to get started.',
  createWarehouse: 'Create warehouse',
  quantity: 'Quantity',
  notes: 'Notes',
  reasons: 'Reason',
  submitStockIn: 'Record stock in',
  submitting: 'Saving...',
  stockInDone: 'Stock increased.',
  availableStock: 'Available',
  reservedStock: 'Reserved',
  movementHistory: 'Stock movements',
  stockMovementsTitle: 'Stock Movements',
  stockMovementsSubtitle: 'Every movement is recorded with its reason and time.',
  inStock: 'In',
  outOfStock: 'Out',
  adjustment: 'Adjustment',
  transfer: 'Transfer',

  purchasesTitle: 'Purchase Orders',
  purchasesSubtitle: 'What you agreed to buy from a supplier. Ordering does not move stock.',
  newPurchase: 'New purchase order',
  purchaseNumber: 'PO number',
  purchaseDate: 'Purchase date',
  supplier: 'Supplier',
  purchaseTotal: 'Purchase total',
  paymentStatus: 'Payment',
  receivingStatus: 'Receiving',
  markPaid: 'Record payment',
  viewPurchase: 'View',
  ordered: 'Ordered',
  partiallyReceived: 'Partly received',
  received: 'Received',
  unpaid: 'Unpaid',
  partialPaid: 'Partly paid',
  paid: 'Paid',

  receivingTitle: 'Receive Stock',
  receivingSubtitle: 'Record goods that arrived from the supplier and went into a warehouse.',
  receivingQueueEmpty: 'Nothing is waiting to be received',
  receivingQueueEmptyBody: 'Every purchase order has been received.',
  destinationWarehouse: 'Destination warehouse',
  orderedQty: 'Ordered',
  receivedQty: 'Received',
  outstandingQty: 'Outstanding',
  submitReceiving: 'Record receiving',
  receivingDone: 'Receiving recorded and stock added.',
  receiveAll: 'Receive everything outstanding',
  nothingAwaiting: 'Nothing left to receive.',

  ordersTitle: 'Orders',
  ordersSubtitle: 'Every registered order with its payment, fulfilment and source.',
  searchOrders: 'Search by order number, customer name or phone',
  filterAll: 'All',
  filterNew: 'New',
  filterPendingPayment: 'Awaiting payment',
  filterPaid: 'Paid',
  filterPreparing: 'Preparing',
  filterCompleted: 'Completed',
  filterCancelled: 'Cancelled',
  orderNumber: 'Order number',
  itemsCount: 'Items',
  source: 'Source',
  registerOrder: 'Register Order',
  orderDetail: 'Order detail',
  customer: 'Customer',
  timeline: 'Timeline',
  subtotal: 'Subtotal',
  discount: 'Discount',
  tax: 'Tax',
  total: 'Total',
  pay: 'Pay',
  prepare: 'Prepare',
  cancel: 'Cancel',
  refund: 'Refund',
  view: 'View',
  noOrders: 'No orders registered yet',
  noOrdersBody: 'Orders placed at the counter or imported from a message appear here.',
  sourcePos: 'Counter',
  sourceWeb: 'Web',
  sourceMessage: 'Customer message',
  statusNew: 'New',
  statusPreparing: 'Preparing',
  statusCompleted: 'Completed',
  statusCancelled: 'Cancelled',
  statusPending: 'Awaiting payment',
  statusRefunded: 'Refunded',

  messageTitle: 'Order From Message',
  messageSubtitle: 'Paste what the customer wrote and review the draft before it is registered.',
  messageStep1: 'Enter message',
  messageStep2: 'Identify customer',
  messageStep3: 'Identify items',
  messageStep4: 'Review order',
  messageStep5: 'Correct if needed',
  messageStep6: 'Register order',
  messageInputLabel: 'Paste the customer message',
  messageInputPlaceholder: 'Hello\n2 Espresso Coffee\n1 Pack Milk\nPlease add it to my order.',
  reviewMessage: 'Review message',
  showExamples: 'Show examples',
  hideExamples: 'Hide examples',
  examplesTitle: 'Messages that read correctly',
  parsedDraft: 'Order draft',
  unmatchedLines: 'Lines that could not be read',
  unmatchedExplain:
    'These lines did not match any product. Correct them or remove them before continuing.',
  fixAndCheckout: 'Correct and register order',
  nothingToReview: 'No message reviewed yet.',

  businessSettingsTitle: 'Business Settings',
  businessSettingsSubtitle: 'Settings applied to every new order.',
  defaultOrderTax: 'Default order tax',
  defaultOrderTaxHint:
    'This rate applies to every new order unless a manager sets a different rate for one order.',
  saveSettings: 'Save settings',
  saving: 'Saving...',
  settingsSaved: 'Business settings saved.',
  taxFromBusiness: 'Business default rate',
  taxOverridable: 'Managers may set a different rate for a single order.',
  taxNotOverridable: 'Sales staff do not change the tax rate; the business rate always applies.',
  orderTaxRate: 'Tax rate',
  useBusinessRate: 'Use business rate',

  expensesTitle: 'Expenses',
  expensesSubtitle: "The business's running costs, excluding goods bought from suppliers.",
  expensesPurpose:
    'An operating expense is money spent to keep the business running: rent, internet, electricity, transport, advertising, salaries and repairs.',
  expensesVsPurchase:
    'Buying goods from a supplier is a Purchase Order and adds stock. An expense does neither — it is a cost of the period.',
  newExpense: 'Record expense',
  expenseTitle: 'Title',
  expenseTitlePlaceholder: 'e.g. This month’s electricity bill',
  category: 'Category',
  paymentMethod: 'Payment method',
  amount: 'Amount',
  date: 'Date',
  attachment: 'Receipt',
  monthTotal: 'This month',
  todayTotal: 'Today',
  byCategory: 'By category',
  trend: 'Trend',
  vsLastMonth: 'vs last month',
  automationNoteTitle: 'Which expenses are recorded automatically?',
  automationNoteBody:
    'None are recorded automatically, because the real amount of rent, electricity or internet is only known to you. If we ever connect a payment gateway or a courier, we read the actual fee from that document and record the expense from it — never a guessed number.',
};

const strings: Record<Language, OpsStrings> = { fa, en };

export const opsStrings = (language: Language): OpsStrings => strings[language];

/**
 * Realistic example messages, shown so the person knows what format reads
 * correctly. Deliberately written the way customers actually write, with no
 * internal key/value syntax exposed.
 */
export const messageExamples = (language: Language): string[] =>
  language === 'fa'
    ? [
        'سلام\n۲ عدد قهوه اسپرسو\n۱ بسته شیر\nلطفاً برای من ثبت کنید.',
        'مشتری: مریم رضایی\nتلفن: ۰۹۱۲۳۴۵۶۷۸۹\n۳ عدد چای سیاه\nپرداخت: کارت',
        '۱ عدد کیک تولد\n۲ عدد نوشابه\nتحویل: فردا ساعت ۱۸',
      ]
    : [
        'Hello\n2 Espresso Coffee\n1 Pack Milk\nPlease add it to my order.',
        'Customer: Maryam Rezaei\nPhone: 09123456789\n3 Black Tea\nPayment: card',
        '1 Birthday Cake\n2 Soda\nDelivery: tomorrow at 6pm',
      ];

/** Order sources this backend actually stores. */
export const orderSourceLabel = (source: string, language: Language): string => {
  const s = opsStrings(language);

  switch ((source || '').toUpperCase()) {
    case 'MESSAGE':
      return s.sourceMessage;
    case 'INSTAGRAM':
      return language === 'fa' ? 'اینستاگرام' : 'Instagram';
    case 'TELEGRAM':
      return language === 'fa' ? 'تلگرام' : 'Telegram';
    case 'WHATSAPP':
      return language === 'fa' ? 'واتس‌اپ' : 'WhatsApp';
    case 'WEB':
      return s.sourceWeb;
    case 'POS':
    default:
      return s.sourcePos;
  }
};