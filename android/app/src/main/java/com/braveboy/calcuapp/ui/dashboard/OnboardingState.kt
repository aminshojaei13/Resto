package com.braveboy.calcuapp.ui.dashboard

import com.braveboy.calcuapp.ui.MainDestination

data class OnboardingStepItem(
    val stepIndex: Int,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val subtitleEn: String,
    val isCompleted: Boolean,
    val isOptional: Boolean = false,
    val primaryDestination: MainDestination,
    val primaryActionTextFa: String,
    val primaryActionTextEn: String,
    val secondaryDestination: MainDestination? = null,
    val secondaryActionTextFa: String? = null,
    val secondaryActionTextEn: String? = null,
    val openAddProduct: Boolean = false,
    val canSkip: Boolean = false
)

data class OnboardingState(
    val isDismissed: Boolean = false,
    val hasProducts: Boolean = false,
    val hasSuppliers: Boolean = false,
    val isSupplierSkipped: Boolean = false,
    val hasOrders: Boolean = false,
    val hasCompletedOrders: Boolean = false
) {
    val step1Completed: Boolean get() = hasProducts
    val step2Completed: Boolean get() = hasSuppliers || isSupplierSkipped
    val step3Completed: Boolean get() = hasOrders
    val step4Completed: Boolean get() = hasCompletedOrders

    val completedStepsCount: Int
        get() {
            var count = 0
            if (step1Completed) count++
            if (step2Completed) count++
            if (step3Completed) count++
            if (step4Completed) count++
            return count
        }

    val totalSteps: Int get() = 4

    val progress: Float
        get() = completedStepsCount.toFloat() / totalSteps.toFloat()

    val isAllCompleted: Boolean
        get() = completedStepsCount >= totalSteps

    val steps: List<OnboardingStepItem>
        get() = listOf(
            OnboardingStepItem(
                stepIndex = 1,
                titleFa = "۱. تعریف کالا و موجودی انبار",
                titleEn = "1. Add Products & Initial Stock",
                subtitleFa = "ابتدا کالاهای خود را همراه با قیمت فروش و موجودی اولیه ثبت کنید تا آماده فروش شوند.",
                subtitleEn = "Add your products with selling prices and stock levels to get started.",
                isCompleted = step1Completed,
                isOptional = false,
                primaryDestination = MainDestination.INVENTORY,
                primaryActionTextFa = "ثبت کالای جدید",
                primaryActionTextEn = "Add New Product",
                openAddProduct = true
            ),
            OnboardingStepItem(
                stepIndex = 2,
                titleFa = "۲. تعریف تأمین‌کننده و خرید (اختیاری)",
                titleEn = "2. Add Suppliers & Purchases (Optional)",
                subtitleFa = "تأمین‌کنندگان را مشخص کنید تا فاکتورهای خرید و بدهی‌های تجاری دقیق ثبت شوند.",
                subtitleEn = "Register your vendors to track purchase invoices and supplier debts.",
                isCompleted = step2Completed,
                isOptional = true,
                primaryDestination = MainDestination.SUPPLIERS,
                primaryActionTextFa = "ثبت تأمین‌کننده",
                primaryActionTextEn = "Add Supplier",
                canSkip = !hasSuppliers && !isSupplierSkipped
            ),
            OnboardingStepItem(
                stepIndex = 3,
                titleFa = "۳. ثبت اولین فروش و صدور فاکتور",
                titleEn = "3. Record First Sale & Invoice",
                subtitleFa = "سفارش مشتری را می‌توانید مستقیماً از صندوق یا هوشمندانه از متن پیامک و چت اینستاگرام ثبت کنید.",
                subtitleEn = "Create an order via POS terminal or automatically parse customer chat/SMS.",
                isCompleted = step3Completed,
                isOptional = false,
                primaryDestination = MainDestination.POS,
                primaryActionTextFa = "صندوق فروش (POS)",
                primaryActionTextEn = "Open POS",
                secondaryDestination = MainDestination.MESSAGES,
                secondaryActionTextFa = "ثبت از پیام / چت",
                secondaryActionTextEn = "Parse Chat/SMS"
            ),
            OnboardingStepItem(
                stepIndex = 4,
                titleFa = "۴. مراحل آماده‌سازی، ارسال و تکمیل",
                titleEn = "4. Order Fulfillment & Delivery",
                subtitleFa = "سفارشات ثبت‌شده را پیگیری کنید، مرحله آماده‌سازی و ارسال را ثبت و فاکتور را نهایی کنید.",
                subtitleEn = "Track order progress, update fulfillment status to processing and completed.",
                isCompleted = step4Completed,
                isOptional = false,
                primaryDestination = MainDestination.ORDERS,
                primaryActionTextFa = "مشاهده و مدیریت سفارشات",
                primaryActionTextEn = "Manage Orders"
            )
        )
}
