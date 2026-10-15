/**
 * Bilingual help strings for the Resto web application guidance system.
 * Follows the pattern established in opsStrings.ts and authStrings.ts.
 */

export type Language = 'fa' | 'en';

export interface HelpStrings {
  /* -------------------------------------------------------- General help */
  helpMenuLabel: string;
  startTour: string;
  restartTour: string;
  tourCompleted: string;
  tourSkipped: string;
  helpPanelTitle: string;
  closeHelp: string;
  whatIsThisSection: string;
  prerequisites: string;
  steps: string;
  importantFields: string;
  afterSaving: string;
  commonErrors: string;
  tipPrefix: string;
  aboutResto: string;
  versionInfo: string;
  expandHelp: string;
  collapseHelp: string;
  noHelpAvailable: string;
  openContextualHelp: string;

  /* ------------------------------------------------------ Section titles */
  dashboardTitle: string;
  productsTitle: string;
  inventoryTitle: string;
  suppliersTitle: string;
  purchasesTitle: string;
  ordersTitle: string;
  customersTitle: string;
  expensesTitle: string;
  accountingTitle: string;
  importTitle: string;
  settingsTitle: string;
  staffTitle: string;
  messagesTitle: string;
  posTitle: string;
  receivingTitle: string;
  profileTitle: string;
  tenantsTitle: string;
  tenantsHelp: string;
  tenantsSteps: string;
  tenantsTip: string;
  auditTitle: string;
  auditHelp: string;
  auditTip: string;

  /* ----------------------------------------------------------- Dashboard */
  dashboardHelp: string;
  dashboardTip: string;

  /* ------------------------------------------------------------ Products */
  productsHelp: string;
  productsPrereq: string;
  productsSteps: string;
  productsFields: string;
  productsAfterSave: string;
  productsTip: string;

  /* ----------------------------------------------------------- Inventory */
  inventoryHelp: string;
  inventoryPrereq: string;
  inventorySteps: string;
  inventoryTip: string;

  /* ----------------------------------------------------------- Suppliers */
  suppliersHelp: string;
  suppliersSteps: string;
  suppliersTip: string;

  /* ----------------------------------------------------------- Purchases */
  purchasesHelp: string;
  purchasesSteps: string;
  purchasesTip: string;

  /* -------------------------------------------------------------- Orders */
  ordersHelp: string;
  ordersSteps: string;
  ordersTip: string;

  /* ----------------------------------------------------------- Customers */
  customersHelp: string;
  customersSteps: string;
  customersTip: string;

  /* ------------------------------------------------------------ Expenses */
  expensesHelp: string;
  expensesSteps: string;
  expensesTip: string;

  /* -------------------------------------------------- Accounting/Reports */
  accountingHelp: string;
  accountingTip: string;

  /* ------------------------------------------------------- Import Center */
  importHelp: string;
  importSteps: string;
  importTip: string;

  /* ------------------------------------------------------------ Settings */
  settingsHelp: string;
  settingsTip: string;

  /* --------------------------------------------------------------- Staff */
  staffHelp: string;
  staffSteps: string;
  staffTip: string;

  /* ------------------------------------------------------------ Messages */
  messagesHelp: string;
  messagesSteps: string;
  messagesTip: string;

  /* ----------------------------------------------------------------- POS */
  posHelp: string;
  posSteps: string;
  posTip: string;

  /* ----------------------------------------------------------- Receiving */
  receivingHelp: string;
  receivingSteps: string;
  receivingTip: string;

  /* ------------------------------------------------------------- Profile */
  profileHelp: string;
  profileTip: string;
}

export interface SectionHelpData {
  key: string;
  title: string;
  help: string;
  prerequisites?: string;
  steps?: string;
  importantFields?: string;
  afterSaving?: string;
  tip?: string;
}

