package com.braveboy.resto.util

import android.content.Context
import android.net.Uri
import com.braveboy.resto.data.local.datastore.TenantPreferences
import com.braveboy.resto.data.local.db.AppDatabase
import com.braveboy.resto.data.local.db.entity.CustomerEntity
import com.braveboy.resto.data.local.db.entity.ExpenseEntity
import com.braveboy.resto.data.local.db.entity.LedgerEntryEntity
import com.braveboy.resto.data.local.db.entity.OrganizationEntity
import com.braveboy.resto.data.local.db.entity.ProductEntity
import com.braveboy.resto.data.local.db.entity.PurchaseEntity
import com.braveboy.resto.data.local.db.entity.SalesOrderEntity
import com.braveboy.resto.data.local.db.entity.StoreEntity
import com.braveboy.resto.data.local.db.entity.SupplierEntity
import com.braveboy.resto.data.local.db.entity.WarehouseEntity
import com.braveboy.resto.data.model.FulfillmentStatus
import com.braveboy.resto.data.model.LedgerCategory
import com.braveboy.resto.data.model.LedgerType
import com.braveboy.resto.data.model.OrderItem
import com.braveboy.resto.data.model.PaymentMethod
import com.braveboy.resto.data.model.PaymentStatus
import com.braveboy.resto.data.model.PurchaseItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class BackupRestoreSummary(
    val productsCount: Int = 0,
    val salesOrdersCount: Int = 0,
    val customersCount: Int = 0,
    val suppliersCount: Int = 0,
    val purchasesCount: Int = 0,
    val expensesCount: Int = 0,
    val ledgerEntriesCount: Int = 0
)

object LocalBackupManager {

    private const val HEADER_MAGIC = "RESTO_BACKUP_V1\n"

