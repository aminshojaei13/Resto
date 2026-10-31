import com.braveboy.resto.data.local.db.entity.toEntity
import com.braveboy.resto.data.model.Customer
import com.braveboy.resto.data.model.Expense
import com.braveboy.resto.data.model.FulfillmentStatus
import com.braveboy.resto.data.model.PaymentMethod
import com.braveboy.resto.data.model.PaymentStatus
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.Purchase
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.data.model.Supplier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OfflineSyncUnitTest {

    @Test
    fun salesOrderEntity_defaultsSyncedToTrue_andCanBeSetUnsynced() {
        val order = SalesOrder(
            id = UUID.randomUUID().toString(),
            orderNumber = "ORD-OFFLINE-001",
            orgId = "org_1",
            storeId = "store_1",
            warehouseId = "wh_1",
            customerId = null,
            customerName = "مشتری حضوری",
            items = emptyList(),
            subtotal = 500000.0,
            discountAmount = 0.0,
            taxAmount = 0.0,
            totalAmount = 500000.0,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.PAID,
            fulfillmentStatus = FulfillmentStatus.COMPLETED,
            notes = "ثبت آفلاین",
            createdAt = System.currentTimeMillis()
        )

        val syncedEntity = order.toEntity(isSynced = true)
        val unsyncedEntity = order.toEntity(isSynced = false)

        assertTrue(syncedEntity.isSynced)
        assertFalse(unsyncedEntity.isSynced)
    }

    @Test
    fun productEntity_supportsOfflineUnsyncedFlag() {
        val product = Product(
            id = "prod_offline_1",
            orgId = "org_1",
            sku = "SKU-001",
            barcode = "123456789",
            name = "کالای آفلاین نمونه",
            description = "توضیحات کالا",
            price = 150000.0,
            costPrice = 100000.0,
            category = "General",
            unit = "عدد"
        )

        val unsyncedProduct = product.toEntity(isSynced = false)
        assertFalse(unsyncedProduct.isSynced)
    }

    @Test
    fun customerAndExpenseAndSupplier_supportUnsyncedFlags() {
        val customer = Customer(
            id = "cust_offline_1",
            orgId = "org_1",
            name = "علی محمدی",
            email = "ali@example.com",
            phone = "09121112233",
            address = "تهران"
        )
        val expense = Expense(
            id = "exp_offline_1",
            orgId = "org_1",
            storeId = "store_1",
            category = "Utilities",
            amount = 250000.0,
            paymentMethod = "CASH",
            date = "2026-03-30",
            notes = "قبوض کارگاه"
        )
        val supplier = Supplier(
            id = "supp_offline_1",
            orgId = "org_1",
            name = "شرکت بازرگانی آریا",
            email = "info@arya.ir",
            phone = "02188889999",
            address = "بازار بزرگ"
        )

        val unsyncedCust = customer.toEntity(isSynced = false)
        val unsyncedExp = expense.toEntity(isSynced = false)
        val unsyncedSupp = supplier.toEntity(isSynced = false)

        assertFalse(unsyncedCust.isSynced)
        assertFalse(unsyncedExp.isSynced)
        assertFalse(unsyncedSupp.isSynced)
    }
}
