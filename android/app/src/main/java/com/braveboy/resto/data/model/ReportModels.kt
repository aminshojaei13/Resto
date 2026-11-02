package com.braveboy.resto.data.model

enum class ReportTimeFrame(val labelFa: String, val labelEn: String) {
    TODAY("امروز", "Today"),
    LAST_7_DAYS("۷ روز اخیر", "Last 7 Days"),
    THIS_MONTH("ماه جاری", "This Month"),
    LAST_3_MONTHS("۳ ماه اخیر", "Last 3 Months"),
    ALL_TIME("همه زمان‌ها", "All Time")
}

enum class ReportTab(val labelFa: String, val labelEn: String) {
    COMPREHENSIVE("جامع (سود و زیان)", "Profit & Loss"),
    SALES("فروش و سفارشات", "Sales & Orders"),
    INVENTORY("موجودی انبار", "Inventory Assets"),
    EXPENSES("هزینه‌های جاری", "Expenses"),
    PURCHASES("خرید و تامین", "Purchases"),
    CUSTOMERS("مشتریان و CRM", "Customers")
}

data class ComprehensiveReportData(
    val totalRevenue: Double = 0.0,
    val totalCogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalOrdersCount: Int = 0,
    val averageOrderValue: Double = 0.0
)

data class SalesReportData(
    val totalRevenue: Double = 0.0,
    val totalOrdersCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val channelBreakdown: Map<String, Double> = emptyMap(),
    val paymentMethodBreakdown: Map<String, Double> = emptyMap(),
    val orders: List<SalesOrder> = emptyList()
)

data class InventoryReportData(
    val totalProductTypesCount: Int = 0,
    val totalPhysicalStockUnits: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val products: List<Product> = emptyList()
)

data class ExpensesReportData(
    val totalExpenses: Double = 0.0,
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val expenses: List<Expense> = emptyList()
)

data class PurchasesReportData(
    val totalPurchasesValue: Double = 0.0,
    val totalPurchaseOrdersCount: Int = 0,
    val supplierBreakdown: Map<String, Double> = emptyMap(),
    val purchases: List<Purchase> = emptyList()
)

data class CustomersReportData(
    val totalCustomersCount: Int = 0,
    val totalCustomerSpentValue: Double = 0.0,
    val totalLoyaltyPoints: Int = 0,
    val customers: List<Customer> = emptyList()
)
