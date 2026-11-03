package com.braveboy.resto.ui.pos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.CartItem
import com.braveboy.resto.data.model.Customer
import com.braveboy.resto.data.model.PaymentMethod
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutModal(
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    selectedChannel: SalesChannel = SalesChannel.WALK_IN,
    customGuestName: String = "",
    currencyUnit: String = stringResource(R.string.currency_unit),
    isCheckingOut: Boolean = false,
    onDismiss: () -> Unit,
    onProcessCheckout: (PaymentMethod, String, SalesChannel) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        CheckoutSheetContent(
            cartItems = cartItems,
            selectedCustomer = selectedCustomer,
            initialSelectedChannel = selectedChannel,
            customGuestName = customGuestName,
            currencyUnit = currencyUnit,
            isCheckingOut = isCheckingOut,
            onDismiss = onDismiss,
            onProcessCheckout = onProcessCheckout
        )
    }
}

@Composable
fun CheckoutSheetContent(
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    initialSelectedChannel: SalesChannel = SalesChannel.WALK_IN,
    customGuestName: String = "",
    currencyUnit: String = stringResource(R.string.currency_unit),
    isCheckingOut: Boolean = false,
    onDismiss: () -> Unit,
    onProcessCheckout: (PaymentMethod, String, SalesChannel) -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var selectedChannel by remember { mutableStateOf(initialSelectedChannel) }
    var cashTenderedText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val totalAmount = cartItems.sumOf { it.total }
    val cashTendered = PersianFormatter.parsePrice(cashTenderedText)
    val changeDue = (cashTendered - totalAmount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.checkout_and_payment_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grand Total Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.total_payable_amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${PersianFormatter.formatTomans(totalAmount)} $currencyUnit",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                val customerBannerText = when {
                    selectedCustomer != null -> "مشتری: ${selectedCustomer.name}"
                    customGuestName.isNotBlank() -> "مشتری مهمان: $customGuestName (${selectedChannel.titleFa})"
                    else -> "کانال فروش: ${selectedChannel.titleFa}"
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customerBannerText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sales Channel Selection Row
        Text(
            text = stringResource(R.string.sales_channel_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SalesChannel.entries.size) { idx ->
                val ch = SalesChannel.entries[idx]
                FilterChip(
                    selected = selectedChannel == ch,
                    onClick = { selectedChannel = ch },
                    label = {
                        Text(
                            text = ch.titleFa,
                            fontWeight = if (selectedChannel == ch) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Method Selection Grid
        Text(
            text = stringResource(R.string.payment_method_label_simple),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodGridCard(
                    method = PaymentMethod.CASH,
                    icon = Icons.Rounded.Money,
                    selected = selectedPaymentMethod == PaymentMethod.CASH,
                    onClick = { selectedPaymentMethod = PaymentMethod.CASH },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodGridCard(
                    method = PaymentMethod.CARD,
                    icon = Icons.Rounded.CreditCard,
                    selected = selectedPaymentMethod == PaymentMethod.CARD,
                    onClick = { selectedPaymentMethod = PaymentMethod.CARD },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodGridCard(
                    method = PaymentMethod.MOBILE_PAYMENT,
                    icon = Icons.Rounded.QrCodeScanner,
                    selected = selectedPaymentMethod == PaymentMethod.MOBILE_PAYMENT,
                    onClick = { selectedPaymentMethod = PaymentMethod.MOBILE_PAYMENT },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodGridCard(
                    method = PaymentMethod.STORE_CREDIT,
                    icon = Icons.Rounded.Storefront,
                    selected = selectedPaymentMethod == PaymentMethod.STORE_CREDIT,
                    onClick = { selectedPaymentMethod = PaymentMethod.STORE_CREDIT },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cash math details when CASH selected
        AnimatedVisibility(visible = selectedPaymentMethod == PaymentMethod.CASH) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = cashTenderedText,
                    onValueChange = { cashTenderedText = it.filter { ch -> ch.isDigit() || ch in '۰'..'۹' } },
                    visualTransformation = com.braveboy.resto.util.ThousandsSeparatorVisualTransformation(true),
                    label = { Text(stringResource(R.string.cash_tendered_label, currencyUnit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.change_due_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${PersianFormatter.formatTomans(changeDue)} $currencyUnit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            label = { Text(stringResource(R.string.invoice_notes_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isCheckingOut,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel))
            }

            Button(
                onClick = { onProcessCheckout(selectedPaymentMethod, notesText, selectedChannel) },
                enabled = !isCheckingOut,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isCheckingOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.confirm_and_issue_invoice),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PaymentMethodGridCard(
    method: PaymentMethod,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val methodTitle = when (method) {
        PaymentMethod.CASH -> stringResource(R.string.cash)
        PaymentMethod.CARD -> stringResource(R.string.card)
        PaymentMethod.MOBILE_PAYMENT -> stringResource(R.string.mobile_pay)
        PaymentMethod.STORE_CREDIT -> stringResource(R.string.store_credit)
    }

    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = methodTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(name = "Checkout Bottom Sheet Preview", showBackground = true, widthDp = 380, heightDp = 650)
@Composable
fun CheckoutBottomSheetPreview() {
    RestoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            CheckoutSheetContent(
                cartItems = listOf(
                    CartItem(
                        orgId = "org1",
                        storeId = "store1",
                        productId = "p1",
                        productName = "قهوه اسپرسو دوبل",
                        sku = "ESP-1",
                        barcode = "123",
                        price = 120000.0,
                        quantity = 2
                    ),
                    CartItem(
                        orgId = "org1",
                        storeId = "store1",
                        productId = "p2",
                        productName = "چای لاهیجان",
                        sku = "TEA-1",
                        barcode = "456",
                        price = 85000.0,
                        quantity = 1
                    )
                ),
                selectedCustomer = Customer(
                    id = "c1",
                    orgId = "org1",
                    name = "رضا احمدی",
                    phone = "09121111111",
                    loyaltyPoints = 200
                ),
                initialSelectedChannel = SalesChannel.WALK_IN,
                onDismiss = {},
                onProcessCheckout = { _, _, _ -> }
            )
        }
    }
}
