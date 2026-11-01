package com.braveboy.resto.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale

object PersianFormatter {

    private val englishToPersianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(englishToPersianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Number): String {
        return toPersianDigits(number.toString())
    }

    fun formatNumber(amount: Double, isPersian: Boolean = true): String {
        val formatter = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
        val formatted = formatter.format(amount)
        return if (isPersian) toPersianDigits(formatted) else formatted
    }

    fun formatTomans(amount: Double): String {
        return formatNumber(amount, isPersian = true)
    }

    fun formatCurrency(amount: Double, currencySymbol: String = "تومان", isPersian: Boolean = true): String {
        val formattedNumber = formatNumber(amount, isPersian)
        return if (isPersian) "$formattedNumber $currencySymbol" else "$currencySymbol$formattedNumber"
    }

    /**
     * Formats raw numeric text entry into 3-digit comma separated price string (e.g. "1,000,000" or "۱,۰۰۰,۰۰۰").
     */
    fun formatPriceInput(input: String, isPersian: Boolean = true): String {
        val cleanDigits = input.filter { it.isDigit() || it in '۰'..'۹' }
        if (cleanDigits.isEmpty()) return ""
        val englishDigits = cleanDigits.map { ch ->
            if (ch in '۰'..'۹') '0' + (ch - '۰') else ch
        }.joinToString("")
        val number = englishDigits.toLongOrNull() ?: return ""
        return formatNumber(number.toDouble(), isPersian)
    }

    /**
     * Parses formatted 3-digit comma separated price string back to Double.
     */
    fun parsePrice(input: String): Double {
        val cleanDigits = input.filter { it.isDigit() || it in '۰'..'۹' }
        if (cleanDigits.isEmpty()) return 0.0
        val englishDigits = cleanDigits.map { ch ->
            if (ch in '۰'..'۹') '0' + (ch - '۰') else ch
        }.joinToString("")
        return englishDigits.toDoubleOrNull() ?: 0.0
    }

    /**
     * Converts Epoch timestamp to Solar Hijri (Shamsi / Jalali) Date string.
     */
    fun formatShamsiDate(timestamp: Long, isPersian: Boolean = true): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        val (sYear, sMonth, sDay) = gregorianToJalali(gYear, gMonth, gDay)
        val monthNames = arrayOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )

        val dateStr = if (isPersian) {
            "$sDay ${monthNames[sMonth - 1]} $sYear"
        } else {
            "$sYear/$sMonth/$sDay"
        }

        return if (isPersian) toPersianDigits(dateStr) else dateStr
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += ((days - 1) / 365)
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
        val jd = 1 + if (days < 186) (days % 31) else ((days - 186) % 30)
        return Triple(jy, jm, jd)
    }
}

/**
 * VisualTransformation that formats raw digits (e.g. "1000000") with 3-digit thousands separators
 * (e.g. "۱,۰۰۰,۰۰۰" or "1,000,000") for text fields without altering raw numeric state value or cursor position.
 */
class ThousandsSeparatorVisualTransformation(
    private val isPersian: Boolean = true
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val cleanDigits = originalText.filter { it.isDigit() || it in '۰'..'۹' }
        if (cleanDigits.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val englishDigits = cleanDigits.map { ch ->
            if (ch in '۰'..'۹') '0' + (ch - '۰') else ch
        }.joinToString("")

        val number = englishDigits.toLongOrNull() ?: return TransformedText(text, OffsetMapping.Identity)
        val formattedText = PersianFormatter.formatNumber(number.toDouble(), isPersian)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, originalText.length)
                if (clampedOffset == 0) return 0

                val rawSub = originalText.substring(0, clampedOffset).filter { it.isDigit() || it in '۰'..'۹' }
                if (rawSub.isEmpty()) return 0

                val subEnglish = rawSub.map { ch -> if (ch in '۰'..'۹') '0' + (ch - '۰') else ch }.joinToString("")
                val subNum = subEnglish.toLongOrNull() ?: return clampedOffset
                val subFormatted = PersianFormatter.formatNumber(subNum.toDouble(), isPersian)

                return subFormatted.length.coerceAtMost(formattedText.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, formattedText.length)
                if (clampedOffset == 0) return 0

                val formattedSub = formattedText.substring(0, clampedOffset)
                val digitsCount = formattedSub.count { it.isDigit() || it in '۰'..'۹' }

                return digitsCount.coerceAtMost(originalText.length)
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
