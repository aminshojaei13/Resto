package com.braveboy.calcuapp

import com.braveboy.calcuapp.data.mock.MockSaaSDataSource
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesAccountingAndCustomerUnitTest {

    @Test
    fun customers_mockData_containsApexCustomers() {
        val customers = MockSaaSDataSource.customers
        assertTrue(customers.isNotEmpty())
        val sarah = customers.find { it.name == "Sarah Connor" }
        assertNotNull(sarah)
        assertEquals("org_apex", sarah?.orgId)
        assertTrue(sarah!!.loyaltyPoints > 0)
    }

    @Test
    fun ledgerEntries_typeAndCategoryMatch() {
        val entries = MockSaaSDataSource.ledgerEntries
        assertTrue(entries.isNotEmpty())
        val salesEntry = entries.find { it.category == LedgerCategory.SALES }
        assertNotNull(salesEntry)
        assertEquals(LedgerType.CREDIT, salesEntry?.type)
    }

    @Test
    fun salesOrders_mockData_containsOrdersWithItems() {
        val orders = MockSaaSDataSource.salesOrders
        assertTrue(orders.isNotEmpty())
        val firstOrder = orders.first()
        assertTrue(firstOrder.items.isNotEmpty())
        assertTrue(firstOrder.totalAmount > 0.0)
    }
}
