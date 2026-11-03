package com.braveboy.resto.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.CartItem
import com.braveboy.resto.data.model.Customer
import com.braveboy.resto.data.model.SalesChannel
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter.toPersianDigits

@Composable
fun CartPanel(
    modifier: Modifier = Modifier,
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    selectedChannel: SalesChannel = SalesChannel.WALK_IN,
    customGuestName: String = "",
    onUpdateQuantity: (CartItem, Int) -> Unit,
    onApplyDiscount: (String, Double) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onSelectCustomerClick: () -> Unit,
    onCheckoutClick: () -> Unit,
    currencyUnit: String = stringResource(R.string.currency_unit)
) {
    val subtotal = cartItems.sumOf { it.subtotal }
    val discountTotal = cartItems.sumOf { it.discountAmount }
    val taxTotal = cartItems.sumOf { it.taxAmount }
    val grandTotal = cartItems.sumOf { it.total }
    val totalItemCount = cartItems.sumOf { it.quantity }

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.cart_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = toPersianDigits(totalItemCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = onClearCart) {
                        Text(
                            text = stringResource(R.string.clear_all),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer selector chip
            Card(
                onClick = onSelectCustomerClick,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val headerText = when {
                            selectedCustomer != null -> selectedCustomer.name
                            customGuestName.isNotBlank() -> "$customGuestName (${selectedChannel.titleFa})"
                            else -> selectedChannel.titleFa
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedCustomer != null) {
                            Text(
                                text = stringResource(
                                    R.string.customer_points_label,
                                    toPersianDigits(selectedCustomer.loyaltyPoints),
                                    selectedCustomer.phone.ifBlank { selectedCustomer.email }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.select_channel_or_customer_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items List
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.cart_empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.cart_empty_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(cartItems, key = { it.id }) { item ->
                        CartItemRow(
                            item = item,
                            currencyUnit = currencyUnit,
                            onQuantityChange = { delta -> onUpdateQuantity(item, delta) },
                            onRemove = { onRemoveItem(item.id) },
                            onApplyDiscount = { percent -> onApplyDiscount(item.id, percent) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Summary Totals
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.subtotal_items),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${com.braveboy.resto.util.PersianFormatter.formatTomans(subtotal)} $currencyUnit",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (discountTotal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.discount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "-${com.braveboy.resto.util.PersianFormatter.formatTomans(discountTotal)} $currencyUnit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                if (taxTotal > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.tax_plain),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${com.braveboy.resto.util.PersianFormatter.formatTomans(taxTotal)} $currencyUnit",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.grand_total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${com.braveboy.resto.util.PersianFormatter.formatTomans(grandTotal)} $currencyUnit",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checkout Button
            Button(
                onClick = onCheckoutClick,
                enabled = cartItems.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Rounded.ShoppingCartCheckout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.checkout_and_invoice,
                        com.braveboy.resto.util.PersianFormatter.formatTomans(grandTotal),
                        currencyUnit
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    onApplyDiscount: (Double) -> Unit,
    currencyUnit: String = stringResource(R.string.currency_unit)
) {
    Card(
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.variantName != null) {
                        Text(
                            text = item.variantName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "${com.braveboy.resto.util.PersianFormatter.formatTomans(item.price)} $currencyUnit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.remove_item),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    OutlinedIconButton(
                        onClick = { onQuantityChange(-1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = stringResource(R.string.decrease),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = toPersianDigits(item.quantity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    OutlinedIconButton(
                        onClick = { onQuantityChange(1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.increase),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "${com.braveboy.resto.util.PersianFormatter.formatTomans(item.total)} $currencyUnit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CartItemRowPreview() {
    RestoTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CartItemRow(
                item = CartItem(
                    id = "cart-item-1",
                    orgId = "org1",
                    storeId = "store1",
                    productId = "p1",
                    productName = "قهوه اسپرسو دوبل",
                    variantName = "۱۰۰٪ عربیکا",
                    sku = "ESP-101",
                    barcode = "123456789",
                    price = 120000.0,
                    quantity = 2
                ),
                onQuantityChange = {},
                onRemove = {},
                onApplyDiscount = {}
            )
        }
    }
}

@Preview(name = "Cart Panel Preview", showBackground = true, widthDp = 380, heightDp = 720)
@Composable
fun CartPanelPreview() {
    RestoTheme {
        CartPanel(
            cartItems = listOf(
                CartItem(
                    id = "1",
                    orgId = "org1",
                    storeId = "store1",
                    productId = "p1",
                    productName = "پیتزا سیر و استیک",
                    variantName = "دونفره",
                    sku = "PIZ-001",
                    barcode = "123456",
                    price = 320000.0,
                    quantity = 2,
                    discountPercent = 10.0,
                    taxRate = 0.09
                ),
                CartItem(
                    id = "2",
                    orgId = "org1",
                    storeId = "store1",
                    productId = "p2",
                    productName = "نوشابه قوطی کوکاکولا",
                    sku = "DRK-001",
                    barcode = "654321",
                    price = 25000.0,
                    quantity = 3,
                    taxRate = 0.09
                )
            ),
            selectedCustomer = Customer(
                id = "cust1",
                orgId = "org1",
                name = "علی محمدی",
                phone = "09123456789",
                loyaltyPoints = 150
            ),
            selectedChannel = SalesChannel.WALK_IN,
            onUpdateQuantity = { _, _ -> },
            onApplyDiscount = { _, _ -> },
            onRemoveItem = {},
            onClearCart = {},
            onSelectCustomerClick = {},
            onCheckoutClick = {}
        )
    }
}
