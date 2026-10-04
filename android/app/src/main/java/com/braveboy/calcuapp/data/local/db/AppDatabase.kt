package com.braveboy.calcuapp.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.braveboy.calcuapp.data.local.db.dao.CartDao
import com.braveboy.calcuapp.data.local.db.dao.CustomerDao
import com.braveboy.calcuapp.data.local.db.dao.ExpenseDao
import com.braveboy.calcuapp.data.local.db.dao.LedgerDao
import com.braveboy.calcuapp.data.local.db.dao.ProductDao
import com.braveboy.calcuapp.data.local.db.dao.PurchaseDao
import com.braveboy.calcuapp.data.local.db.dao.SalesOrderDao
import com.braveboy.calcuapp.data.local.db.dao.SupplierDao
import com.braveboy.calcuapp.data.local.db.dao.TenantDao
import com.braveboy.calcuapp.data.local.db.entity.CartItemEntity
import com.braveboy.calcuapp.data.local.db.entity.CustomerEntity
import com.braveboy.calcuapp.data.local.db.entity.ExpenseEntity
import com.braveboy.calcuapp.data.local.db.entity.LedgerEntryEntity
import com.braveboy.calcuapp.data.local.db.entity.OrganizationEntity
import com.braveboy.calcuapp.data.local.db.entity.ProductEntity
import com.braveboy.calcuapp.data.local.db.entity.PurchaseEntity
import com.braveboy.calcuapp.data.local.db.entity.SalesOrderEntity
import com.braveboy.calcuapp.data.local.db.entity.StoreEntity
import com.braveboy.calcuapp.data.local.db.entity.SupplierEntity
import com.braveboy.calcuapp.data.local.db.entity.WarehouseEntity

@Database(
    entities = [
        OrganizationEntity::class,
        StoreEntity::class,
        WarehouseEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        PurchaseEntity::class,
        ExpenseEntity::class,
        SalesOrderEntity::class,
        LedgerEntryEntity::class,
        CartItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tenantDao(): TenantDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun salesOrderDao(): SalesOrderDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calcuapp_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
