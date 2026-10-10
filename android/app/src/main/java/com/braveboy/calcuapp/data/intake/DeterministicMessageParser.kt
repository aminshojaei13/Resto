package com.braveboy.calcuapp.data.intake

import com.braveboy.calcuapp.data.model.PaymentMethod
import com.braveboy.calcuapp.data.model.Product
import java.util.UUID

data class ParsedCustomer(
    val id: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val isNew: Boolean = true
)

data class ParsedItem(
    val productId: String? = null,
    val productName: String,
    val rawText: String,
    val sku: String? = null,
    val quantity: Int = 1,
    val unit: String = "piece",
    val unitLabel: String = "عدد",
    val catalogPrice: Double = 0.0,
    val statedPrice: Double? = null,
    val discountPercent: Double = 0.0,
    val isMatched: Boolean = false,
    val confidence: Float = 0f,
    val ambiguityReason: String? = null
)

data class ParsedOrderDraft(
    val id: String = UUID.randomUUID().toString(),
    val source: String,
    val rawText: String,
    val customer: ParsedCustomer,
    val items: List<ParsedItem>,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val notes: String = "",
    val taxRate: Double = 0.0,
    val warehouseId: String = ""
) {
    val subtotal: Double
        get() = items.sumOf {
            val unitP = if (it.catalogPrice > 0.0) it.catalogPrice else (it.statedPrice ?: 0.0)
            unitP * it.quantity
        }

    val discountAmount: Double
        get() = items.sumOf {
            val unitP = if (it.catalogPrice > 0.0) it.catalogPrice else (it.statedPrice ?: 0.0)
            (unitP * it.quantity) * (it.discountPercent / 100.0)
        }

    val taxableSubtotal: Double
        get() = (subtotal - discountAmount).coerceAtLeast(0.0)

    val taxAmount: Double
        get() = taxableSubtotal * (taxRate / 100.0)

    val grandTotal: Double
        get() = taxableSubtotal + taxAmount

    val hasUnmatchedItems: Boolean
        get() = items.any { !it.isMatched }

    val isCustomerComplete: Boolean
        get() = !customer.name.isNullOrBlank()
}

object DeterministicMessageParser {

    private val UNIT_WORDS = listOf(
        "عدد", "عددی", "تا", "بسته", "جعبه", "بطری", "دست", "سرو",
        "کیلو", "کیلوگرم", "گرم", "گرمی", "متر", "شاخه", "حلقه", "جفت",
        "کارتن", "پک", "رول", "طغری", "دستگاه",
        "piece", "pieces", "item", "items", "box", "boxes", "pack", "packs",
        "kg", "g", "m", "pcs"
    )

    private val NOISE_WORDS = listOf(
        "سلام", "درود", "سپاس", "تشکر", "لطفا", "لطفاً", "ببخشید", "ممنون",
        "میخوام", "می‌خوام", "خواستم", "میخواستم", "می‌خواستم", "بفرستید", "ارسال",
        "کنید", "بشه", "روز بخیر", "وقت بخیر", "هستم", "هستتم",
        "hello", "hi", "please", "thanks", "thank you", "send", "order"
    )

    /**
     * Normalizes digits: Persian (۰-۹) and Arabic (٠-٩) to standard English ASCII (0-9).
     * Normalizes Arabic Kaf (ك -> ک) and Yeh (ي -> ی).
     */
    fun normalizeDigitsAndChars(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append((ch - '۰' + '0'.code).toChar())
                in '٠'..'٩' -> sb.append((ch - '٠' + '0'.code).toChar())
                'ك' -> sb.append('ک')
                'ي' -> sb.append('ی')
                '\u200c' -> sb.append(' ') // Half-space to space for token matching
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Extract structured draft from raw message text and match with real catalog.
     */
    fun parse(
        rawText: String,
        source: String = "MANUAL_PASTE",
        catalog: List<Product> = emptyList(),
        taxRate: Double = 0.0,
        warehouseId: String = ""
    ): ParsedOrderDraft {
        val normalized = normalizeDigitsAndChars(rawText)
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }

        val customer = extractCustomer(lines)
        val paymentMethod = extractPaymentMethod(normalized)
        val notes = extractNotes(lines)
        val items = extractItems(lines, catalog)

        return ParsedOrderDraft(
            source = source,
            rawText = rawText,
            customer = customer,
            items = items,
            paymentMethod = paymentMethod,
            notes = notes,
            taxRate = taxRate,
            warehouseId = warehouseId
        )
    }

