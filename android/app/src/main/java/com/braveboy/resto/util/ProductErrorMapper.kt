package com.braveboy.resto.util

data class ProductValidationErrors(
    val nameError: String? = null,
    val skuError: String? = null,
    val priceError: String? = null,
    val costPriceError: String? = null,
    val categoryError: String? = null,
    val unitError: String? = null,
    val stockError: String? = null
) {
    val hasError: Boolean
        get() = nameError != null || skuError != null || priceError != null ||
                costPriceError != null || categoryError != null || unitError != null || stockError != null
}

object ProductErrorMapper {

    fun validate(
        name: String,
        sku: String,
        priceStr: String,
        costPriceStr: String,
        category: String,
        unit: String,
        stockStr: String? = null
    ): ProductValidationErrors {
        val nameErr = if (name.trim().isEmpty()) "نام کالا نمی‌تواند خالی باشد" else null
        val skuErr = if (sku.trim().isEmpty()) "کد SKU الزامی است" else null

        val priceVal = PersianFormatter.parsePrice(priceStr)
        val priceErr = when {
            priceStr.trim().isEmpty() -> "قیمت فروش الزامی است"
            priceVal <= 0 -> "قیمت فروش باید بزرگتر از صفر باشد"
            else -> null
        }

        val costVal = if (costPriceStr.trim().isEmpty()) 0.0 else PersianFormatter.parsePrice(costPriceStr)
        val costErr = if (costPriceStr.trim().isNotEmpty() && costVal < 0) "قیمت خرید نامعتبر است" else null

        val catErr = if (category.trim().isEmpty()) "دسته‌بندی الزامی است" else null
        val unitErr = if (unit.trim().isEmpty()) "واحد سنجش الزامی است" else null

        val stockErr = if (stockStr != null) {
            val stockVal = stockStr.trim().toIntOrNull()
            if (stockStr.trim().isEmpty() || stockVal == null || stockVal < 0) "موجودی اولیه باید یک عدد معتبر باشد" else null
        } else null

        return ProductValidationErrors(
            nameError = nameErr,
            skuError = skuErr,
            priceError = priceErr,
            costPriceError = costErr,
            categoryError = catErr,
            unitError = unitErr,
            stockError = stockErr
        )
    }

    fun getPersianError(errorCode: String): String {
        return when (errorCode.lowercase()) {
            "name_required" -> "نام کالا نمی‌تواند خالی باشد"
            "sku_required" -> "کد SKU الزامی است"
            "price_invalid" -> "قیمت فروش باید بزرگتر از صفر باشد"
            "cost_invalid" -> "قیمت خرید نامعتبر است"
            "category_required" -> "دسته‌بندی الزامی است"
            "unit_required" -> "واحد سنجش الزامی است"
            "stock_invalid" -> "موجودی اولیه نامعتبر است"
            "duplicate_sku" -> "این کد SKU قبلاً ثبت شده است"
            "duplicate_barcode" -> "این بارکد قبلاً برای کالای دیگری ثبت شده است"
            else -> "اطلاعات وارد شده نامعتبر است"
        }
    }
}
