package com.braveboy.calcuapp.util

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

    fun formatNumber(amount: Double, isPersian: Boolean = true): String {
        val formatter = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))
        val formatted = formatter.format(amount)
        return if (isPersian) toPersianDigits(formatted) else formatted
    }

    fun formatCurrency(amount: Double, currencySymbol: String = "تومان", isPersian: Boolean = true): String {
        val formattedNumber = formatNumber(amount, isPersian)
        return if (isPersian) "$formattedNumber $currencySymbol" else "$currencySymbol$formattedNumber"
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
