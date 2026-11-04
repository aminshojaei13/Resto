package com.braveboy.resto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.util.PersianFormatter.formatTomans
import com.braveboy.resto.util.PersianFormatter.toPersianDigits

@Composable
fun RestoProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    currencySymbol: String = stringResource(R.string.currency_unit),
    warehouseId: String = "",
    onAddToCart: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val stock = if (warehouseId.isNotBlank()) product.getStockForWarehouse(warehouseId) else product.getTotalStock()
    val isOutOfStock = stock <= 0
    val isLowStock = stock in 1..5

    val stockBadgeVariant = when {
        isOutOfStock -> RestoBadgeVariant.Error
        isLowStock -> RestoBadgeVariant.Warning
        else -> RestoBadgeVariant.Success
    }

    val stockBadgeText = when {
        isOutOfStock -> stringResource(R.string.stock_out_of_stock)
        isLowStock -> stringResource(R.string.stock_low_label, toPersianDigits(stock))
        else -> "${toPersianDigits(stock)} ${product.unit}"
    }

    val formattedPrice = "${formatTomans(product.price)} $currencySymbol"

    RestoCard(
        modifier = modifier,
        variant = RestoCardVariant.Filled,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RestoShapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(RestoSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.sku,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                    RestoBadge(text = stockBadgeText, variant = stockBadgeVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (onAddToCart != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    IconButton(
                        onClick = onAddToCart,
                        enabled = !isOutOfStock,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddShoppingCart,
                            contentDescription = stringResource(R.string.add_to_cart),
                            tint = if (!isOutOfStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}