    suspend fun writeBackupToUri(
        context: Context,
        uri: Uri,
        database: AppDatabase,
        tenantPreferences: TenantPreferences?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootObj = JSONObject().apply {
                put("app", "Resto Business OS")
                put("backupVersion", "1.3")
                put("createdTimestamp", System.currentTimeMillis())

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                put("createdDate", dateFormat.format(Date()))

                val tenantState = tenantPreferences?.tenantState?.firstOrNull()
                put("tenantInfo", JSONObject().apply {
                    put("orgId", tenantState?.activeOrgId.orEmpty())
                    put("storeId", tenantState?.activeStoreId.orEmpty())
                    put("warehouseId", tenantState?.activeWarehouseId.orEmpty())
                    put("userName", tenantState?.userName.orEmpty())
                })

                // Organizations
                val orgs = database.tenantDao().getAllOrganizationsDirect()
                put("organizations", JSONArray().apply {
                    orgs.forEach { o ->
                        put(JSONObject().apply {
                            put("id", o.id)
                            put("name", o.name)
                            put("code", o.code)
                            put("logoUrl", o.logoUrl)
                            put("currencySymbol", o.currencySymbol)
                            put("currencyCode", o.currencyCode)
                            put("subscriptionTier", o.subscriptionTier)
                        })
                    }
                })

                // Stores
                val stores = database.tenantDao().getAllStoresDirect()
                put("stores", JSONArray().apply {
                    stores.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("orgId", s.orgId)
                            put("name", s.name)
                            put("code", s.code)
                            put("address", s.address)
                            put("phone", s.phone)
                        })
                    }
                })

                // Warehouses
                val warehouses = database.tenantDao().getAllWarehousesDirect()
                put("warehouses", JSONArray().apply {
                    warehouses.forEach { w ->
                        put(JSONObject().apply {
                            put("id", w.id)
                            put("storeId", w.storeId)
                            put("orgId", w.orgId)
                            put("name", w.name)
                            put("code", w.code)
                            put("address", w.address)
                        })
                    }
                })

                // Products
                val products = database.productDao().getAllProductsDirect()
                put("products", JSONArray().apply {
                    products.forEach { p ->
                        put(JSONObject().apply {
                            put("id", p.id)
                            put("orgId", p.orgId)
                            put("sku", p.sku)
                            put("barcode", p.barcode)
                            put("name", p.name)
                            put("description", p.description)
                            put("price", p.price)
                            put("costPrice", p.costPrice)
                            put("category", p.category)
                            put("unit", p.unit)
                            put("imageUrl", p.imageUrl)

                            put("stockMap", JSONObject().apply {
                                p.stockQuantityByWarehouse.forEach { (k, v) -> put(k, v) }
                            })
                            put("isSynced", p.isSynced)
                        })
                    }
                })

                // Sales Orders
                val orders = database.salesOrderDao().getAllSalesOrdersDirect()
                put("salesOrders", JSONArray().apply {
                    orders.forEach { o ->
                        put(JSONObject().apply {
                            put("id", o.id)
                            put("orderNumber", o.orderNumber)
                            put("orgId", o.orgId)
                            put("storeId", o.storeId)
                            put("warehouseId", o.warehouseId)
                            put("customerId", o.customerId.orEmpty())
                            put("customerName", o.customerName)
                            put("subtotal", o.subtotal)
                            put("discountAmount", o.discountAmount)
                            put("taxAmount", o.taxAmount)
                            put("totalAmount", o.totalAmount)
                            put("paymentMethod", o.paymentMethod.name)
                            put("paymentStatus", o.paymentStatus.name)
                            put("fulfillmentStatus", o.fulfillmentStatus.name)
                            put("notes", o.notes)
                            put("createdAt", o.createdAt)
                            put("isSynced", o.isSynced)

                            put("items", JSONArray().apply {
                                o.items.forEach { item ->
                                    put(JSONObject().apply {
                                        put("productId", item.productId)
                                        put("variantId", item.variantId.orEmpty())
                                        put("productName", item.productName)
                                        put("sku", item.sku)
                                        put("unitPrice", item.unitPrice)
                                        put("quantity", item.quantity)
                                        put("discountPercent", item.discountPercent)
                                        put("taxAmount", item.taxAmount)
                                        put("totalPrice", item.totalPrice)
                                    })
                                }
                            })
                        })
                    }
                })

                // Customers
                val customers = database.customerDao().getAllCustomersDirect()
                put("customers", JSONArray().apply {
                    customers.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("orgId", c.orgId)
                            put("name", c.name)
                            put("phone", c.phone)
                            put("email", c.email)
                            put("address", c.address)
                            put("totalPurchases", c.totalPurchases)
                            put("loyaltyPoints", c.loyaltyPoints)
                            put("createdAt", c.createdAt)
                            put("isSynced", c.isSynced)
                        })
                    }
                })

                // Suppliers
                val suppliers = database.supplierDao().getAllSuppliersDirect()
                put("suppliers", JSONArray().apply {
                    suppliers.forEach { s ->
                        put(JSONObject().apply {
                            put("id", s.id)
                            put("orgId", s.orgId)
                            put("name", s.name)
                            put("phone", s.phone)
                            put("email", s.email)
                            put("address", s.address)
                            put("createdAt", s.createdAt)
                            put("isSynced", s.isSynced)
                        })
                    }
                })

                // Purchases
                val purchases = database.purchaseDao().getAllPurchasesDirect()
                put("purchases", JSONArray().apply {
                    purchases.forEach { p ->
                        put(JSONObject().apply {
                            put("id", p.id)
                            put("purchaseNumber", p.purchaseNumber)
                            put("orgId", p.orgId)
                            put("storeId", p.storeId)
                            put("warehouseId", p.warehouseId)
                            put("supplierId", p.supplierId)
                            put("supplierName", p.supplierName)
                            put("totalAmount", p.totalAmount)
                            put("status", p.status)
                            put("paymentStatus", p.paymentStatus)
                            put("createdAt", p.createdAt)
                            put("isSynced", p.isSynced)

                            put("items", JSONArray().apply {
                                p.items.forEach { pi ->
                                    put(JSONObject().apply {
                                        put("id", pi.id)
                                        put("purchaseId", pi.purchaseId)
                                        put("productId", pi.productId)
                                        put("productName", pi.productName)
                                        put("quantity", pi.quantity)
                                        put("unitCost", pi.unitCost)
                                        put("totalCost", pi.totalCost)
                                    })
                                }
                            })
                        })
                    }
                })

                // Expenses
                val expenses = database.expenseDao().getAllExpensesDirect()
                put("expenses", JSONArray().apply {
                    expenses.forEach { e ->
                        put(JSONObject().apply {
                            put("id", e.id)
                            put("orgId", e.orgId)
                            put("storeId", e.storeId)
                            put("category", e.category)
                            put("amount", e.amount)
                            put("paymentMethod", e.paymentMethod)
                            put("date", e.date)
                            put("notes", e.notes)
                            put("userId", e.userId.orEmpty())
                            put("createdAt", e.createdAt)
                            put("isSynced", e.isSynced)
                        })
                    }
                })

                // Ledger Entries
                val ledgerEntries = database.ledgerDao().getAllLedgerEntriesDirect()
                put("ledgerEntries", JSONArray().apply {
                    ledgerEntries.forEach { l ->
                        put(JSONObject().apply {
                            put("id", l.id)
                            put("orgId", l.orgId)
                            put("storeId", l.storeId)
                            put("entryNumber", l.entryNumber)
                            put("description", l.description)
                            put("type", l.type.name)
                            put("amount", l.amount)
                            put("category", l.category.name)
                            put("referenceId", l.referenceId.orEmpty())
                            put("createdAt", l.createdAt)
                        })
                    }
                })
            }

            val rawJsonBytes = rootObj.toString().toByteArray(Charsets.UTF_8)
            val compressedBytesOut = ByteArrayOutputStream()
            GZIPOutputStream(compressedBytesOut).use { gzip ->
                gzip.write(rawJsonBytes)
                gzip.flush()
            }

            val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri, "w")
            if (outputStream == null) return@withContext false

            outputStream.use { out ->
                out.write(HEADER_MAGIC.toByteArray(Charsets.UTF_8))
                out.write(compressedBytesOut.toByteArray())
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackupFromUri(
        context: Context,
        uri: Uri,
        database: AppDatabase
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return@withContext Result.failure(Exception("امکان خواندن فایل انتخاب شده وجود ندارد."))
            }

            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return@withContext Result.failure(Exception("فایل انتخاب شده خالی است."))
            }

            val headerMagicBytes = HEADER_MAGIC.toByteArray(Charsets.UTF_8)
            val jsonText: String = if (bytes.size > headerMagicBytes.size &&
                bytes.sliceArray(0 until headerMagicBytes.size).contentEquals(headerMagicBytes)
            ) {
                // Decompress Resto compressed backup
                val compressedPayload = bytes.copyOfRange(headerMagicBytes.size, bytes.size)
                GZIPInputStream(ByteArrayInputStream(compressedPayload)).use { gzip ->
                    gzip.bufferedReader(Charsets.UTF_8).readText()
                }
            } else {
                // Fallback for plain JSON or raw GZIP
                try {
                    GZIPInputStream(ByteArrayInputStream(bytes)).use { gzip ->
                        gzip.bufferedReader(Charsets.UTF_8).readText()
                    }
                } catch (_: Exception) {
                    String(bytes, Charsets.UTF_8)
                }
            }

            val rootObj = JSONObject(jsonText)

            val isRestoFile = rootObj.has("app") || rootObj.has("backupVersion") || rootObj.has("products") || rootObj.has("salesOrders")
            if (!isRestoFile) {
                return@withContext Result.failure(Exception("فرمت فایل غیرمجاز است. این فایل متعلق به پشتیبان سیستم Resto نیست."))
            }

            var productsRestored = 0
            var salesOrdersRestored = 0
            var customersRestored = 0
            var suppliersRestored = 0
            var purchasesRestored = 0
            var expensesRestored = 0
            var ledgerEntriesRestored = 0

            // Organizations
            if (rootObj.has("organizations")) {
                val arr = rootObj.getJSONArray("organizations")
                val list = mutableListOf<OrganizationEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        OrganizationEntity(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            code = o.optString("code"),
                            logoUrl = o.optString("logoUrl"),
                            currencySymbol = o.optString("currencySymbol", "تومان"),
                            currencyCode = o.optString("currencyCode", "TOMAN"),
                            subscriptionTier = o.optString("subscriptionTier", "ENTERPRISE")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    database.tenantDao().insertOrganizations(list)
                }
            }

            // Stores
            if (rootObj.has("stores")) {
                val arr = rootObj.getJSONArray("stores")
                val list = mutableListOf<StoreEntity>()
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    list.add(
                        StoreEntity(
                            id = s.optString("id"),
                            orgId = s.optString("orgId"),
                            name = s.optString("name"),
                            code = s.optString("code"),
                            address = s.optString("address"),
                            phone = s.optString("phone")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    database.tenantDao().insertStores(list)
                }
            }

            // Warehouses
            if (rootObj.has("warehouses")) {
                val arr = rootObj.getJSONArray("warehouses")
                val list = mutableListOf<WarehouseEntity>()
                for (i in 0 until arr.length()) {
                    val w = arr.getJSONObject(i)
                    list.add(
                        WarehouseEntity(
                            id = w.optString("id"),
                            storeId = w.optString("storeId"),
                            orgId = w.optString("orgId"),
                            name = w.optString("name"),
                            code = w.optString("code"),
                            address = w.optString("address")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    database.tenantDao().insertWarehouses(list)
                }
            }

            // Products
            if (rootObj.has("products")) {
                val arr = rootObj.getJSONArray("products")
                val productEntities = mutableListOf<ProductEntity>()
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    val stockMap = mutableMapOf<String, Int>()
                    if (p.has("stockMap")) {
                        val stockObj = p.optJSONObject("stockMap")
                        if (stockObj != null) {
                            val keys = stockObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                stockMap[key] = stockObj.optInt(key, 0)
                            }
                        }
                    }

                    productEntities.add(
                        ProductEntity(
                            id = p.optString("id"),
                            orgId = p.optString("orgId"),
                            sku = p.optString("sku"),
                            barcode = p.optString("barcode"),
                            name = p.optString("name"),
                            description = p.optString("description"),
                            price = p.optDouble("price", 0.0),
                            costPrice = p.optDouble("costPrice", 0.0),
                            category = p.optString("category", "عمومی"),
                            unit = p.optString("unit", "عدد"),
                            imageUrl = p.optString("imageUrl"),
                            variants = emptyList(),
                            stockQuantityByWarehouse = stockMap,
                            isSynced = p.optBoolean("isSynced", false)
                        )
                    )
                }
                if (productEntities.isNotEmpty()) {
                    database.productDao().insertProducts(productEntities)
                    productsRestored = productEntities.size
                }
            }

            // Sales Orders
            if (rootObj.has("salesOrders")) {
                val arr = rootObj.getJSONArray("salesOrders")
                val orderEntities = mutableListOf<SalesOrderEntity>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val itemsArr = o.optJSONArray("items") ?: JSONArray()
                    val orderItems = mutableListOf<OrderItem>()
                    for (j in 0 until itemsArr.length()) {
                        val itemObj = itemsArr.getJSONObject(j)
                        orderItems.add(
                            OrderItem(
                                productId = itemObj.optString("productId"),
                                variantId = itemObj.optString("variantId").ifBlank { null },
                                productName = itemObj.optString("productName"),
                                sku = itemObj.optString("sku"),
                                unitPrice = itemObj.optDouble("unitPrice", 0.0),
                                quantity = itemObj.optInt("quantity", 1),
                                discountPercent = itemObj.optDouble("discountPercent", 0.0),
                                taxAmount = itemObj.optDouble("taxAmount", 0.0),
                                totalPrice = itemObj.optDouble("totalPrice", 0.0)
                            )
                        )
                    }

                    orderEntities.add(
                        SalesOrderEntity(
                            id = o.optString("id"),
                            orderNumber = o.optString("orderNumber"),
                            orgId = o.optString("orgId"),
                            storeId = o.optString("storeId"),
                            warehouseId = o.optString("warehouseId"),
                            customerId = o.optString("customerId").ifBlank { null },
                            customerName = o.optString("customerName", "بدون نام"),
                            items = orderItems,
                            subtotal = o.optDouble("subtotal", 0.0),
                            discountAmount = o.optDouble("discountAmount", 0.0),
                            taxAmount = o.optDouble("taxAmount", 0.0),
                            totalAmount = o.optDouble("totalAmount", 0.0),
                            paymentMethod = try { PaymentMethod.valueOf(o.optString("paymentMethod", "CASH")) } catch (_: Exception) { PaymentMethod.CASH },
                            paymentStatus = try { PaymentStatus.valueOf(o.optString("paymentStatus", "PAID")) } catch (_: Exception) { PaymentStatus.PAID },
                            fulfillmentStatus = try { FulfillmentStatus.valueOf(o.optString("fulfillmentStatus", "COMPLETED")) } catch (_: Exception) { FulfillmentStatus.COMPLETED },
                            notes = o.optString("notes"),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            isSynced = o.optBoolean("isSynced", false)
                        )
                    )
                }
                if (orderEntities.isNotEmpty()) {
                    database.salesOrderDao().insertSalesOrders(orderEntities)
                    salesOrdersRestored = orderEntities.size
                }
            }

            // Customers
            if (rootObj.has("customers")) {
                val arr = rootObj.getJSONArray("customers")
                val customerEntities = mutableListOf<CustomerEntity>()
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    customerEntities.add(
                        CustomerEntity(
                            id = c.optString("id"),
                            orgId = c.optString("orgId"),
                            name = c.optString("name"),
                            phone = c.optString("phone"),
                            email = c.optString("email"),
                            address = c.optString("address"),
                            totalPurchases = c.optDouble("totalPurchases", 0.0),
                            loyaltyPoints = c.optInt("loyaltyPoints", 0),
                            createdAt = c.optLong("createdAt", System.currentTimeMillis()),
                            isSynced = c.optBoolean("isSynced", false)
                        )
                    )
                }
                if (customerEntities.isNotEmpty()) {
                    database.customerDao().insertCustomers(customerEntities)
                    customersRestored = customerEntities.size
                }
            }

            // Suppliers
            if (rootObj.has("suppliers")) {
                val arr = rootObj.getJSONArray("suppliers")
                val supplierEntities = mutableListOf<SupplierEntity>()
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    supplierEntities.add(
                        SupplierEntity(
                            id = s.optString("id"),
                            orgId = s.optString("orgId"),
                            name = s.optString("name"),
                            phone = s.optString("phone"),
                            email = s.optString("email"),
                            address = s.optString("address"),
                            createdAt = s.optLong("createdAt", System.currentTimeMillis()),
                            isSynced = s.optBoolean("isSynced", false)
                        )
                    )
                }
                if (supplierEntities.isNotEmpty()) {
                    database.supplierDao().insertSuppliers(supplierEntities)
                    suppliersRestored = supplierEntities.size
                }
            }

            // Purchases
            if (rootObj.has("purchases")) {
                val arr = rootObj.getJSONArray("purchases")
                val purchaseEntities = mutableListOf<PurchaseEntity>()
                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    val purchaseItemsArr = p.optJSONArray("items") ?: JSONArray()
                    val purchaseItemsList = mutableListOf<PurchaseItem>()
                    for (j in 0 until purchaseItemsArr.length()) {
                        val itemObj = purchaseItemsArr.getJSONObject(j)
                        purchaseItemsList.add(
                            PurchaseItem(
                                id = itemObj.optString("id"),
                                purchaseId = itemObj.optString("purchaseId"),
                                productId = itemObj.optString("productId"),
                                productName = itemObj.optString("productName"),
                                quantity = itemObj.optInt("quantity", 1),
                                unitCost = itemObj.optDouble("unitCost", 0.0),
                                totalCost = itemObj.optDouble("totalCost", 0.0)
                            )
                        )
                    }

                    purchaseEntities.add(
                        PurchaseEntity(
                            id = p.optString("id"),
                            purchaseNumber = p.optString("purchaseNumber"),
                            orgId = p.optString("orgId"),
                            storeId = p.optString("storeId"),
                            warehouseId = p.optString("warehouseId"),
                            supplierId = p.optString("supplierId"),
                            supplierName = p.optString("supplierName"),
                            items = purchaseItemsList,
                            totalAmount = p.optDouble("totalAmount", 0.0),
                            status = p.optString("status", "COMPLETED"),
                            paymentStatus = p.optString("paymentStatus", "PAID"),
                            createdAt = p.optLong("createdAt", System.currentTimeMillis()),
                            isSynced = p.optBoolean("isSynced", false)
                        )
                    )
                }
                if (purchaseEntities.isNotEmpty()) {
                    database.purchaseDao().insertPurchases(purchaseEntities)
                    purchasesRestored = purchaseEntities.size
                }
            }

            // Expenses
            if (rootObj.has("expenses")) {
                val arr = rootObj.getJSONArray("expenses")
                val expenseEntities = mutableListOf<ExpenseEntity>()
                for (i in 0 until arr.length()) {
                    val e = arr.getJSONObject(i)
                    expenseEntities.add(
                        ExpenseEntity(
                            id = e.optString("id"),
                            orgId = e.optString("orgId"),
                            storeId = e.optString("storeId"),
                            category = e.optString("category"),
                            amount = e.optDouble("amount", 0.0),
                            paymentMethod = e.optString("paymentMethod", "CASH"),
                            date = e.optString("date"),
                            notes = e.optString("notes"),
                            userId = e.optString("userId").ifBlank { null },
                            createdAt = e.optLong("createdAt", System.currentTimeMillis()),
                            isSynced = e.optBoolean("isSynced", false)
                        )
                    )
                }
                if (expenseEntities.isNotEmpty()) {
                    database.expenseDao().insertExpenses(expenseEntities)
                    expensesRestored = expenseEntities.size
                }
            }

            // Ledger Entries
            if (rootObj.has("ledgerEntries")) {
                val arr = rootObj.getJSONArray("ledgerEntries")
                val ledgerEntities = mutableListOf<LedgerEntryEntity>()
                for (i in 0 until arr.length()) {
                    val l = arr.getJSONObject(i)
                    ledgerEntities.add(
                        LedgerEntryEntity(
                            id = l.optString("id"),
                            orgId = l.optString("orgId"),
                            storeId = l.optString("storeId"),
                            entryNumber = l.optString("entryNumber"),
                            type = try { LedgerType.valueOf(l.optString("type", "DEBIT")) } catch (_: Exception) { LedgerType.DEBIT },
                            category = try { LedgerCategory.valueOf(l.optString("category", "EXPENSE")) } catch (_: Exception) { LedgerCategory.EXPENSE },
                            amount = l.optDouble("amount", 0.0),
                            description = l.optString("description"),
                            referenceId = l.optString("referenceId").ifBlank { null },
                            createdAt = l.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (ledgerEntities.isNotEmpty()) {
                    database.ledgerDao().insertLedgerEntries(ledgerEntities)
                    ledgerEntriesRestored = ledgerEntities.size
                }
            }

            Result.success(
                BackupRestoreSummary(
                    productsCount = productsRestored,
                    salesOrdersCount = salesOrdersRestored,
                    customersCount = customersRestored,
                    suppliersCount = suppliersRestored,
                    purchasesCount = purchasesRestored,
                    expensesCount = expensesRestored,
                    ledgerEntriesCount = ledgerEntriesRestored
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("خطا در بازگردانی فایل پشتیبان: ${e.localizedMessage}"))
        }
    }
}
