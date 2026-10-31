package com.braveboy.resto.ui.guide

object ScreenGuideRegistry {

    private val registry = mapOf(
        "dashboard" to ScreenGuideConfig(
            screenId = "dashboard",
            screenTitleFa = "داشبورد مدیریت",
            screenTitleEn = "Dashboard",
            steps = listOf(
                GuideStep(
                    targetKey = "dashboard_header",
                    titleFa = "هدر و خوش‌آمدگویی",
                    titleEn = "Dashboard Header",
                    descriptionFa = "نمایش نام کاربر، اطلاعیه‌ها و دسترسی به مشخصات فروشگاه.",
                    descriptionEn = "View profile greetings, notifications, and store details."
                ),
                GuideStep(
                    targetKey = "dashboard_stats",
                    titleFa = "آمار سود و سفارشات",
                    titleEn = "KPI Metrics",
                    descriptionFa = "مشاهده سود خالص عملیاتی و کل سفارشات به‌صورت زنده.",
                    descriptionEn = "Real-time operating profit and total orders summary."
                ),
                GuideStep(
                    targetKey = "dashboard_revenue",
                    titleFa = "درآمد کل",
                    titleEn = "Total Revenue",
                    descriptionFa = "کارت برجسته نمایش مجموع فروش و روند رشد نسبت به ماه قبل.",
                    descriptionEn = "Total revenue card with growth indicators."
                ),
                GuideStep(
                    targetKey = "dashboard_actions",
                    titleFa = "میانبرهای سریع",
                    titleEn = "Quick Actions",
                    descriptionFa = "دسترسی مستقیم به ثبت کالا، سفارشات و برداشت وجه.",
                    descriptionEn = "Direct shortcuts to add products, view orders, and withdraw funds."
                ),
                GuideStep(
                    targetKey = "dashboard_activity",
                    titleFa = "گردش مالی و تراکنش‌ها",
                    titleEn = "Financial Activity",
                    descriptionFa = "مشاهده آخرین تراکنش‌ها و ثبت سند مالی جدید (درآمد/هزینه).",
                    descriptionEn = "View latest ledger activity and record new financial entries."
                )
            )
        ),
        "orders" to ScreenGuideConfig(
            screenId = "orders",
            screenTitleFa = "مدیریت سفارش‌ها",
            screenTitleEn = "Sales Orders",
            steps = listOf(
                GuideStep(
                    targetKey = "orders_search",
                    titleFa = "جستجوی سفارشات",
                    titleEn = "Order Search",
                    descriptionFa = "جستجوی هوشمند فاکتورها بر اساس شماره سفارش یا نام مشتری.",
                    descriptionEn = "Search orders by invoice number or customer name."
                ),
                GuideStep(
                    targetKey = "orders_filters",
                    titleFa = "فیلترهای وضعیت",
                    titleEn = "Status Filters",
                    descriptionFa = "دسته‌بندی سفارشات بر اساس وضعیت پرداخت و تحویل.",
                    descriptionEn = "Filter orders by payment and fulfillment status."
                )
            )
        ),
        "inventory" to ScreenGuideConfig(
            screenId = "inventory",
            screenTitleFa = "موجودی انبار",
            screenTitleEn = "Inventory Catalog",
            steps = listOf(
                GuideStep(
                    targetKey = "inventory_stats",
                    titleFa = "شاخص‌های انبار",
                    titleEn = "Stock Metrics",
                    descriptionFa = "مشاهده کل کالاها، موجودی کل و هشدار کسری انبار.",
                    descriptionEn = "Overview of total SKUs, units in stock, and low stock alerts."
                ),
                GuideStep(
                    targetKey = "inventory_search",
                    titleFa = "جستجوی کالا",
                    titleEn = "Product Search",
                    descriptionFa = "یافتن کالا بر اساس نام، بارکد یا کد شناسه SKU.",
                    descriptionEn = "Quickly search products by name, barcode, or SKU."
                ),
                GuideStep(
                    targetKey = "inventory_fab",
                    titleFa = "افزودن کالای جدید",
                    titleEn = "Add New Product",
                    descriptionFa = "ثبت کالای جدید همراه با قیمت فروش، قیمت خرید و موجودی اولیه.",
                    descriptionEn = "Register new products with prices and initial stock levels."
                )
            )
        ),
        "pos" to ScreenGuideConfig(
            screenId = "pos",
            screenTitleFa = "صندوق فروش (POS)",
            screenTitleEn = "Mobile POS",
            steps = listOf(
                GuideStep(
                    targetKey = "pos_catalog",
                    titleFa = "کاتالوگ فروش",
                    titleEn = "Product Catalog",
                    descriptionFa = "انتخاب سریع کالاها جهت افزودن به سبد خرید مشتری.",
                    descriptionEn = "Tap products to quickly add them to customer cart."
                ),
                GuideStep(
                    targetKey = "pos_barcode",
                    titleFa = "اسکنر بارکد",
                    titleEn = "Barcode Scanner",
                    descriptionFa = "اسکن سریع بارکد کالا با دوربین گوشی.",
                    descriptionEn = "Scan product barcodes using phone camera."
                )
            )
        ),
        "messages" to ScreenGuideConfig(
            screenId = "messages",
            screenTitleFa = "سفارش پیامکی",
            screenTitleEn = "Social Orders",
            steps = listOf(
                GuideStep(
                    targetKey = "messages_input",
                    titleFa = "استخراج هوشمند پیام",
                    titleEn = "AI Message Parsing",
                    descriptionFa = "جای‌گذاری چت اینستاگرام یا واتساپ جهت استخراج خودکار فاکتور.",
                    descriptionEn = "Paste customer chats to automatically parse and generate sales orders."
                )
            )
        ),
        "customers" to ScreenGuideConfig(
            screenId = "customers",
            screenTitleFa = "مدیریت مشتریان",
            screenTitleEn = "Customer CRM",
            steps = listOf(
                GuideStep(
                    targetKey = "customers_search",
                    titleFa = "جستجوی مشتریان",
                    titleEn = "Customer Search",
                    descriptionFa = "یافتن پروفایل مشتری با نام یا شماره تلفن.",
                    descriptionEn = "Find customer profiles by name or phone number."
                ),
                GuideStep(
                    targetKey = "customers_fab",
                    titleFa = "ثبت مشتری جدید",
                    titleEn = "Add Customer",
                    descriptionFa = "افزودن مشتری جدید و ثبت تاریخچه خرید و بدهی.",
                    descriptionEn = "Register new customer profiles and track purchase history."
                )
            )
        ),
        "settings" to ScreenGuideConfig(
            screenId = "settings",
            screenTitleFa = "تنظیمات برنامه",
            screenTitleEn = "Settings",
            steps = listOf(
                GuideStep(
                    targetKey = "settings_profile",
                    titleFa = "حساب و پشتیبان‌گیری",
                    titleEn = "Account & Cloud Sync",
                    descriptionFa = "مشاهده وضعیت اکانت محلی و اتصال به حساب ابری.",
                    descriptionEn = "View local account status and connect to cloud sync."
                )
            )
        )
    )

    fun getGuideForScreen(screenId: String): ScreenGuideConfig? {
        return registry[screenId]
    }

    fun hasGuideForScreen(screenId: String): Boolean {
        return registry.containsKey(screenId)
    }
}