const fa: HelpStrings = {
  /* -------------------------------------------------------- General help */
  helpMenuLabel: 'راهنما',
  startTour: 'شروع تور راهنما',
  restartTour: 'شروع مجدد تور راهنما',
  tourCompleted: 'تور راهنما تکمیل شد!',
  tourSkipped: 'از تور رد شدید',
  helpPanelTitle: 'راهنما و آموزش',
  closeHelp: 'بستن راهنما',
  whatIsThisSection: 'این بخش چیست؟',
  prerequisites: 'پیش‌نیازها',
  steps: 'مراحل',
  importantFields: 'فیلدهای مهم',
  afterSaving: 'بعد از ذخیره',
  commonErrors: 'خطاهای رایج',
  tipPrefix: 'نکته',
  aboutResto: 'درباره رستو',
  versionInfo: 'نسخه ۱.۰.۰',
  expandHelp: 'نمایش راهنما',
  collapseHelp: 'مخفی‌سازی راهنما',
  noHelpAvailable: 'راهنمایی برای این بخش ثبت نشده است.',
  openContextualHelp: 'مشاهده راهنمای این بخش',

  /* ------------------------------------------------------ Section titles */
  dashboardTitle: 'نمای کلی کسب‌وکار',
  productsTitle: 'کالاها',
  inventoryTitle: 'موجودی فعلی',
  suppliersTitle: 'تأمین‌کنندگان',
  purchasesTitle: 'سفارش‌های خرید',
  ordersTitle: 'سفارش‌ها',
  customersTitle: 'مشتریان',
  expensesTitle: 'هزینه‌ها',
  accountingTitle: 'دفتر کل و گزارش‌ها',
  importTitle: 'مرکز درون‌ریزی داده‌ها',
  settingsTitle: 'تنظیمات کسب‌وکار',
  staffTitle: 'کارمندان',
  messagesTitle: 'ثبت سفارش از پیام',
  posTitle: 'صندوق فروش',
  receivingTitle: 'دریافت کالا',
  profileTitle: 'حساب کاربری من',

  /* ----------------------------------------------------------- Dashboard */
  dashboardHelp:
    'نمای کلی شاخص‌های کلیدی کسب‌وکار، گردش فروش اخیر، وضعیت درآمد و هشدارهای مهم عملیاتی را به صورت متمرکز ارائه می‌دهد.',
  dashboardTip:
    'برای نظارت دقیق‌تر، ارقام فروش و تعداد سفارش‌های امروز را با روزهای گذشته مقایسه کنید.',

  /* ------------------------------------------------------------ Products */
  productsHelp:
    'تعریف و مدیریت اطلاعات کالاها، واحدهای سنجش، نرخ‌های فروش و کدهای اختصاصی (SKU) در این بخش انجام می‌شود.',
  productsPrereq:
    'قبل از تعریف کالا، واحدهای سنجش مورد استفاده (مانند عدد، کیلوگرم یا بسته) و دسته‌بندی‌ها را در نظر داشته باشید.',
  productsSteps:
    '۱. دکمه کالای جدید را انتخاب کنید.\n۲. نام، واحد سنجش و قیمت فروش را وارد نمایید.\n۳. در صورت تمایل کد SKU و دسته‌بندی را مشخص نموده و ذخیره کنید.',
  productsFields:
    'نام کالا، واحد سنجش، قیمت فروش، کد کالا (SKU) و دسته‌بندی.',
  productsAfterSave:
    'کالا بلافاصله در فاکتورهای فروش، صندوق (POS)، سفارش‌های خرید و کنترل موجودی قابل انتخاب خواهد بود.',
  productsTip:
    'تخصیص بارکد و شناسه SKU منحصربه‌فرد، سرعت ثبت اقلام در صندوق و جستجو را دوچندان می‌کند.',

  /* ----------------------------------------------------------- Inventory */
  inventoryHelp:
    'مشاهده لحظه‌ای موجودی کالاها در هر انبار و ثبت تنظیمات دستی یا ورود و خروج موجودی در این قسمت قرار دارد.',
  inventoryPrereq:
    'برای ثبت ورود کالا یا انتقال، ابتدا باید حداقل یک انبار و کالاهای مرتبط در سیستم تعریف شده باشند.',
  inventorySteps:
    '۱. انبار مورد نظر را تعیین نمایید.\n۲. کالا و مقدار تعدادی یا وزنی ورودی را مشخص کنید.\n۳. علت تغییر موجودی و یادداشت تکمیلی را نوشته و ثبت فرمایید.',
  inventoryTip:
    'برای بالا بردن موجودی ناشی از اقلام خریداری‌شده، از بخش «دریافت کالا» استفاده کنید تا زنجیره انبارداری منظم بماند.',

  /* ----------------------------------------------------------- Suppliers */
  suppliersHelp:
    'فهرست و اطلاعات تماس تأمین‌کنندگان کالا جهت تسریع در سفارش‌گذاری خرید و پیگیری مطالبات و بدهی‌ها در این صفحه ثبت می‌شود.',
  suppliersSteps:
    '۱. روی تأمین‌کننده جدید کلیک کنید.\n۲. نام شرکت یا شخص طرف قرارداد و شماره تماس معتبر را وارد کنید.\n۳. آدرس و یادداشت‌های تجاری را تکمیل و ذخیره فرمایید.',
  suppliersTip:
    'ثبت اطلاعات دقیق تماس و شرایط تسویه به هماهنگی بهتر تأمین کالا کمک شایانی می‌کند.',

  /* ----------------------------------------------------------- Purchases */
  purchasesHelp:
    'صدور و بایگانی سفارش‌های خرید از تأمین‌کنندگان؛ توجه داشته باشید که ثبت فاکتور خرید به‌تنهایی موجودی انبار را تغییر نمی‌دهد.',
  purchasesSteps:
    '۱. تأمین‌کننده را انتخاب نمایید.\n۲. اقلام درخواستی، تعداد و قیمت واحد خرید را ثبت کنید.\n۳. پس از رسیدن فیزیکی اقلام، دریافت آن را در صفحه دریافت کالا تایید نمایید.',
  purchasesTip:
    'سفارش خرید تنها تعهد مالی را ثبت می‌کند؛ موجودی انبار پس از اقدام در صفحه «دریافت کالا» به‌روز خواهد شد.',

  /* -------------------------------------------------------------- Orders */
  ordersHelp:
    'پیگیری چرخه حیات سفارش‌های فروش از زمان ثبت تا تحویل، تفکیک بر اساس وضعیت پرداخت و صدور فاکتور رسمی مشتری.',
  ordersSteps:
    '۱. سفارش مورد نظر را از فهرست باز کنید یا سفارش جدید ثبت نمایید.\n۲. اقلام و جزئیات تخفیف و مالیات را بررسی کنید.\n۳. وضعیت پرداخت و پیشرفت سفارش را به‌روزرسانی نمایید.',
  ordersTip:
    'بررسی منبع سفارش (صندوق، پیام‌رسان‌ها یا وب) به شناخت کانال‌های سودآورتر فروش کمک می‌کند.',

  /* ----------------------------------------------------------- Customers */
  customersHelp:
    'پایگاه داده مشتریان برای ثبت سوابق خرید، اطلاعات تماس، مانده‌حساب و تسهیل در صدور فاکتورهای بعدی مشتریان.',
  customersSteps:
    '۱. روی دکمه ثبت مشتری جدید بزنید.\n۲. نام، شماره تماس و در صورت نیاز آدرس تحویل را وارد کنید.\n۳. مشتری را ذخیره کرده و در فاکتورهای فروش بعدی انتخاب نمایید.',
  customersTip:
    'ذخیره شماره همراه مشتری امکان صدور آنی فاکتور و پیگیری سفارش‌های بعدی را ساده‌تر می‌کند.',

  /* ------------------------------------------------------------ Expenses */
  expensesHelp:
    'ثبت هزینه‌های عملیاتی و جاری کسب‌وکار نظیر اجاره، آب، برق، اینترنت، ایاب‌وذهاب و حقوق پرسنل (متمایز از خرید کالای انبار).',
  expensesSteps:
    '۱. عنوان هزینه، مبلغ و دسته‌بندی مرتبط را انتخاب کنید.\n۲. روش پرداخت و تاریخ تحقق هزینه را مشخص نمایید.\n۳. در صورت وجود، تصویر قبض یا فاکتور را پیوست کرده و ثبت کنید.',
  expensesTip:
    'خرید اقلام فروشی باید در سفارش خرید ثبت شود؛ هزینه‌های اداری و عمومی را در این بخش بیاورید.',

  /* -------------------------------------------------- Accounting/Reports */
  accountingHelp:
    'صورت‌های مالی، تراز آزمایشی، ترازنامه، گزارش سود و زیان و دفاتر قانونی مبتنی بر اصول استاندارد حسابداری دوطرفه.',
  accountingTip:
    'صحت گزارش‌های مالی وابسته به ثبت منظم هزینه‌ها، سفارش‌های خرید و دریافت‌های نقدی و کارتی است.',

  /* ------------------------------------------------------- Import Center */
  importHelp:
    'درون‌ریزی گروهی داده‌های کالاها، انبارها، مشتریان و تأمین‌کنندگان از فایل‌های اکسل و CSV با تطبیق هوشمند ستون‌ها.',
  importSteps:
    '۱. فایل الگو را دانلود کرده و سطرهای اطلاعاتی را تکمیل نمایید.\n۲. فایل را بارگذاری و ستون‌های مبدأ را به فیلدهای سامانه نگاشت کنید.\n۳. پیش‌نمایش و اعتبارسنجی را بررسی کرده و عملیات ورود داده را تایید کنید.',
  importTip:
    'قبل از بارگذاری نهایی، عدم تکرار شماره‌های تماس و کدهای SKU را در فایل بررسی فرمایید.',

  /* ------------------------------------------------------------ Settings */
  settingsHelp:
    'تنظیم مشخصات حقوقی سازمان، درصد مالیات بر ارزش افزوده، واحد پول پایه و ترجیحات فاکتور فروش.',
  settingsTip:
    'نرخ مالیات پیش‌فرض تعیین‌شده، مستقیماً در محاسبه اقلام فاکتورهای فروش و اسناد اعمال خواهد شد.',

  /* --------------------------------------------------------------- Staff */
  staffHelp:
    'مدیریت دسترسی‌های پرسنل، تعریف نقش‌های کاربری (مدیر، صندوق‌دار، مسئول انبار) و ارسال دعوت‌نامه سازمانی.',
  staffSteps:
    '۱. دکمه افزودن همکار را بزنید.\n۲. ایمیل یا شماره تماس همکار را به همراه نام درج کنید.\n۳. نقش و سطح دسترسی مناسب را تعیین کرده و دعوت‌نامه را بفرستید.',
  staffTip:
    'به جهت حفظ امنیت اطلاعات مالی، نقش‌ها را متناسب با وظایف دقیق هر یک از پرسنل محدود نمایید.',

  /* ------------------------------------------------------------ Messages */
  messagesHelp:
    'استخراج و تبدیل خودکار پیام‌های ثبت سفارش مشتریان در شبکه‌های اجتماعی (اینستاگرام، تلگرام و پیامک) به سفارش رسمی فروش.',
  messagesSteps:
    '۱. متن خام ارسالی مشتری را در کادر ورودی جای‌گذاری کنید.\n۲. اقلام شناسایی‌شده، تعداد و قیمت را بازبینی فرمایید.\n۳. روی دکمه ثبت نهایی سفارش کلیک کنید.',
  messagesTip:
    'قید شدن نام کالا و تعداد در متن پیام توسط مشتری، سرعت و دقت تشخیص خودکار اقلام را بالا می‌برد.',

  /* ----------------------------------------------------------------- POS */
  posHelp:
    'صندوق فروش سریع و بهینه‌سازی‌شده برای ثبت سفارش‌های حضوری پای میز یا پیشخوان با قابلیت چاپ آنی فاکتور.',
  posSteps:
    '۱. با کلیک روی کارت‌های کالا یا بارکد، اقلام را به سبد بیفزایید.\n۲. در صورت نیاز مشتری یا تخفیف را مشخص کنید.\n۳. شیوه تسویه حساب (کارت، نقد) را برگزیده و سفارش را ببندید.',
  posTip:
    'استفاده از بارکدخوان و کلیدهای میانبر، فرآیند تسویه در ساعت‌های شلوغی فروشگاه را بسیار سریع می‌کند.',

  /* ----------------------------------------------------------- Receiving */
  receivingHelp:
    'ثبت ورود فیزیکی کالاهای سفارش داده شده از تأمین‌کننده به انبار و اضافه شدن واقعی تعداد اقلام به موجودی قفسه‌ها.',
  receivingSteps:
    '۱. سفارش خرید مورد نظر را از فهرست منتظران دریافت انتخاب کنید.\n۲. انبار مقصد و مقادیر دریافتی در هر ردیف را تایید نمایید.\n۳. دکمه ثبت دریافت را بزنید تا موجودی انبار ارتقا یابد.',
  receivingTip:
    'در صورت دریافت بخشی از بار، تنها تعداد تحویل‌شده را وارد کنید؛ باقیمانده برای دریافت‌های بعدی در انتظار می‌ماند.',

  /* ------------------------------------------------------------- Profile */
  profileHelp:
    'مشاهده و به‌روزرسانی مشخصات فردی، نام نمایشی، تغییر رمز عبور و تنظیمات امنیتی حساب کاربری شما.',
  profileTip:
    'برای محافظت از حساب تجاری خود، از رمز عبور قدرتمند شامل حروف، اعداد و نمادها استفاده فرمایید.',

  /* ----------------------------------------------------- Admin Sections */
  tenantsTitle: 'مدیریت تننت‌ها و سازمان‌ها',
  tenantsHelp:
    'مدیریت سازمان‌ها، شعبه‌ها و مشتریان سازمانی ثبت‌شده در سامانه رستو، تعیین ارز پیش‌فرض و ایجاد تننت‌های جدید.',
  tenantsSteps:
    '۱. دکمه ثبت سازمان جدید را انتخاب کنید.\n۲. نام، کد شناسه و نماد واحد پولی سازمان را وارد نمایید.\n۳. ذخیره را بزنید تا محیط کسب‌وکار مستقل ایجاد شود.',
  tenantsTip:
    'کد سازمان به‌صورت یکتا شناسه اختصاصی دسترسی و تفکیک پایگاه‌داده آن تننت خواهد بود.',

  auditTitle: 'گزارش وقایع و لاگ امنیتی سیستم',
  auditHelp:
    'مشاهده تاریخچه کامل و غیرقابل تغییر تمامی رویدادها، ورودهای کاربران، تغییرات ساختاری و عملیات حساس در سراسر پلتفرم.',
  auditTip:
    'برای پیگیری امنیتی و انطباق با قوانین داده، گزارش رویدادها را بر اساس کاربر، سطح اهمیت و نوع عملیات فیلتر کنید.',
};

