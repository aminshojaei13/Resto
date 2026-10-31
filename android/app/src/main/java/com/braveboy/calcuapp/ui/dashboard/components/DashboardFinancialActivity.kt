package com.braveboy.calcuapp.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.calcuapp.data.model.LedgerCategory
import com.braveboy.calcuapp.data.model.LedgerEntry
import com.braveboy.calcuapp.data.model.LedgerType
import com.braveboy.calcuapp.ui.theme.CalcuappTheme
import com.braveboy.calcuapp.util.PersianFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardFinancialActivity(
    ledgerEntries: List<LedgerEntry>,
    isPersian: Boolean,
    currencyUnit: String,
    onNewEntryClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEntryClick: (LedgerEntry) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersian) "گردش مالی و تراکنش‌های اخیر" else "Recent Financial Activity",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = if (isPersian) "ثبت درآمدهای فروش و هزینه‌های عملیاتی" else "Sales income & operational expenses",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Pill Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .clickable { onNewEntryClick() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPersian) "ثبت سند" else "New Entry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        if (ledgerEntries.isEmpty()) {
            // Dribbble-style Empty State Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = if (isPersian) "هنوز تراکنشی ثبت نشده است" else "No transactions recorded yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isPersian)
                            "پس از انجام اولین فروش یا ثبت هزینه، تاریخچه تراکنش‌ها در اینجا نمایش داده می‌شود."
                        else
                            "When you make your first sale or record an expense, activity will appear here.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .clickable { onNewEntryClick() }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isPersian) "+ ثبت اولین سند" else "+ Record First Entry",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Ledger Entries Card List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ledgerEntries.take(5).forEach { entry ->
                    ModernLedgerCardItem(
                        entry = entry,
                        isPersian = isPersian,
                        currencyUnit = currencyUnit,
                        onClick = { onEntryClick(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun ModernLedgerCardItem(
    entry: LedgerEntry,
    isPersian: Boolean,
    currencyUnit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCredit = entry.type == LedgerType.CREDIT
    val dateFormat = SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault())
    val dateStr = if (isPersian) {
        PersianFormatter.toPersianDigits(dateFormat.format(Date(entry.createdAt)))
    } else {
        dateFormat.format(Date(entry.createdAt))
    }

    val formattedAmount = if (isPersian) {
        "${if (isCredit) "+" else "-"}${PersianFormatter.formatTomans(entry.amount)} $currencyUnit"
    } else {
        "${if (isCredit) "+" else "-"}$${String.format(Locale.getDefault(), "%.2f", entry.amount)}"
    }

    val categoryTitle = when (entry.category) {
        LedgerCategory.SALES -> if (isPersian) "درآمد فروش" else "Sales Income"
        LedgerCategory.INVENTORY_ADJUSTMENT -> if (isPersian) "اصلاح موجودی" else "Stock Adjustment"
        LedgerCategory.EXPENSE -> if (isPersian) "هزینه جاری" else "Operating Expense"
        LedgerCategory.CASH_IN -> if (isPersian) "ورود نقدینگی" else "Cash In"
        LedgerCategory.CASH_OUT -> if (isPersian) "برداشت نقدینگی" else "Cash Out"
        LedgerCategory.REFUND -> if (isPersian) "مرجوعی مشتری" else "Refund"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = if (isCredit) Color(0xFF16A34A) else Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isPersian) PersianFormatter.toPersianDigits(entry.entryNumber) else entry.entryNumber,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Category Pill
                    val badgeBg = if (isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    val badgeTextClr = if (isCredit) Color(0xFF15803D) else Color(0xFFB91C1C)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = categoryTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeTextClr
                        )
                    }
                }

                if (entry.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.description,
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                }

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formattedAmount,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardFinancialActivityPreview() {
    CalcuappTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8FAFC))
                    .padding(16.dp)
            ) {
                DashboardFinancialActivity(
                    ledgerEntries = emptyList(),
                    isPersian = true,
                    currencyUnit = "تومان",
                    onNewEntryClick = {}
                )
            }
        }
    }
}
