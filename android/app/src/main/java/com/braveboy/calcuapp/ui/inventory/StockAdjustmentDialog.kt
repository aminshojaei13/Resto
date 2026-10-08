package com.braveboy.calcuapp.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.Warehouse
import com.braveboy.calcuapp.ui.components.RestoBadge
import com.braveboy.calcuapp.ui.components.RestoBadgeVariant
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoDialog
import com.braveboy.calcuapp.ui.components.RestoTextField
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentDialog(
    product: Product,
    warehouses: List<Warehouse>,
    activeWarehouseId: String,
    onSubmitAdjustment: (productId: String, warehouseId: String, delta: Int, reason: String) -> Unit,
    onDismiss: () -> Unit,
    isPersian: Boolean = true
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

    val reasons = if (isPersian) {
        listOf(
            "خرید و ورود کالا به انبار",
            "اصلاح موجودی بر اساس انبارگردانی",
            "ضایعات، شکستگی و آسیب‌دیدگی",
            "مرجوعی از سمت مشتری",
            "انتقال داخلی بین انبارها"
        )
    } else {
        listOf(
            "Restock / Purchase Order Receive",
            "Audit / Cycle Count Correction",
            "Damage / Breakage Loss",
            "Customer Return",
            "Store Transfer"
        )
    }

    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }

    val qtyValue = quantityText.toIntOrNull() ?: 0
    val delta = if (isAddingStock) qtyValue else -qtyValue

    RestoDialog(
        title = if (isPersian) "تغییر موجودی انبار" else "Adjust Stock Level",
        confirmText = if (isPersian) "ثبت تغییر موجودی" else "Save Adjustment",
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
                        text = "${if (isPersian) "کد کالا:" else "SKU:"} ${product.sku}  •  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val stockText = if (isPersian)
                        "موجودی کل: ${PersianFormatter.toPersianDigits(product.getTotalStock())} ${product.unit}"
                    else
                        "Total Stock: ${product.getTotalStock()} ${product.unit}"

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
                    text = if (isPersian) "هنوز انباری برای این شعبه ثبت نشده است" else "No warehouses available",
                    variant = RestoBadgeVariant.Warning
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = warehouseDropdownExpanded,
                    onExpandedChange = { warehouseDropdownExpanded = it }
                ) {
                    val warehouseLabel = selectedWarehouse?.let {
                        val whStock = product.getStockForWarehouse(it.id)
                        if (isPersian)
                            "${it.name} (موجودی فعلی: ${PersianFormatter.toPersianDigits(whStock)} ${product.unit})"
                        else
                            "${it.name} (Stock: $whStock ${product.unit})"
                    } ?: if (isPersian) "انتخاب انبار..." else "Select Warehouse..."

                    OutlinedTextField(
                        value = warehouseLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isPersian) "انبار مقصد" else "Target Warehouse") },
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
                            val itemLabel = if (isPersian)
                                "${wh.name} (موجودی فعلی: ${PersianFormatter.toPersianDigits(currentStock)} ${product.unit})"
                            else
                                "${wh.name} (Current: $currentStock ${product.unit})"

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

            // Inflow vs Outflow Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoButton(
                    text = if (isPersian) "+ ورود / افزایش موجودی" else "+ Add Stock",
                    onClick = { isAddingStock = true },
                    variant = if (isAddingStock) RestoButtonVariant.Primary else RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )

                RestoButton(
                    text = if (isPersian) "- خروج / کسر موجودی" else "- Reduce Stock",
                    onClick = { isAddingStock = false },
                    variant = if (!isAddingStock) RestoButtonVariant.Destructive else RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quantity Input with Unit
            RestoTextField(
                value = quantityText,
                onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                label = if (isPersian) "تعداد تغییر (${product.unit})" else "Quantity Delta (${product.unit})",
                placeholder = "1",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                    label = { Text(if (isPersian) "علت تغییر موجودی" else "Reason") },
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
                    reasons.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r) },
                            onClick = {
                                selectedReason = r
                                reasonDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
