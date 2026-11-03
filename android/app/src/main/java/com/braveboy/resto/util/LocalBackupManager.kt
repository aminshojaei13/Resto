package com.braveboy.resto.util

import android.content.Context
import android.os.Environment
import com.braveboy.resto.data.local.datastore.TenantPreferences
import com.braveboy.resto.data.local.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalBackupManager {

    suspend fun createLocalBackup(
        context: Context,
        database: AppDatabase,
        tenantPreferences: TenantPreferences?
    ): File? = withContext(Dispatchers.IO) {
        try {
            val rootObj = JSONObject()
            rootObj.put("app", "Resto Business OS")
            rootObj.put("backupVersion", "1.0")
            rootObj.put("createdTimestamp", System.currentTimeMillis())

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            rootObj.put("createdDate", dateFormat.format(Date()))

            // Tenant Info
            val tenantState = tenantPreferences?.tenantState?.first()
            val tenantJson = JSONObject().apply {
                put("orgId", tenantState?.activeOrgId ?: "")
                put("storeId", tenantState?.activeStoreId ?: "")
                put("warehouseId", tenantState?.activeWarehouseId ?: "")
                put("userId", tenantState?.userId ?: "")
                put("userName", tenantState?.userName ?: "")
                put("userRole", tenantState?.userRole ?: "")
            }
            rootObj.put("tenantInfo", tenantJson)

            // Products
            val products = database.productDao().getAllProductsDirect()
            val productsArray = JSONArray()
            for (p in products) {
                productsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("orgId", p.orgId)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("barcode", p.barcode)
                    put("price", p.price)
                    put("costPrice", p.costPrice)
                    put("category", p.category)
                    put("unit", p.unit)
                    put("isSynced", p.isSynced)
                })
            }
            rootObj.put("products", productsArray)

            // Sales Orders
            val orders = database.salesOrderDao().getAllSalesOrdersDirect()
            val ordersArray = JSONArray()
            for (o in orders) {
                ordersArray.put(JSONObject().apply {
                    put("id", o.id)
                    put("orderNumber", o.orderNumber)
                    put("orgId", o.orgId)
                    put("storeId", o.storeId)
                    put("customerId", o.customerId ?: "")
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
                })
            }
            rootObj.put("salesOrders", ordersArray)

            // Customers
            val customers = database.customerDao().getAllCustomersDirect()
            val customersArray = JSONArray()
            for (c in customers) {
                customersArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("email", c.email)
                    put("address", c.address)
                    put("totalPurchases", c.totalPurchases)
                    put("loyaltyPoints", c.loyaltyPoints)
                    put("isSynced", c.isSynced)
                })
            }
            rootObj.put("customers", customersArray)

            // Suppliers
            val suppliers = database.supplierDao().getAllSuppliersDirect()
            val suppliersArray = JSONArray()
            for (s in suppliers) {
                suppliersArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("phone", s.phone)
                    put("email", s.email)
                    put("address", s.address)
                    put("isSynced", s.isSynced)
                })
            }
            rootObj.put("suppliers", suppliersArray)

            // Purchases
            val purchases = database.purchaseDao().getAllPurchasesDirect()
            val purchasesArray = JSONArray()
            for (p in purchases) {
                purchasesArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("purchaseNumber", p.purchaseNumber)
                    put("supplierName", p.supplierName)
                    put("totalAmount", p.totalAmount)
                    put("status", p.status)
                    put("paymentStatus", p.paymentStatus)
                    put("createdAt", p.createdAt)
                    put("isSynced", p.isSynced)
                })
            }
            rootObj.put("purchases", purchasesArray)

            // Expenses
            val expenses = database.expenseDao().getAllExpensesDirect()
            val expensesArray = JSONArray()
            for (e in expenses) {
                expensesArray.put(JSONObject().apply {
                    put("id", e.id)
                    put("category", e.category)
                    put("amount", e.amount)
                    put("paymentMethod", e.paymentMethod)
                    put("date", e.date)
                    put("notes", e.notes)
                    put("isSynced", e.isSynced)
                })
            }
            rootObj.put("expenses", expensesArray)

            // Ledger Entries
            val ledgerEntries = database.ledgerDao().getAllLedgerEntriesDirect()
            val ledgerArray = JSONArray()
            for (l in ledgerEntries) {
                ledgerArray.put(JSONObject().apply {
                    put("id", l.id)
                    put("entryNumber", l.entryNumber)
                    put("description", l.description)
                    put("type", l.type.name)
                    put("amount", l.amount)
                    put("category", l.category.name)
                    put("createdAt", l.createdAt)
                })
            }
            rootObj.put("ledgerEntries", ledgerArray)

            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val fileTimestamp = fileDateFormat.format(Date())
            val fileName = "Resto_LocalBackup_$fileTimestamp.json"

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val backupFile = File(downloadsDir, fileName)

            FileOutputStream(backupFile).use { out ->
                out.write(rootObj.toString(2).toByteArray(Charsets.UTF_8))
            }

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareBackup(context: Context, backupFile: File) {
        ReportExportManager.shareFile(context, backupFile, "application/json")
    }
}