    private fun extractCustomer(lines: List<String>): ParsedCustomer {
        var name: String? = null
        var phone: String? = null
        var address: String? = null

        val nameRegex = Regex("^(?:مشتری|نام(?:\\s+و\\s+نام\\s+خانوادگی|\\s+خانوادگی)?|گیرنده|تحویل\\s+گیرنده|name|customer)\\s*[:：\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val phoneRegex = Regex("^(?:تلفن(?:\\s+همراه)?|موبایل|همراه|شماره(?:\\s+تماس|\\s+موبایل|\\s+همراه)?|تماس|phone|mobile|tel)\\s*[:：\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val addressRegex = Regex("^(?:آدرس(?:\\s+تحویل|\\s+ارسال)?|نشانی|مقصد|تحویل|محل\\s+تحویل|address|destination)\\s*[:：\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)

        for (line in lines) {
            nameRegex.find(line)?.let { match ->
                if (name == null) name = cleanCustomerName(match.groupValues[1])
            }
            phoneRegex.find(line)?.let { match ->
                if (phone == null) phone = extractDigitsOnly(match.groupValues[1])
            }
            addressRegex.find(line)?.let { match ->
                if (address == null) address = match.groupValues[1].trim()
            }
        }

        // Conversational fallback for name: e.g. "علی رضایی هستم"
        if (name == null) {
            val conversationalNameRegex = Regex("(?:سلام\\s+)?(?:من\\s+)?([\\p{L}\\s]{2,30}?)\\s+(?:هستم|هستتم|باشم)")
            for (line in lines) {
                conversationalNameRegex.find(line)?.let { match ->
                    val candidate = cleanCustomerName(match.groupValues[1])
                    if (candidate.length >= 2 && !NOISE_WORDS.contains(candidate)) {
                        name = candidate
                        return@let
                    }
                }
                if (name != null) break
            }
        }

        // Standalone phone number line fallback: 09121234567 or +98912...
        if (phone == null) {
            val standalonePhone = Regex("^(\\+?\\d[\\d\\s\\-]{8,15})$")
            for (line in lines) {
                standalonePhone.find(line)?.let { match ->
                    val digits = extractDigitsOnly(match.groupValues[1])
                    if (digits.length >= 8) {
                        phone = digits
                        return@let
                    }
                }
                if (phone != null) break
            }
        }

        return ParsedCustomer(
            name = name,
            phone = phone,
            address = address,
            isNew = true
        )
    }

    private fun cleanCustomerName(raw: String): String {
        return raw.replace(Regex("^(?:سلام|درود|عزیز|آقا|خانم)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(?:هستم|هستتم|باشم|هست)$"), "")
            .trim()
    }

    private fun extractDigitsOnly(raw: String): String {
        return raw.filter { it.isDigit() || it == '+' }
    }

    private fun extractPaymentMethod(normalizedText: String): PaymentMethod {
        val lower = normalizedText.lowercase()
        return when {
            lower.contains("کارت به کارت") || lower.contains("حواله") || lower.contains("انتقال") || lower.contains("transfer") || lower.contains("qr") -> PaymentMethod.MOBILE_PAYMENT
            lower.contains("کارت") || lower.contains("پوز") || lower.contains("card") || lower.contains("pos") -> PaymentMethod.CARD
            lower.contains("اعتبار") || lower.contains("نسیه") || lower.contains("چک") || lower.contains("credit") || lower.contains("cheque") -> PaymentMethod.STORE_CREDIT
            else -> PaymentMethod.CASH
        }
    }

    private fun extractNotes(lines: List<String>): String {
        val noteRegex = Regex("^(?:توضیحات|یادداشت|نکته|note|notes)\\s*[:：\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        for (line in lines) {
            noteRegex.find(line)?.let {
                return it.groupValues[1].trim()
            }
        }
        return ""
    }

    private fun isMetadataOrNoise(line: String): Boolean {
        val trimmed = line.trim()

        // Headers like "سفارش:", "سفارش من:", "اقلام:", "اقلام سفارش:", "items:", "order:"
        val headerRegex = Regex("^(?:سفارش(?:\\s+من)?|اقلام(?:\\s+سفارش)?|لیست(?:\\s+اقلام|\\s+سفارش)?|سبد\\s+خرید|order|items|products)\\s*[:：\\-]\\s*$", RegexOption.IGNORE_CASE)
        if (headerRegex.matches(trimmed)) return true

        val prefixes = listOf(
            "مشتری", "نام", "نام خانوادگی", "نام و نام خانوادگی",
            "تلفن", "تلفن همراه", "موبایل", "همراه", "شماره تماس", "شماره همراه", "شماره موبایل", "شماره",
            "آدرس", "نشانی", "مقصد", "آدرس تحویل", "محل تحویل",
            "پرداخت", "روش پرداخت", "نحوه پرداخت", "واریز",
            "توضیحات", "یادداشت", "نکته",
            "سفارش", "اقلام", "لیست",
            "customer", "name", "phone", "mobile", "tel", "address", "destination", "payment", "note", "notes", "order", "items"
        )
        for (p in prefixes) {
            val prefixRegex = Regex("^$p\\s*[:：\\-]", RegexOption.IGNORE_CASE)
            if (prefixRegex.containsMatchIn(trimmed)) {
                return true
            }
        }

        // Standalone phone number
        if (Regex("^(\\+?\\d[\\d\\s\\-]{8,15})$").matches(trimmed)) return true

        // Conversational noise line
        if (NOISE_WORDS.any { trimmed.equals(it, ignoreCase = true) || trimmed.equals("$it.", ignoreCase = true) }) return true

        // Friendly closings e.g. "ممنون", "تشکر", "با تشکر", "خیلی ممنون"
        val closingRegex = Regex("^(?:خیلی\\s+)?(?:ممنون|تشکر|سپاس|مرسی)(?:\\s+از\\s+شما)?\\s*[.!؟]?\\s*$", RegexOption.IGNORE_CASE)
        if (closingRegex.matches(trimmed)) return true

        return false
    }

    private fun extractItems(lines: List<String>, catalog: List<Product>): List<ParsedItem> {
        val items = mutableListOf<ParsedItem>()
        val unitPattern = UNIT_WORDS.joinToString("|")
        val countUnitRegex = Regex("(?:^|\\s)(\\d+)\\s*($unitPattern)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val countXRegex = Regex("(?:^|\\s)(\\d+)\\s*[x×*]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val nameDashCountRegex = Regex("^(.+?)\\s*[-–—]\\s*(\\d+)(?:\\s*($unitPattern))?$", RegexOption.IGNORE_CASE)
        val nameCountUnitRegex = Regex("^(.+?)\\s+(\\d+)\\s*($unitPattern)$", RegexOption.IGNORE_CASE)
        val countNameRegex = Regex("(?:^|\\s)(\\d+)\\s+([\\p{L}].+)$", RegexOption.IGNORE_CASE)

        for (raw in lines) {
            if (isMetadataOrNoise(raw)) continue

            var quantity = 1
            var unitWord = "عدد"
            var rawQuery = ""

            val match1 = countUnitRegex.find(raw)
            val match2 = countXRegex.find(raw)
            val match3 = nameDashCountRegex.find(raw)
            val match4 = nameCountUnitRegex.find(raw)
            val match5 = countNameRegex.find(raw)

            when {
                match1 != null -> {
                    quantity = match1.groupValues[1].toIntOrNull() ?: 1
                    unitWord = match1.groupValues[2]
                    rawQuery = match1.groupValues[3]
                }
                match2 != null -> {
                    quantity = match2.groupValues[1].toIntOrNull() ?: 1
                    rawQuery = match2.groupValues[2]
                }
                match3 != null -> {
                    rawQuery = match3.groupValues[1]
                    quantity = match3.groupValues[2].toIntOrNull() ?: 1
                    if (match3.groupValues[3].isNotBlank()) unitWord = match3.groupValues[3]
                }
                match4 != null -> {
                    rawQuery = match4.groupValues[1]
                    quantity = match4.groupValues[2].toIntOrNull() ?: 1
                    unitWord = match4.groupValues[3]
                }
                match5 != null -> {
                    quantity = match5.groupValues[1].toIntOrNull() ?: 1
                    rawQuery = match5.groupValues[2]
                }
                else -> {
                    rawQuery = raw
                    quantity = 1
                }
            }

            val cleanedQuery = cleanProductQuery(rawQuery)
            if (cleanedQuery.isBlank() || NOISE_WORDS.contains(cleanedQuery)) continue

            // Explicit stated price check if present in text, e.g. "به قیمت 50000" or "قیمت: 50000"
            val statedPrice = extractStatedPrice(raw)

            // Match against catalog
            val matchResult = matchProductInCatalog(cleanedQuery, catalog)
            if (matchResult != null) {
                val matchedProd = matchResult.first
                val score = matchResult.second
                items.add(
                    ParsedItem(
                        productId = matchedProd.id,
                        productName = matchedProd.name,
                        rawText = raw,
                        sku = matchedProd.sku,
                        quantity = quantity.coerceAtLeast(1),
                        unit = matchedProd.unit,
                        unitLabel = matchedProd.unit,
                        catalogPrice = matchedProd.price,
                        statedPrice = statedPrice,
                        discountPercent = 0.0,
                        isMatched = true,
                        confidence = score
                    )
                )
            } else {
                items.add(
                    ParsedItem(
                        productId = null,
                        productName = cleanedQuery,
                        rawText = raw,
                        sku = null,
                        quantity = quantity.coerceAtLeast(1),
                        unit = "piece",
                        unitLabel = unitWord,
                        catalogPrice = 0.0,
                        statedPrice = statedPrice,
                        discountPercent = 0.0,
                        isMatched = false,
                        confidence = 0f,
                        ambiguityReason = "کالایی با این عنوان در انبار یافت نشد"
                    )
                )
            }
        }

        return items
    }

    private fun extractStatedPrice(raw: String): Double? {
        val priceRegex = Regex("(?:قیمت|مبلغ|فی|price)\\s*[:：\\-]?\\s*(\\d+)", RegexOption.IGNORE_CASE)
        val match = priceRegex.find(raw)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun cleanProductQuery(raw: String): String {
        var query = raw.trim()
        query = query.replace(Regex("^(?:کالا|محصول|item|product)\\s*[:：\\-]\\s*", RegexOption.IGNORE_CASE), "")
        query = query.replace(Regex("(?:سلام\\s+)?(?:من\\s+)?[\\p{L}\\s]{2,30}?\\s+(?:هستم|هستتم)"), "")
        query = query.replace(Regex("^(?:سلام|درود|لطفا|لطفاً|ممنون|ببخشید)\\s+", RegexOption.IGNORE_CASE), "")
        query = query.replace(Regex("\\s+(?:میخوام|می‌خوام|میخواستم|می‌خواستم|بفرستید|ارسال|کنید|لطفا|لطفاً|ممنون|تشکر|لازم دارم|نیاز دارم|ثبت کنید)$", RegexOption.IGNORE_CASE), "")
        return query.trim()
    }

    private fun matchProductInCatalog(query: String, catalog: List<Product>): Pair<Product, Float>? {
        if (catalog.isEmpty() || query.isBlank()) return null
        val normQuery = query.lowercase().replace(" ", "")

        // 1. Exact match by name
        val exactName = catalog.find { it.name.trim().equals(query, ignoreCase = true) }
        if (exactName != null) return exactName to 1.0f

        // 2. Exact match by SKU
        val exactSku = catalog.find { it.sku.trim().equals(query, ignoreCase = true) }
        if (exactSku != null) return exactSku to 0.95f

        // 3. Space-stripped match
        val spaceStripped = catalog.find { it.name.lowercase().replace(" ", "") == normQuery }
        if (spaceStripped != null) return spaceStripped to 0.90f

        // 4. Starts with or contains
        val startsWith = catalog.find {
            val normName = it.name.lowercase().replace(" ", "")
            normName.startsWith(normQuery) || normQuery.startsWith(normName)
        }
        if (startsWith != null) return startsWith to 0.80f

        val contains = catalog.find {
            val normName = it.name.lowercase().replace(" ", "")
            normName.contains(normQuery) || normQuery.contains(normName)
        }
        if (contains != null) return contains to 0.70f

        return null
    }
}
