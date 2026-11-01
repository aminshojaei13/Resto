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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.R
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
                    text = stringResource(R.string.recent_financial_activity),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.sales_and_expenses_sub),
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
                        text = stringResource(R.string.new_entry),
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
                        text = stringResource(R.string.no_transactions_yet),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = stringResource(R.string.no_transactions_desc),
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
                            text = stringResource(R.string.record_first_entry),
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
        LedgerCategory.SALES -> stringResource(R.string.sales_income_cat)
        LedgerCategory.INVENTORY_ADJUSTMENT -> stringResource(R.string.stock_adjustment_cat)
        LedgerCategory.EXPENSE -> stringResource(R.string.operating_expense_cat)
        LedgerCategory.CASH_IN -> stringResource(R.string.cash_in_cat)
        LedgerCategory.CASH_OUT -> stringResource(R.string.cash_out_cat)
        LedgerCategory.REFUND -> stringResource(R.string.customer_refund_cat)
    }

    val badgeBg = if (isCredit) RestoTheme.colors.successContainer else MaterialTheme.colorScheme.errorContainer
    val badgeTextClr = if (isCredit) RestoTheme.colors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Icon + Category Pill + Entry Number <---> Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isCredit) RestoTheme.colors.successContainer else MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCredit) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (isCredit) RestoTheme.colors.success else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = categoryTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextClr,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Text(
                        text = if (isPersian) PersianFormatter.toPersianDigits(entry.entryNumber) else entry.entryNumber,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formattedAmount,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCredit) RestoTheme.colors.success else MaterialTheme.colorScheme.error,
                    maxLines = 1
                )
            }

            // Description & Date
            if (entry.description.isNotBlank()) {
                Text(
                    text = entry.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardFinancialActivityPreview() {
    val sampleEntries = listOf(
        LedgerEntry(
            id = "1",
            orgId = "org_1",
            storeId = "store_1",
            entryNumber = "LEDG-ORD-3206-1001",
            type = LedgerType.CREDIT,
            category = LedgerCategory.SALES,
            amount = 22400000.0,
            description = "Sales Order #ORD-3206-1001 via Cash",
            referenceId = "ord_1",
            createdAt = System.currentTimeMillis()
        ),
        LedgerEntry(
            id = "2",
            orgId = "org_1",
            storeId = "store_1",
            entryNumber = "INIT-833544",
            type = LedgerType.CREDIT,
            category = LedgerCategory.INVENTORY_ADJUSTMENT,
            amount = 85000000.0,
            description = "موجودی اولیه برای کابل vga (100 عدد)",
            referenceId = "prod_2",
            createdAt = System.currentTimeMillis()
        )
    )

    RestoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                DashboardFinancialActivity(
                    ledgerEntries = sampleEntries,
                    isPersian = true,
                    currencyUnit = "تومان",
                    onNewEntryClick = {}
                )
            }
        }
    }
}
