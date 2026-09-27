package com.braveboy.calcuapp

import com.braveboy.calcuapp.data.mock.MockDataArchive
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PosAndInventoryUnitTest {

    @Test
    fun mockProducts_verifyStockBreakdownAndVariants() {
        val laptop = MockDataArchive.products.find { it.id == "prod_1" }
        assertNotNull(laptop)
        assertTrue(laptop!!.variants.isNotEmpty())
        val totalStock = laptop.getTotalStock()
        assertTrue(totalStock > 0)
        assertEquals(22, laptop.getStockForWarehouse("wh_apex_1a"))
    }

    @Test
    fun barcodeLookup_inMockData() {
        val barcode = "880609123401"
        val matched = MockDataArchive.products.find { it.barcode == barcode }
        assertNotNull(matched)
        assertEquals("ProBook Ultra 15 M3", matched?.name)
    }

    @Test
    fun checkoutTotals_cashMath() {
        val cartItems = listOf(
            CartItem(
                id = "item_1",
                orgId = "org_apex",
                storeId = "store_apex_1",
                productId = "prod_1",
                productName = "ProBook Ultra 15 M3",
                sku = "APX-LAP-001",
                barcode = "880609123401",
                price = 1000.0,
                quantity = 1,
                discountPercent = 0.0,
                taxRate = 0.08
            )
        )

        val totalAmount = cartItems.sumOf { it.total }
        val cashTendered = 1100.0
        val changeDue = (cashTendered - totalAmount).coerceAtLeast(0.0)

        assertEquals(1080.0, totalAmount, 0.01)
        assertEquals(20.0, changeDue, 0.01)
    }
}
