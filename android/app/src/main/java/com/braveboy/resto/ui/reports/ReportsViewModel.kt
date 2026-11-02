package com.braveboy.resto.ui.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.braveboy.resto.data.model.ComprehensiveReportData
import com.braveboy.resto.data.model.Customer
import com.braveboy.resto.data.model.CustomersReportData
import com.braveboy.resto.data.model.Expense
import com.braveboy.resto.data.model.ExpensesReportData
import com.braveboy.resto.data.model.InventoryReportData
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.Purchase
import com.braveboy.resto.data.model.PurchasesReportData
import com.braveboy.resto.data.model.ReportTab
import com.braveboy.resto.data.model.ReportTimeFrame
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.data.model.SalesReportData
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.repository.CustomerRepository
import com.braveboy.resto.data.repository.ExpenseRepository
import com.braveboy.resto.data.repository.ProductRepository
import com.braveboy.resto.data.repository.PurchaseRepository
import com.braveboy.resto.data.repository.SalesOrderRepository
import com.braveboy.resto.data.repository.TenantRepository
import com.braveboy.resto.util.PersianFormatter
import com.braveboy.resto.util.ReportExportManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class ReportsViewModel(
    private val tenantRepository: TenantRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val productRepository: ProductRepository,
    private val expenseRepository: ExpenseRepository,
    private val purchaseRepository: PurchaseRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    val tenantState: StateFlow<TenantState> = tenantRepository.getTenantState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TenantState()
    )

    private val _selectedTab = MutableStateFlow(ReportTab.COMPREHENSIVE)
    val selectedTab: StateFlow<ReportTab> = _selectedTab.asStateFlow()

    private val _selectedTimeFrame = MutableStateFlow(ReportTimeFrame.THIS_MONTH)
    val selectedTimeFrame: StateFlow<ReportTimeFrame> = _selectedTimeFrame.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val salesOrders: StateFlow<List<SalesOrder>> = combine(tenantState, _selectedTimeFrame) { state, timeFrame ->
        Pair(state.activeOrgId, timeFrame)
    }.flatMapLatest { (orgId, timeFrame) ->
        salesOrderRepository.getAllOrdersByOrg(orgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>> = tenantState.flatMapLatest { state ->
        productRepository.getProducts(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: StateFlow<List<Expense>> = tenantState.flatMapLatest { state ->
        expenseRepository.getExpenses(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val purchases: StateFlow<List<Purchase>> = tenantState.flatMapLatest { state ->
        purchaseRepository.getPurchases(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val customers: StateFlow<List<Customer>> = tenantState.flatMapLatest { state ->
        customerRepository.getCustomers(state.activeOrgId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tab: ReportTab) {
        _selectedTab.value = tab
    }

    fun selectTimeFrame(timeFrame: ReportTimeFrame) {
        _selectedTimeFrame.value = timeFrame
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    private fun isTimestampInTimeFrame(timestamp: Long, timeFrame: ReportTimeFrame): Boolean {
        if (timestamp <= 0) return true
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        return when (timeFrame) {
            ReportTimeFrame.TODAY -> {
                val startOfToday = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                timestamp >= startOfToday
            }
            ReportTimeFrame.LAST_7_DAYS -> timestamp >= (now - 7L * 24 * 60 * 60 * 1000)
            ReportTimeFrame.THIS_MONTH -> {
                val startOfMonth = cal.apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                timestamp >= startOfMonth
            }
            ReportTimeFrame.LAST_3_MONTHS -> timestamp >= (now - 90L * 24 * 60 * 60 * 1000)
            ReportTimeFrame.ALL_TIME -> true
        }
    }

    // Filtered Report Data
    val filteredOrders: StateFlow<List<SalesOrder>> = combine(salesOrders, _selectedTimeFrame) { list, frame ->
        list.filter { isTimestampInTimeFrame(it.createdAt, frame) }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val filteredExpenses: StateFlow<List<Expense>> = combine(expenses, _selectedTimeFrame) { list, frame ->
        list.filter { isTimestampInTimeFrame(it.createdAt, frame) }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val filteredPurchases: StateFlow<List<Purchase>> = combine(purchases, _selectedTimeFrame) { list, frame ->
        list.filter { isTimestampInTimeFrame(it.createdAt, frame) }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val comprehensiveData: StateFlow<ComprehensiveReportData> = combine(
        filteredOrders,
        filteredExpenses,
        products
    ) { ordersList, expensesList, productList ->
        val totalRevenue = ordersList.sumOf { it.totalAmount }
        var totalCogs = 0.0

        for (order in ordersList) {
            for (item in order.items) {
                val matchedProd = productList.find { it.id == item.productId }
                val costPrice = matchedProd?.costPrice ?: (item.unitPrice * 0.7)
                totalCogs += (costPrice * item.quantity)
            }
        }

        val grossProfit = (totalRevenue - totalCogs).coerceAtLeast(0.0)
        val totalExpensesAmount = expensesList.sumOf { it.amount }
        val netProfit = totalRevenue - totalCogs - totalExpensesAmount
        val ordersCount = ordersList.size
        val avgOrderValue = if (ordersCount > 0) totalRevenue / ordersCount else 0.0

        ComprehensiveReportData(
            totalRevenue = totalRevenue,
            totalCogs = totalCogs,
            grossProfit = grossProfit,
            totalExpenses = totalExpensesAmount,
            netProfit = netProfit,
            totalOrdersCount = ordersCount,
            averageOrderValue = avgOrderValue
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = ComprehensiveReportData())

    val salesData: StateFlow<SalesReportData> = combine(filteredOrders, _selectedTimeFrame) { ordersList, _ ->
        val totalRevenue = ordersList.sumOf { it.totalAmount }
        val count = ordersList.size
        val avg = if (count > 0) totalRevenue / count else 0.0

        val channelMap = mutableMapOf<String, Double>()
        val paymentMap = mutableMapOf<String, Double>()

        for (order in ordersList) {
            val chTitle = SalesChannel.fromCode(order.source).titleFa
            channelMap[chTitle] = (channelMap[chTitle] ?: 0.0) + order.totalAmount

            val payTitle = order.paymentMethod.displayName
            paymentMap[payTitle] = (paymentMap[payTitle] ?: 0.0) + order.totalAmount
        }

        SalesReportData(
            totalRevenue = totalRevenue,
            totalOrdersCount = count,
            averageOrderValue = avg,
            channelBreakdown = channelMap,
            paymentMethodBreakdown = paymentMap,
            orders = ordersList
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = SalesReportData())

    val inventoryData: StateFlow<InventoryReportData> = combine(products, tenantState) { productList, state ->
        val productTypes = productList.size
        var physicalUnits = 0
        var totalValue = 0.0
        var lowStock = 0

        for (p in productList) {
            val stock = if (state.activeWarehouseId.isNotBlank()) p.getStockForWarehouse(state.activeWarehouseId) else p.getTotalStock()
            physicalUnits += stock
            totalValue += (stock * p.costPrice)
            if (stock in 0..5) lowStock++
        }

        InventoryReportData(
            totalProductTypesCount = productTypes,
            totalPhysicalStockUnits = physicalUnits,
            totalInventoryValue = totalValue,
            lowStockCount = lowStock,
            products = productList
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = InventoryReportData())

    val expensesData: StateFlow<ExpensesReportData> = combine(filteredExpenses, _selectedTimeFrame) { list, _ ->
        val total = list.sumOf { it.amount }
        val catMap = mutableMapOf<String, Double>()
        for (e in list) {
            catMap[e.category] = (catMap[e.category] ?: 0.0) + e.amount
        }
        ExpensesReportData(
            totalExpenses = total,
            categoryBreakdown = catMap,
            expenses = list
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = ExpensesReportData())

    val purchasesData: StateFlow<PurchasesReportData> = combine(filteredPurchases, _selectedTimeFrame) { list, _ ->
        val total = list.sumOf { it.totalAmount }
        val suppMap = mutableMapOf<String, Double>()
        for (p in list) {
            suppMap[p.supplierName] = (suppMap[p.supplierName] ?: 0.0) + p.totalAmount
        }
        PurchasesReportData(
            totalPurchasesValue = total,
            totalPurchaseOrdersCount = list.size,
            supplierBreakdown = suppMap,
            purchases = list
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = PurchasesReportData())

    val customersData: StateFlow<CustomersReportData> = combine(customers, _selectedTimeFrame) { list, _ ->
        val totalSpent = list.sumOf { it.totalPurchases }
        val totalPoints = list.sumOf { it.loyaltyPoints }
        CustomersReportData(
            totalCustomersCount = list.size,
            totalCustomerSpentValue = totalSpent,
            totalLoyaltyPoints = totalPoints,
            customers = list
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = CustomersReportData())

    // EXPORT HANDLERS
    fun exportReportToPdf(context: Context, onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val tab = _selectedTab.value
            val tf = _selectedTimeFrame.value
            val timeFrameStr = tf.labelFa

            val file = when (tab) {
                ReportTab.COMPREHENSIVE -> {
                    val comp = comprehensiveData.value
                    ReportExportManager.exportToPdf(
                        context = context,
                        fileName = "Resto_Profit_Loss_Report_${System.currentTimeMillis()}",
                        title = "گزارش جامع سود و زیان (P&L)",
                        subtitle = "بازه زمانی: $timeFrameStr | تاریخ صدور: ${PersianFormatter.toPersianDigits(System.currentTimeMillis().toString().takeLast(6))}",
                        metricsSummary = listOf(
                            "فروش کل" to "${PersianFormatter.formatTomans(comp.totalRevenue)} تومان",
                            "سود ناخالص" to "${PersianFormatter.formatTomans(comp.grossProfit)} تومان",
                            "کل هزینه‌ها" to "${PersianFormatter.formatTomans(comp.totalExpenses)} تومان",
                            "سود خالص" to "${PersianFormatter.formatTomans(comp.netProfit)} تومان"
                        ),
                        tableHeaders = listOf("عنوان شاخص مالی", "مبلغ (تومان)"),
                        tableRows = listOf(
                            listOf("درآمد کل فروش (Revenue)", PersianFormatter.formatTomans(comp.totalRevenue)),
                            listOf("قیمت تمام‌شده کالاهای فروخته‌شده (COGS)", PersianFormatter.formatTomans(comp.totalCogs)),
                            listOf("سود ناخالص (Gross Profit)", PersianFormatter.formatTomans(comp.grossProfit)),
                            listOf("هزینه‌های جاری و عملیاتی (Expenses)", PersianFormatter.formatTomans(comp.totalExpenses)),
                            listOf("سود خالص عملیاتی (Net Profit)", PersianFormatter.formatTomans(comp.netProfit)),
                            listOf("تعداد کل سفارش‌های ثبت‌شده", PersianFormatter.toPersianDigits(comp.totalOrdersCount)),
                            listOf("میانگین درآمد هر سفارش", PersianFormatter.formatTomans(comp.averageOrderValue))
                        )
                    )
                }
                ReportTab.SALES -> {
                    val sales = salesData.value
                    ReportExportManager.exportToPdf(
                        context = context,
                        fileName = "Resto_Sales_Report_${System.currentTimeMillis()}",
                        title = "گزارش فروش و سفارشات",
                        subtitle = "بازه زمانی: $timeFrameStr | تعداد کل سفارش‌ها: ${PersianFormatter.toPersianDigits(sales.totalOrdersCount)}",
                        metricsSummary = listOf(
                            "مجموع فروش" to "${PersianFormatter.formatTomans(sales.totalRevenue)} تومان",
                            "تعداد سفارش" to PersianFormatter.toPersianDigits(sales.totalOrdersCount),
                            "میانگین هر فاکتور" to "${PersianFormatter.formatTomans(sales.averageOrderValue)} تومان"
                        ),
                        tableHeaders = listOf("شماره سفارش", "نام مشتری", "کانال فروش", "پرداخت", "مبلغ کل"),
                        tableRows = sales.orders.map { o ->
                            listOf(
                                o.orderNumber,
                                o.customerName,
                                SalesChannel.fromCode(o.source).titleFa,
                                o.paymentMethod.displayName,
                                PersianFormatter.formatTomans(o.totalAmount)
                            )
                        }
                    )
                }
                ReportTab.INVENTORY -> {
                    val inv = inventoryData.value
                    ReportExportManager.exportToPdf(
                        context = context,
                        fileName = "Resto_Inventory_Assets_Report_${System.currentTimeMillis()}",
                        title = "گزارش موجودی و دارایی انبار",
                        subtitle = "بازه زمانی: $timeFrameStr | تعداد کل عناوین کالا: ${PersianFormatter.toPersianDigits(inv.totalProductTypesCount)}",
                        metricsSummary = listOf(
                            "ارزش کل دارایی انبار" to "${PersianFormatter.formatTomans(inv.totalInventoryValue)} تومان",
                            "مجموع فیزیکی کالا" to "${PersianFormatter.toPersianDigits(inv.totalPhysicalStockUnits)} عدد",
                            "کالاهای رو به اتمام" to PersianFormatter.toPersianDigits(inv.lowStockCount)
                        ),
                        tableHeaders = listOf("کد SKU", "نام محصول", "دسته‌بندی", "موجودی کل", "قیمت خرید", "ارزش موجودی"),
                        tableRows = inv.products.map { p ->
                            val totalStock = p.getTotalStock()
                            listOf(
                                p.sku,
                                p.name,
                                p.category,
                                PersianFormatter.toPersianDigits(totalStock),
                                PersianFormatter.formatTomans(p.costPrice),
                                PersianFormatter.formatTomans(totalStock * p.costPrice)
                            )
                        }
                    )
                }
                ReportTab.EXPENSES -> {
                    val exp = expensesData.value
                    ReportExportManager.exportToPdf(
                        context = context,
                        fileName = "Resto_Expenses_Report_${System.currentTimeMillis()}",
                        title = "گزارش هزینه‌های جاری و عملیاتی",
                        subtitle = "بازه زمانی: $timeFrameStr | کل هزینه‌ها: ${PersianFormatter.formatTomans(exp.totalExpenses)} تومان",
                        metricsSummary = listOf(
                            "مجموع هزینه‌ها" to "${PersianFormatter.formatTomans(exp.totalExpenses)} تومان",
                            "تعداد ثبت هزینه" to PersianFormatter.toPersianDigits(exp.expenses.size)
                        ),
                        tableHeaders = listOf("دسته‌بندی", "توضیحات", "روش پرداخت", "مبلغ (تومان)"),
                        tableRows = exp.expenses.map { e ->
                            listOf(
                                e.category,
                                e.notes.ifBlank { "-" },
                                e.paymentMethod,
                                PersianFormatter.formatTomans(e.amount)
                            )
                        }
                    )
                }
                ReportTab.PURCHASES -> {
                    val pur = purchasesData.value
                    ReportExportManager.exportToPdf(
                        context = context,
                        fileName = "Resto_Purchases_Report_${System.currentTimeMillis()}",
                        title = "گزارش خریدهای انبار و تامین‌کنندگان",
                        subtitle = "بازه زمانی: $timeFrameStr | کل فاکتورها: ${PersianFormatter.toPersianDigits(pur.totalPurchaseOrdersCount)}",
                        metricsSummary = listOf(
                            "مجموع خریدهای انبار" to "${PersianFormatter.formatTomans(pur.totalPurchasesValue)} تومان",
                            "تعداد سفارش خرید" to PersianFormatter.toPersianDigits(pur.totalPurchaseOrdersCount)
                        ),
                        tableHeaders = listOf("شماره خرید", "نام تامین‌کننده", "وضعیت سفارش", "پرداخت", "مبلغ کل"),
                        tableRows = pur.purchases.map { p ->
                            listOf(
                                p.purchaseNumber,
                                p.supplierName,
                                p.status,
                                p.paymentStatus,
                                PersianFormatter.formatTomans(p.totalAmount)
                            )
                        }
                    )
                }
                ReportTab.CUSTOMERS -> {
                    val cust = customersData.value
                    ReportExportManager.exportToPdf(
                        context = context,
                        fileName = "Resto_Customers_CRM_Report_${System.currentTimeMillis()}",
                        title = "گزارش مشتریان و باشگاه مشتریان",
                        subtitle = "تعداد کل مشتریان: ${PersianFormatter.toPersianDigits(cust.totalCustomersCount)}",
                        metricsSummary = listOf(
                            "حجم خرید مشتریان" to "${PersianFormatter.formatTomans(cust.totalCustomerSpentValue)} تومان",
                            "تعداد کل مشتریان" to PersianFormatter.toPersianDigits(cust.totalCustomersCount),
                            "مجموع امتیازات" to PersianFormatter.toPersianDigits(cust.totalLoyaltyPoints)
                        ),
                        tableHeaders = listOf("نام مشتری", "شماره تماس", "مجموع خریدهای مشتری", "امتیاز باشگاه"),
                        tableRows = cust.customers.map { c ->
                            listOf(
                                c.name,
                                c.phone.ifBlank { "-" },
                                PersianFormatter.formatTomans(c.totalPurchases),
                                PersianFormatter.toPersianDigits(c.loyaltyPoints)
                            )
                        }
                    )
                }
            }

            if (file != null) {
                _snackbarMessage.value = "فایل PDF با موفقیت ذخیره شد: ${file.name}"
                onFileReady(file)
            } else {
                _snackbarMessage.value = "خطا در ایجاد فایل PDF"
            }
        }
    }

    fun exportReportToCsv(context: Context, onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val tab = _selectedTab.value
            val tf = _selectedTimeFrame.value

            val (fileName, csvContent) = when (tab) {
                ReportTab.COMPREHENSIVE -> {
                    val comp = comprehensiveData.value
                    val sb = StringBuilder()
                    sb.append("عنوان شاخص,مبلغ به تومان\n")
                    sb.append("درآمد کل فروش,${comp.totalRevenue}\n")
                    sb.append("قیمت تمام‌شده کالاهای فروخته‌شده (COGS),${comp.totalCogs}\n")
                    sb.append("سود ناخالص,${comp.grossProfit}\n")
                    sb.append("هزینه‌های جاری,${comp.totalExpenses}\n")
                    sb.append("سود خالص عملیاتی,${comp.netProfit}\n")
                    sb.append("تعداد سفارشات,${comp.totalOrdersCount}\n")
                    sb.append("میانگین هر سفارش,${comp.averageOrderValue}\n")
                    Pair("Resto_P_and_L_${tf.name}", sb.toString())
                }
                ReportTab.SALES -> {
                    val sales = salesData.value
                    val sb = StringBuilder()
                    sb.append("شماره سفارش,نام مشتری,کانال فروش,روش پرداخت,مبلغ کل تومان\n")
                    sales.orders.forEach { o ->
                        sb.append("${o.orderNumber},${o.customerName},${SalesChannel.fromCode(o.source).titleFa},${o.paymentMethod.displayName},${o.totalAmount}\n")
                    }
                    Pair("Resto_Sales_Report_${tf.name}", sb.toString())
                }
                ReportTab.INVENTORY -> {
                    val inv = inventoryData.value
                    val sb = StringBuilder()
                    sb.append("کد SKU,نام کالا,دسته‌بندی,موجودی کل,قیمت خرید تومان,ارزش کل تومان\n")
                    inv.products.forEach { p ->
                        val stock = p.getTotalStock()
                        sb.append("${p.sku},${p.name},${p.category},$stock,${p.costPrice},${stock * p.costPrice}\n")
                    }
                    Pair("Resto_Inventory_Assets_${tf.name}", sb.toString())
                }
                ReportTab.EXPENSES -> {
                    val exp = expensesData.value
                    val sb = StringBuilder()
                    sb.append("دسته‌بندی,توضیحات,روش پرداخت,مبلغ تومان\n")
                    exp.expenses.forEach { e ->
                        sb.append("${e.category},${e.notes},${e.paymentMethod},${e.amount}\n")
                    }
                    Pair("Resto_Expenses_Report_${tf.name}", sb.toString())
                }
                ReportTab.PURCHASES -> {
                    val pur = purchasesData.value
                    val sb = StringBuilder()
                    sb.append("شماره فاکتور خرید,نام تامین‌کننده,وضعیت سفارش,وضعیت پرداخت,مبلغ کل تومان\n")
                    pur.purchases.forEach { p ->
                        sb.append("${p.purchaseNumber},${p.supplierName},${p.status},${p.paymentStatus},${p.totalAmount}\n")
                    }
                    Pair("Resto_Purchases_Report_${tf.name}", sb.toString())
                }
                ReportTab.CUSTOMERS -> {
                    val cust = customersData.value
                    val sb = StringBuilder()
                    sb.append("نام مشتری,شماره تماس,آدرس,مجموع خریدها تومان,امتیاز باشگاه\n")
                    cust.customers.forEach { c ->
                        sb.append("${c.name},${c.phone},${c.address},${c.totalPurchases},${c.loyaltyPoints}\n")
                    }
                    Pair("Resto_Customers_CRM_${tf.name}", sb.toString())
                }
            }

            val file = ReportExportManager.exportToCsv(context, fileName, csvContent)
            if (file != null) {
                _snackbarMessage.value = "فایل CSV/اکسل با موفقیت ذخیره شد: ${file.name}"
                onFileReady(file)
            } else {
                _snackbarMessage.value = "خطا در ایجاد فایل CSV"
            }
        }
    }

    class Factory(
        private val tenantRepository: TenantRepository,
        private val salesOrderRepository: SalesOrderRepository,
        private val productRepository: ProductRepository,
        private val expenseRepository: ExpenseRepository,
        private val purchaseRepository: PurchaseRepository,
        private val customerRepository: CustomerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportsViewModel(
                tenantRepository,
                salesOrderRepository,
                productRepository,
                expenseRepository,
                purchaseRepository,
                customerRepository
            ) as T
        }
    }
}
