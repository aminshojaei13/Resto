package com.braveboy.resto.ui.pos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.braveboy.resto.R
import com.braveboy.resto.data.model.SalesOrder
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter.formatShamsiDate
import com.braveboy.resto.util.PersianFormatter.formatTomans
import com.braveboy.resto.util.PersianFormatter.toPersianDigits

@Composable
fun ReceiptDialog(
    order: SalesOrder,
    onDismiss: () -> Unit,
    customerPhone: String = "",
    storeName: String = "فروشگاه",
    storePhone: String = "۰۲۱-۸۸۸۸۹۹۹۹",
    currencyUnit: String = stringResource(R.string.currency_unit)
) {
    val dateStr = formatShamsiDate(order.createdAt, isPersian = true)
    val context = LocalContext.current

    val shareText = remember(order, currencyUnit) {
        val sb = StringBuilder()
        sb.appendLine("🧾 فاکتور رسمی Resto")
        sb.appendLine("شماره فاکتور: ${order.orderNumber}")
        sb.appendLine("تاریخ: $dateStr")
        sb.appendLine("مشتری: ${order.customerName}")
        sb.appendLine("-------------------------")
        order.items.forEach { item ->
            sb.appendLine("• ${item.productName} (${toPersianDigits(item.quantity)} عدد): ${formatTomans(item.totalPrice)} $currencyUnit")
        }
        sb.appendLine("-------------------------")
        if (order.discountAmount > 0) {
            sb.appendLine("تخفیف: -${formatTomans(order.discountAmount)} $currencyUnit")
        }
        if (order.taxAmount > 0) {
            sb.appendLine("مالیات: ${formatTomans(order.taxAmount)} $currencyUnit")
        }
        sb.appendLine("مبلغ پرداخت شده: ${formatTomans(order.totalAmount)} $currencyUnit")
        sb.appendLine("\nبا تشکر از خرید و همراهی شما!")
        sb.toString()
    }

    fun shareReceipt() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(sendIntent, "اشتراک‌گذاری فاکتور سفارش")
        context.startActivity(chooser)
    }

    fun sendSmsToCustomer() {
        val targetPhone = customerPhone.ifBlank { "" }
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$targetPhone")
            putExtra("sms_body", shareText)
        }
        try {
            context.startActivity(smsIntent)
        } catch (_: Exception) {
            shareReceipt()
        }
    }

    fun printReceiptHtml() {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Resto_Receipt_${order.orderNumber}"

        val itemsHtml = StringBuilder()
        order.items.forEach { item ->
            itemsHtml.append("""
                <tr>
                    <td style="text-align: right; padding: 4px 0;">${item.productName}</td>
                    <td style="text-align: center; padding: 4px 0;">${toPersianDigits(item.quantity)}</td>
                    <td style="text-align: left; padding: 4px 0;">${formatTomans(item.totalPrice)} $currencyUnit</td>
                </tr>
            """.trimIndent())
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html dir="rtl">
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: sans-serif;
                        margin: 0;
                        padding: 12px;
                        color: #000;
                    }
                    .title {
                        font-size: 18px;
                        font-weight: bold;
                        text-align: center;
                        margin-bottom: 4px;
                    }
                    .subtitle {
                        font-size: 12px;
                        text-align: center;
                        color: #555;
                        margin-bottom: 12px;
                    }
                    .divider {
                        border-bottom: 1px dashed #000;
                        margin: 8px 0;
                    }
                    .info-row {
                        font-size: 12px;
                        margin-bottom: 4px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 12px;
                        margin: 8px 0;
                    }
                    th {
                        border-bottom: 1px solid #000;
                        padding: 4px 0;
                    }
                    .total-row {
                        font-size: 14px;
                        font-weight: bold;
                        margin-top: 8px;
                    }
                    .footer {
                        text-align: center;
                        font-size: 11px;
                        margin-top: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="title">فاکتور رسمی فروشگاه</div>
                <div class="subtitle">شماره فاکتور: ${order.orderNumber}</div>
                <div class="info-row">تاریخ: $dateStr</div>
                <div class="info-row">مشتری: ${order.customerName}</div>
                ${if (customerPhone.isNotBlank()) "<div class=\"info-row\">تلفن: ${toPersianDigits(customerPhone)}</div>" else ""}
                <div class="divider"></div>
                <table>
                    <thead>
                        <tr>
                            <th style="text-align: right;">کالا</th>
                            <th style="text-align: center;">تعداد</th>
                            <th style="text-align: left;">مبلغ کل</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>
                <div class="divider"></div>
                <div class="info-row" style="display: flex; justify-content: space-between;">
                    <span>جمع کل: ${formatTomans(order.subtotal)} $currencyUnit</span>
                </div>
                ${if (order.discountAmount > 0) "<div class=\"info-row\">تخفیف: -${formatTomans(order.discountAmount)} $currencyUnit</div>" else ""}
                ${if (order.taxAmount > 0) "<div class=\"info-row\">مالیات: ${formatTomans(order.taxAmount)} $currencyUnit</div>" else ""}
                <div class="total-row" style="display: flex; justify-content: space-between;">
                    <span>مبلغ پرداختی: ${formatTomans(order.totalAmount)} $currencyUnit</span>
                </div>
                <div class="divider"></div>
                <div class="footer">با تشکر از خرید و همراهی شما!</div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Success Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = RestoTheme.colors.success,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = stringResource(R.string.payment_successful),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RestoTheme.colors.success,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thermal Receipt Paper View
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Store Info Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = storeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = "پشتیبانی: $storePhone",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Invoice Metadata Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "فاکتور: ${order.orderNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "تاریخ: $dateStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "مشتری: ${order.customerName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                                if (customerPhone.isNotBlank()) {
                                    Text(
                                        text = "تلفن: ${toPersianDigits(customerPhone)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Order Items Table Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "شرح کالا",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(2f)
                            )
                            Text(
                                text = "تعداد",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "قیمت کل",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Order Items List
                        order.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "فی: ${formatTomans(item.unitPrice)} $currencyUnit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = "${toPersianDigits(item.quantity)} عدد",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${formatTomans(item.totalPrice)} $currencyUnit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(1.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Financial Summary Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("جمع کل فاکتور:", style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false)
                            Text("${formatTomans(order.subtotal)} $currencyUnit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }

                        if (order.discountAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تخفیف:", style = MaterialTheme.typography.bodyMedium, color = RestoTheme.colors.success, maxLines = 1, softWrap = false)
                                Text("-${formatTomans(order.discountAmount)} $currencyUnit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = RestoTheme.colors.success, maxLines = 1, softWrap = false)
                            }
                        }

                        if (order.taxAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("مالیات بر ارزش افزوده:", style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false)
                                Text("${formatTomans(order.taxAmount)} $currencyUnit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Final Total Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مبلغ کل پرداختی:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${formatTomans(order.totalAmount)} $currencyUnit",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // QR Code & Thank You Footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "با تشکر از خرید و همراهی شما\nResto OS • سیستم هوشمند فروش",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons (SMS, Share, Print, New Sale)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { sendSmsToCustomer() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پیامک", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    OutlinedButton(
                        onClick = { shareReceipt() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اشتراک", fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }

                    OutlinedButton(
                        onClick = { printReceiptHtml() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.print), fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.new_sale), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
