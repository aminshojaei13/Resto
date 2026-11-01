package com.braveboy.resto.ui.dashboard.components

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
import androidx.compose.material3.MaterialTheme
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
import com.braveboy.resto.data.model.LedgerCategory
import com.braveboy.resto.data.model.LedgerEntry
import com.braveboy.resto.data.model.LedgerType
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter
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
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isPersian) "ثبت درآمدهای فروش و هزینه‌های عملیاتی" else "Sales income & operational expenses",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Pill Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
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
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPersian) "ثبت سند" else "New Entry",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
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
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
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
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = if (isPersian) "هنوز تراکنشی ثبت نشده است" else "No transactions recorded yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isPersian)
                            "پس از انجام اولین فروش یا ثبت هزینه، تاریخچه تراکنش‌ها در اینجا نمایش داده می‌شود."
                        else
                            "When you make your first sale or record an expense, activity will appear here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onNewEntryClick() }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isPersian) "+ ثبت اولین سند" else "+ Record First Entry",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
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
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
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
                    .background(if (isCredit) RestoTheme.colors.successContainer else MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCredit) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = if (isCredit) RestoTheme.colors.success else MaterialTheme.colorScheme.error,
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
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Category Pill
                    val badgeBg = if (isCredit) RestoTheme.colors.successContainer else MaterialTheme.colorScheme.errorContainer
                    val badgeTextClr = if (isCredit) RestoTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formattedAmount,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCredit) RestoTheme.colors.success else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardFinancialActivityPreview() {
    RestoTheme {
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
