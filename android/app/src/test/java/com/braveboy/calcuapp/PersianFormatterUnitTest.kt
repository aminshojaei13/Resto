package com.braveboy.calcuapp

import com.braveboy.calcuapp.util.PersianFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class PersianFormatterUnitTest {

    @Test
    fun toPersianDigits_convertsEnglishDigitsToPersian() {
        val englishStr = "Order #12345 - Total: $67890.00"
        val persianStr = PersianFormatter.toPersianDigits(englishStr)
        assertEquals("Order #۱۲۳۴۵ - Total: $۶۷۸۹۰.۰۰", persianStr)
    }

    @Test
    fun formatCurrency_formatsTomanCorrectly() {
        val amount = 12500000.0
        val formatted = PersianFormatter.formatCurrency(amount, "تومان", isPersian = true)
        assertEquals("۱۲,۵۰۰,۰۰۰ تومان", formatted)
    }

    @Test
    fun formatShamsiDate_convertsGregorianToPersianSolarHijri() {
        // February 23, 2025 corresponds to 5 Esfand 1403 (1403/12/05)
        val timestamp = 1740312000000L // Feb 23, 2025
        val shamsiStr = PersianFormatter.formatShamsiDate(timestamp, isPersian = true)
        // Should contain Persian year "۱۴۰۳"
        org.junit.Assert.assertTrue(shamsiStr.contains("۱۴۰۳"))
    }
}
