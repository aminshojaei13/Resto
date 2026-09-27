package com.braveboy.calcuapp

import com.braveboy.calcuapp.data.mock.MockDataArchive
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.SalesOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataLayerUnitTest {

    @Test
    fun mockDataSource_containsOrganizationsAndProducts() {
        val orgs = MockDataArchive.organizations
        assertTrue(orgs.isNotEmpty())
        val apexOrg = orgs.find { it.id == "org_apex" }
        assertNotNull(apexOrg)
        assertEquals("Apex Retail Group", apexOrg?.name)
        assertTrue(apexOrg!!.stores.isNotEmpty())

        val products = MockDataArchive.products
        assertTrue(products.isNotEmpty())
        val apexProducts = products.filter { it.orgId == "org_apex" }
        assertTrue(apexProducts.size >= 5)
    }

    @Test
    fun cartItem_calculations_areCorrect() {
        val item = CartItem(
            id = "test_cart_1",
            orgId = "org_apex",
            storeId = "store_apex_1",
            productId = "prod_1",
            productName = "ProBook Ultra 15 M3",
            sku = "APX-LAP-001",
            barcode = "880609123401",
            price = 1000.0,
            quantity = 2,
            discountPercent = 10.0, // 10% discount -> $200 discount
            taxRate = 0.08 // 8% tax
        )

        assertEquals(2000.0, item.subtotal, 0.01)
        assertEquals(200.0, item.discountAmount, 0.01)
        assertEquals(1800.0, item.taxableAmount, 0.01)
        assertEquals(144.0, item.taxAmount, 0.01)
        assertEquals(1944.0, item.total, 0.01)
    }

    @Test
    fun salesOrderAndLedgerEntry_modelCreation() {
        val order = SalesOrder(
            id = "ord_test_1",
            orderNumber = "ORD-2025-9999",
            orgId = "org_apex",
            storeId = "store_apex_1",
            warehouseId = "wh_apex_1a",
            customerId = "cust_1",
            customerName = "Sarah Connor",
            items = emptyList(),
            subtotal = 500.0,
            discountAmount = 50.0,
            taxAmount = 36.0,
            totalAmount = 486.0,
            paymentMethod = PaymentMethod.CARD,
            paymentStatus = PaymentStatus.PAID,
            fulfillmentStatus = FulfillmentStatus.COMPLETED
        )

        assertEquals("ORD-2025-9999", order.orderNumber)
        assertEquals(PaymentMethod.CARD, order.paymentMethod)

        val ledger = LedgerEntry(
            id = "leg_test_1",
            orgId = "org_apex",
            storeId = "store_apex_1",
            entryNumber = "LEDG-2025-9999",
            type = LedgerType.CREDIT,
            category = LedgerCategory.SALES,
            amount = 486.0,
            description = "Sales Order payment",
            referenceId = order.id
        )

        assertEquals(LedgerType.CREDIT, ledger.type)
        assertEquals(LedgerCategory.SALES, ledger.category)
        assertEquals(486.0, ledger.amount, 0.01)
    }
}
