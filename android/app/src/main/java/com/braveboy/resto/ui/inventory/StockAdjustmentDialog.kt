package com.braveboy.resto.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Product
import com.braveboy.resto.data.model.Warehouse
import com.braveboy.resto.ui.components.RestoBadge
import com.braveboy.resto.ui.components.RestoBadgeVariant
import com.braveboy.resto.ui.components.RestoButton
import com.braveboy.resto.ui.components.RestoButtonVariant
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentDialog(
    product: Product,
    warehouses: List<Warehouse>,
    activeWarehouseId: String,
    onSubmitAdjustment: (productId: String, warehouseId: String, delta: Int, reason: String) -> Unit,
    onDismiss: () -> Unit,
    confirmLoading: Boolean = false
) {
    // If only one warehouse exists or active warehouse matches, auto pre-select
    var selectedWarehouse by remember {
        mutableStateOf(
            warehouses.find { it.id == activeWarehouseId }
                ?: warehouses.firstOrNull()
        )
    }
    var warehouseDropdownExpanded by remember { mutableStateOf(false) }

    var isAddingStock by remember { mutableStateOf(true) }
    var quantityText by remember { mutableStateOf("1") }

    val reasons = listOf(
        "خرید و ورود کالا به انبار",
        "اصلاح موجودی بر اساس انبارگردانی",
        "ضایعات، شکستگی و آسیب‌دیدگی",
        "مرجوعی از سمت مشتری",
        "انتقال داخلی بین انبارها"
    )

    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }

    val qtyValue = quantityText.toIntOrNull() ?: 0
    val delta = if (isAddingStock) qtyValue else -qtyValue

    RestoDialog(
        title = stringResource(R.string.adjust_stock_title),
        confirmText = stringResource(R.string.save_stock_adjustment),
        confirmLoading = confirmLoading,
        onConfirm = {
            val whId = selectedWarehouse?.id ?: return@RestoDialog
            if (qtyValue > 0) {
                onSubmitAdjustment(product.id, whId, delta, selectedReason)
            }
        },
        confirmEnabled = selectedWarehouse != null && qtyValue > 0,
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)) {
            // Product summary
            Column {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xxs))
                Row {
                    Text(
                        text = "${stringResource(R.string.stock_code)} ${product.sku}  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val stockText = "موجودی کل: ${PersianFormatter.toPersianDigits(product.getTotalStock())} ${product.unit}"

                    Text(
                        text = stockText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Warehouse Selector
            if (warehouses.isEmpty()) {
                RestoBadge(
                    text = stringResource(R.string.no_warehouses_registered),
                    variant = RestoBadgeVariant.Warning
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = warehouseDropdownExpanded,
                    onExpandedChange = { warehouseDropdownExpanded = it }
                ) {
                    val warehouseLabel = selectedWarehouse?.let {
                        val whStock = product.getStockForWarehouse(it.id)
                        "${it.name} (موجودی فعلی: ${PersianFormatter.toPersianDigits(whStock)} ${product.unit})"
                    } ?: stringResource(R.string.select_warehouse_prompt)

                    OutlinedTextField(
                        value = warehouseLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.target_warehouse)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseDropdownExpanded) },
                        shape = RestoShapes.medium,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = warehouseDropdownExpanded,
                        onDismissRequest = { warehouseDropdownExpanded = false }
                    ) {
                        warehouses.forEach { wh ->
                            val currentStock = product.getStockForWarehouse(wh.id)
                            val itemLabel = "${wh.name} (موجودی فعلی: ${PersianFormatter.toPersianDigits(currentStock)} ${product.unit})"

                            DropdownMenuItem(
                                text = { Text(itemLabel) },
                                onClick = {
                                    selectedWarehouse = wh
                                    warehouseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Delta Direction Toggle (+ Add / - Reduce)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoButton(
                    text = stringResource(R.string.add_stock),
                    onClick = { isAddingStock = true },
                    variant = if (isAddingStock) RestoButtonVariant.Primary else RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )

                RestoButton(
                    text = stringResource(R.string.reduce_stock),
                    onClick = { isAddingStock = false },
                    variant = if (!isAddingStock) RestoButtonVariant.Destructive else RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quantity Input
            RestoTextField(
                value = quantityText,
                onValueChange = { input -> quantityText = input.filter { it.isDigit() } },
                label = stringResource(R.string.quantity_delta),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
            )

            // Reason Selector
            ExposedDropdownMenuBox(
                expanded = reasonDropdownExpanded,
                onExpandedChange = { reasonDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedReason,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.adjustment_reason)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded) },
                    shape = RestoShapes.medium,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = reasonDropdownExpanded,
                    onDismissRequest = { reasonDropdownExpanded = false }
                ) {
                    reasons.forEach { reason ->
                        DropdownMenuItem(
                            text = { Text(reason) },
                            onClick = {
                                selectedReason = reason
                                reasonDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