const en: HelpStrings = {
  /* -------------------------------------------------------- General help */
  helpMenuLabel: 'Help',
  startTour: 'Start guided tour',
  restartTour: 'Restart guided tour',
  tourCompleted: 'Tour completed!',
  tourSkipped: 'Tour skipped',
  helpPanelTitle: 'Help & Guide',
  closeHelp: 'Close help',
  whatIsThisSection: 'What is this section?',
  prerequisites: 'Prerequisites',
  steps: 'Steps',
  importantFields: 'Important fields',
  afterSaving: 'After saving',
  commonErrors: 'Common errors',
  tipPrefix: 'Tip',
  aboutResto: 'About Resto',
  versionInfo: 'Version 1.0.0',
  expandHelp: 'Show help',
  collapseHelp: 'Hide help',
  noHelpAvailable: 'Help information is not available for this section.',
  openContextualHelp: 'View help for this section',

  /* ------------------------------------------------------ Section titles */
  dashboardTitle: 'Business Overview',
  productsTitle: 'Products',
  inventoryTitle: 'Current Stock',
  suppliersTitle: 'Suppliers',
  purchasesTitle: 'Purchase Orders',
  ordersTitle: 'Orders',
  customersTitle: 'Customers',
  expensesTitle: 'Expenses',
  accountingTitle: 'Ledger & Reports',
  importTitle: 'Import Center',
  settingsTitle: 'Business Settings',
  staffTitle: 'Staff',
  messagesTitle: 'Order From Message',
  posTitle: 'Register Order (POS)',
  receivingTitle: 'Receive Stock',
  profileTitle: 'My Account',

  /* ----------------------------------------------------------- Dashboard */
  dashboardHelp:
    'Provides a real-time summary of sales, recent customer orders, core operational indicators, and business performance metrics.',
  dashboardTip:
    'Review daily order volume and compare time periods regularly to identify sales trends and peak demand.',

  /* ------------------------------------------------------------ Products */
  productsHelp:
    'Define and manage your product catalog, sales prices, measurement units, and product identifiers (SKUs).',
  productsPrereq:
    'Determine the units of measure (e.g. piece, kg, pack) and categories you need before creating products.',
  productsSteps:
    '1. Click New Product.\n2. Enter the product name, unit of measure, and sales price.\n3. Optionally assign an SKU code and category, then save.',
  productsFields:
    'Product name, unit of measure, sales price, SKU code, and category.',
  productsAfterSave:
    'The product immediately becomes available in POS, customer orders, purchase orders, and inventory tracking.',
  productsTip:
    'Assign unique SKU codes and barcodes to accelerate point-of-sale scanning and fast item search.',

  /* ----------------------------------------------------------- Inventory */
  inventoryHelp:
    'Inspect real-time stock levels across warehouses and record manual stock adjustments or inward and outward movements.',
  inventoryPrereq:
    'At least one active warehouse and defined products are required before recording stock adjustments.',
  inventorySteps:
    '1. Select the target warehouse.\n2. Choose the product and specify the quantity to add or adjust.\n3. Enter the reason and operational notes, then save.',
  inventoryTip:
    'To increase stock from supplier deliveries, use the Receive Stock screen so purchase traceability remains exact.',

  /* ----------------------------------------------------------- Suppliers */
  suppliersHelp:
    'Manage vendor records and contact details to streamline purchase order creation and monitor accounts payable.',
  suppliersSteps:
    '1. Click New Supplier.\n2. Enter the vendor company name or primary contact along with phone number.\n3. Fill in address details and notes, then save.',
  suppliersTip:
    'Keep supplier phone numbers and payment terms updated to avoid fulfillment delays.',

  /* ----------------------------------------------------------- Purchases */
  purchasesHelp:
    'Create and track purchase orders sent to vendors. Ordering goods records a commercial commitment but does NOT increase stock.',
  purchasesSteps:
    '1. Select the vendor.\n2. Add the items, ordered quantities, and agreed unit purchase prices.\n3. Save the purchase order and receive goods in the Receive Stock screen upon delivery.',
  purchasesTip:
    'Inventory levels only increment when stock is physically accepted in the Receive Stock section.',

  /* -------------------------------------------------------------- Orders */
  ordersHelp:
    'Track customer orders throughout their lifecycle from registration to fulfillment, monitor payment status, and review totals.',
  ordersSteps:
    '1. Open an existing order or register a new one.\n2. Review ordered items, applied discounts, and sales tax.\n3. Update payment status and progress the order towards fulfillment.',
  ordersTip:
    'Inspect order sources (POS, Messaging, or Web) to analyze which sales channels deliver highest volume.',

  /* ----------------------------------------------------------- Customers */
  customersHelp:
    'Maintain customer contact directories, purchase histories, and balances to deliver personalized service and faster checkout.',
  customersSteps:
    '1. Click New Customer.\n2. Enter the full name, mobile number, and delivery address.\n3. Save the customer record for rapid selection on future orders.',
  customersTip:
    'Storing accurate customer phone numbers ensures swift notifications and order traceability.',

  /* ------------------------------------------------------------ Expenses */
  expensesHelp:
    'Record day-to-day operating overhead such as rent, utility bills, internet, salaries, and marketing (distinct from stock purchases).',
  expensesSteps:
    '1. Enter the expense title, category, and monetary amount.\n2. Choose the payment method and date of expense.\n3. Attach an optional receipt slip and submit.',
  expensesTip:
    'Goods purchased for resale belong in Purchase Orders; operating costs should always be logged here.',

  /* -------------------------------------------------- Accounting/Reports */
  accountingHelp:
    'Inspect financial statements, trial balances, profit & loss reports, and balance sheets built upon a double-entry ledger.',
  accountingTip:
    'Regularly recording operating expenses and confirming paid orders keeps financial statements audit-ready.',

  /* ------------------------------------------------------- Import Center */
  importHelp:
    'Bulk import products, initial stock levels, customers, and suppliers from Excel or CSV files using smart column mapping.',
  importSteps:
    '1. Download the reference template and populate your records.\n2. Upload the file and map file columns to system fields.\n3. Validate parsed rows and confirm the import execution.',
  importTip:
    'Verify that SKU codes, barcodes, and phone numbers are unique before confirming large imports.',

  /* ------------------------------------------------------------ Settings */
  settingsHelp:
    'Configure legal organization details, default sales tax rate (VAT), base currency, and receipt print options.',
  settingsTip:
    'Configuring the correct default tax percentage ensures compliant invoices across POS and order registers.',

  /* --------------------------------------------------------------- Staff */
  staffHelp:
    'Manage staff accounts, assign granular role permissions (Admin, Cashier, Warehouse Staff), and invite team members.',
  staffSteps:
    '1. Click Invite Staff.\n2. Provide their name and email or phone number.\n3. Assign an appropriate role and send the invitation.',
  staffTip:
    'Follow the principle of least privilege by granting staff only the permissions needed for their daily tasks.',

  /* ------------------------------------------------------------ Messages */
  messagesHelp:
    'Parse customer order messages from messaging apps (WhatsApp, Telegram, Instagram, SMS) directly into formal orders.',
  messagesSteps:
    '1. Paste raw customer text into the message input field.\n2. Review extracted items, quantities, and customer details.\n3. Confirm and convert into a recorded order with one click.',
  messagesTip:
    'Clear message structure with item names and quantities yields faster, automatic line-item parsing.',

  /* ----------------------------------------------------------------- POS */
  posHelp:
    'High-speed point-of-sale interface designed for front-desk checkout, barcode scanning, and instant receipt creation.',
  posSteps:
    '1. Select items by category or scan barcodes to add to the cart.\n2. Attach customer details or adjust discounts if needed.\n3. Choose the payment method and finalize checkout.',
  posTip:
    'Use physical barcode scanners and keyboard shortcuts to maximize counter throughput during peak hours.',

  /* ----------------------------------------------------------- Receiving */
  receivingHelp:
    'Acknowledge delivery of items ordered from suppliers into specific warehouses, incrementing available physical stock.',
  receivingSteps:
    '1. Select a purchase order pending arrival.\n2. Choose the receiving warehouse and verify quantities received.\n3. Submit receiving to increment warehouse inventory.',
  receivingTip:
    'If an order is partially delivered, receive the delivered portion now; the rest remains queued for subsequent delivery.',

  /* ------------------------------------------------------------- Profile */
  profileHelp:
    'View and update personal profile details, change security credentials, and manage account preferences.',
  profileTip:
    'Use a strong, unique password with letters, numbers, and symbols to protect business operations.',

  /* ----------------------------------------------------- Admin Sections */
  tenantsTitle: 'Platform Tenants',
  tenantsHelp:
    'Manage tenant organizations, store locations, and multi-tenant instances on the Resto platform.',
  tenantsSteps:
    '1. Click New Organization.\n2. Enter the organization name, code identifier, and default currency symbol.\n3. Save to provision an isolated business tenant workspace.',
  tenantsTip:
    'The organization code serves as the unique identifier for database partition and API tenant routing.',

  auditTitle: 'Platform Audit Logs',
  auditHelp:
    'Review immutable audit logs, user sign-in activity, tenant lifecycle events, and sensitive operations across the platform.',
  auditTip:
    'Filter audit logs by actor, severity, or event action to investigate security events or compliance inquiries.',
};

