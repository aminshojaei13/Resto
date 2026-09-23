package com.braveboy.calcuapp.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.braveboy.calcuapp.data.local.db.entity.CartItemEntity
import com.braveboy.calcuapp.data.local.db.entity.CustomerEntity
import com.braveboy.calcuapp.data.local.db.entity.LedgerEntryEntity
import com.braveboy.calcuapp.data.local.db.entity.OrganizationEntity
import com.braveboy.calcuapp.data.local.db.entity.ProductEntity
import com.braveboy.calcuapp.data.local.db.entity.SalesOrderEntity
import com.braveboy.calcuapp.data.local.db.entity.StoreEntity
import com.braveboy.calcuapp.data.local.db.entity.WarehouseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TenantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganizations(organizations: List<OrganizationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStores(stores: List<StoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouses(warehouses: List<WarehouseEntity>)

    @Query("SELECT * FROM organizations")
    fun getAllOrganizations(): Flow<List<OrganizationEntity>>

    @Query("SELECT * FROM stores WHERE orgId = :orgId")
    fun getStoresForOrg(orgId: String): Flow<List<StoreEntity>>

    @Query("SELECT * FROM warehouses WHERE storeId = :storeId")
    fun getWarehousesForStore(storeId: String): Flow<List<WarehouseEntity>>

    @Query("SELECT COUNT(*) FROM organizations")
    suspend fun getOrganizationCount(): Int
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE orgId = :orgId ORDER BY name ASC")
    fun getProductsByOrg(orgId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE orgId = :orgId AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')")
    fun searchProducts(orgId: String, query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductByIdDirect(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE orgId = :orgId AND barcode = :barcode LIMIT 1")
    fun getProductByBarcode(orgId: String, barcode: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE orgId = :orgId AND barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcodeDirect(orgId: String, barcode: String): ProductEntity?
}

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE orgId = :orgId ORDER BY name ASC")
    fun getCustomersByOrg(orgId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE orgId = :orgId AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%')")
    fun searchCustomers(orgId: String, query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: String): Flow<CustomerEntity?>
}

@Dao
interface SalesOrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesOrder(order: SalesOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesOrders(orders: List<SalesOrderEntity>)

    @Query("SELECT * FROM sales_orders WHERE orgId = :orgId AND storeId = :storeId ORDER BY createdAt DESC")
    fun getOrdersByStore(orgId: String, storeId: String): Flow<List<SalesOrderEntity>>

    @Query("SELECT * FROM sales_orders WHERE orgId = :orgId ORDER BY createdAt DESC")
    fun getAllOrdersByOrg(orgId: String): Flow<List<SalesOrderEntity>>

    @Query("SELECT * FROM sales_orders WHERE id = :id")
    fun getOrderById(id: String): Flow<SalesOrderEntity?>

    @Query("SELECT COUNT(*) FROM sales_orders WHERE orgId = :orgId")
    suspend fun getOrderCountForOrg(orgId: String): Int
}

@Dao
interface LedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<LedgerEntryEntity>)

    @Query("SELECT * FROM ledger_entries WHERE orgId = :orgId AND storeId = :storeId ORDER BY createdAt DESC")
    fun getLedgerEntriesByStore(orgId: String, storeId: String): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE orgId = :orgId ORDER BY createdAt DESC")
    fun getLedgerEntriesByOrg(orgId: String): Flow<List<LedgerEntryEntity>>
}

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCartItem(cartItem: CartItemEntity)

    @Query("SELECT * FROM cart_items WHERE orgId = :orgId AND storeId = :storeId")
    fun getCartItems(orgId: String, storeId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE id = :id")
    suspend fun getCartItemById(id: String): CartItemEntity?

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteCartItem(id: String)

    @Query("DELETE FROM cart_items WHERE orgId = :orgId AND storeId = :storeId")
    suspend fun clearCart(orgId: String, storeId: String)
}
