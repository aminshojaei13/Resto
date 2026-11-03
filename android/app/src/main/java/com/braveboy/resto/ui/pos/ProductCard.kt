package com.braveboy.resto.ui.pos

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter

@Composable
fun ProductCard(
    product: Product,
    activeWarehouseId: String,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier,
    currencyUnit: String = stringResource(R.string.currency_unit)
) {
    val stockInWarehouse = product.getStockForWarehouse(activeWarehouseId)
    val totalStock = product.getTotalStock()
    val isLowStock = totalStock in 1..9
    val isOutOfStock = totalStock == 0

    val statusContainerColor = when {
        isOutOfStock -> MaterialTheme.colorScheme.errorContainer
        isLowStock -> RestoTheme.colors.warningContainer
        else -> RestoTheme.colors.successContainer
    }

    val statusDotColor = when {
        isOutOfStock -> MaterialTheme.colorScheme.error
        isLowStock -> RestoTheme.colors.warning
        else -> RestoTheme.colors.success
    }

    val statusTextColor = when {
        isOutOfStock -> MaterialTheme.colorScheme.onErrorContainer
        isLowStock -> RestoTheme.colors.onWarningContainer
        else -> RestoTheme.colors.onSuccessContainer
    }

    val statusText = when {
        isOutOfStock -> stringResource(R.string.stock_out_of_stock)
        isLowStock -> stringResource(R.string.stock_low_label, PersianFormatter.toPersianDigits(totalStock))
        else -> stringResource(R.string.stock_available_label, PersianFormatter.toPersianDigits(totalStock))
    }

    Card(
        onClick = { onProductClick(product) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Column(modifier = Modifier.padding(10.dp)) {
                // Image Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Stock Status Pill Overlay (Top Right)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(statusContainerColor)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Text(
                                text = statusText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusTextColor,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "موجودی: ${PersianFormatter.toPersianDigits(stockInWarehouse)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${PersianFormatter.formatTomans(product.price)} $currencyUnit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            // Full-Width Bottom Touch Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isOutOfStock) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primaryContainer)
                    .clickable(enabled = !isOutOfStock) { onProductClick(product) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isOutOfStock) stringResource(R.string.out_of_stock) else stringResource(R.string.add_to_cart),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOutOfStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProductCardPreview() {
    RestoTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ProductCard(
                product = Product(
                    id = "1",
                    orgId = "org1",
                    sku = "PRD-1001",
                    barcode = "123456789",
                    name = "اسپرسو سینگل",
                    description = "قهوه اسپرسو ۱۰۰٪ عربیکا",
                    price = 45000.0,
                    costPrice = 20000.0,
                    category = "نوشیدنی",
                    unit = "عدد",
                    stockQuantityByWarehouse = mapOf("w1" to 15)
                ),
                activeWarehouseId = "w1",
                onProductClick = {}
            )
        }
    }
}
