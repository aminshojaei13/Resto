package com.braveboy.calcuapp.data.mock

import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.data.model.Expense
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.Organization
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.data.model.Supplier

/**
 * Empty DataSource for clean initialization and testing from scratch.
 * Archived mock dataset is stored in MockDataArchive.kt.
 */
object MockSaaSDataSource {
    val organizations: List<Organization> = emptyList()
    val products: List<Product> = emptyList()
    val customers: List<Customer> = emptyList()
    val suppliers: List<Supplier> = emptyList()
    val expenses: List<Expense> = emptyList()
    val salesOrders: List<SalesOrder> = emptyList()
    val ledgerEntries: List<LedgerEntry> = emptyList()
}
