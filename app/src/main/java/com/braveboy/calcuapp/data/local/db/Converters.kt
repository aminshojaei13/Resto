package com.braveboy.calcuapp.data.local.db

import androidx.room.TypeConverter
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.data.model.OrderItem
import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.ProductVariant
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromProductVariantList(value: List<ProductVariant>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, ProductVariant::class.java)
        val adapter = moshi.adapter<List<ProductVariant>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toProductVariantList(value: String): List<ProductVariant> {
        if (value.isBlank()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, ProductVariant::class.java)
        val adapter = moshi.adapter<List<ProductVariant>>(type)
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>?): String {
        if (value == null) return "{}"
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
        val adapter = moshi.adapter<Map<String, Int>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toStringIntMap(value: String): Map<String, Int> {
        if (value.isBlank()) return emptyMap()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Int::class.javaObjectType)
        val adapter = moshi.adapter<Map<String, Int>>(type)
        return try {
            adapter.fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromOrderItemList(value: List<OrderItem>?): String {
        if (value == null) return "[]"
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        return adapter.toJson(value)
    }

    @TypeConverter
    fun toOrderItemList(value: String): List<OrderItem> {
        if (value.isBlank()) return emptyList()
        val type = Types.newParameterizedType(List::class.java, OrderItem::class.java)
        val adapter = moshi.adapter<List<OrderItem>>(type)
        return try {
            adapter.fromJson(value) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = try {
        PaymentMethod.valueOf(value)
    } catch (e: Exception) {
        PaymentMethod.CASH
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus = try {
        PaymentStatus.valueOf(value)
    } catch (e: Exception) {
        PaymentStatus.PAID
    }

    @TypeConverter
    fun fromFulfillmentStatus(value: FulfillmentStatus): String = value.name

    @TypeConverter
    fun toFulfillmentStatus(value: String): FulfillmentStatus = try {
        FulfillmentStatus.valueOf(value)
    } catch (e: Exception) {
        FulfillmentStatus.COMPLETED
    }

    @TypeConverter
    fun fromLedgerType(value: LedgerType): String = value.name

    @TypeConverter
    fun toLedgerType(value: String): LedgerType = try {
        LedgerType.valueOf(value)
    } catch (e: Exception) {
        LedgerType.CREDIT
    }

    @TypeConverter
    fun fromLedgerCategory(value: LedgerCategory): String = value.name

    @TypeConverter
    fun toLedgerCategory(value: String): LedgerCategory = try {
        LedgerCategory.valueOf(value)
    } catch (e: Exception) {
        LedgerCategory.SALES
    }
}