const strings: Record<Language, HelpStrings> = { fa, en };

export const helpStrings = (language: Language): HelpStrings => strings[language];

/**
 * Normalizes a section key or route path (e.g., '/app/products' -> 'products')
 */
export function normalizeSectionKey(rawKey: string): string {
  if (!rawKey) return '';
  const cleaned = rawKey
    .trim()
    .toLowerCase()
    .replace(/^\/?(app\/)?/, '') // remove leading slash and optional 'app/'
    .replace(/\/.*$/, ''); // take only the first path segment
  return cleaned;
}

/**
 * Retrieves structured help metadata for a given section and language.
 */
export function getSectionHelp(sectionKey: string, language: Language): SectionHelpData | undefined {
  const s = helpStrings(language);
  const normalized = normalizeSectionKey(sectionKey);

  switch (normalized) {
    case 'dashboard':
      return {
        key: 'dashboard',
        title: s.dashboardTitle,
        help: s.dashboardHelp,
        tip: s.dashboardTip,
      };
    case 'products':
      return {
        key: 'products',
        title: s.productsTitle,
        help: s.productsHelp,
        prerequisites: s.productsPrereq,
        steps: s.productsSteps,
        importantFields: s.productsFields,
        afterSaving: s.productsAfterSave,
        tip: s.productsTip,
      };
    case 'inventory':
      return {
        key: 'inventory',
        title: s.inventoryTitle,
        help: s.inventoryHelp,
        prerequisites: s.inventoryPrereq,
        steps: s.inventorySteps,
        tip: s.inventoryTip,
      };
    case 'suppliers':
      return {
        key: 'suppliers',
        title: s.suppliersTitle,
        help: s.suppliersHelp,
        steps: s.suppliersSteps,
        tip: s.suppliersTip,
      };
    case 'purchases':
      return {
        key: 'purchases',
        title: s.purchasesTitle,
        help: s.purchasesHelp,
        steps: s.purchasesSteps,
        tip: s.purchasesTip,
      };
    case 'orders':
      return {
        key: 'orders',
        title: s.ordersTitle,
        help: s.ordersHelp,
        steps: s.ordersSteps,
        tip: s.ordersTip,
      };
    case 'customers':
      return {
        key: 'customers',
        title: s.customersTitle,
        help: s.customersHelp,
        steps: s.customersSteps,
        tip: s.customersTip,
      };
    case 'expenses':
      return {
        key: 'expenses',
        title: s.expensesTitle,
        help: s.expensesHelp,
        steps: s.expensesSteps,
        tip: s.expensesTip,
      };
    case 'accounting':
      return {
        key: 'accounting',
        title: s.accountingTitle,
        help: s.accountingHelp,
        tip: s.accountingTip,
      };
    case 'import':
    case 'imports':
      return {
        key: 'imports',
        title: s.importTitle,
        help: s.importHelp,
        steps: s.importSteps,
        tip: s.importTip,
      };
    case 'settings':
      return {
        key: 'settings',
        title: s.settingsTitle,
        help: s.settingsHelp,
        tip: s.settingsTip,
      };
    case 'staff':
      return {
        key: 'staff',
        title: s.staffTitle,
        help: s.staffHelp,
        steps: s.staffSteps,
        tip: s.staffTip,
      };
    case 'messages':
      return {
        key: 'messages',
        title: s.messagesTitle,
        help: s.messagesHelp,
        steps: s.messagesSteps,
        tip: s.messagesTip,
      };
    case 'pos':
      return {
        key: 'pos',
        title: s.posTitle,
        help: s.posHelp,
        steps: s.posSteps,
        tip: s.posTip,
      };
    case 'receiving':
      return {
        key: 'receiving',
        title: s.receivingTitle,
        help: s.receivingHelp,
        steps: s.receivingSteps,
        tip: s.receivingTip,
      };
    case 'profile':
      return {
        key: 'profile',
        title: s.profileTitle,
        help: s.profileHelp,
        tip: s.profileTip,
      };
    case 'tenants':
    case 'platform-tenants':
      return {
        key: 'tenants',
        title: s.tenantsTitle,
        help: s.tenantsHelp,
        steps: s.tenantsSteps,
        tip: s.tenantsTip,
      };
    case 'audit':
    case 'platform-audit':
      return {
        key: 'audit',
        title: s.auditTitle,
        help: s.auditHelp,
        tip: s.auditTip,
      };
    default:
      return undefined;
  }
}
