package com.braveboy.calcuapp.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.braveboy.calcuapp.data.model.Product
import com.braveboy.calcuapp.data.model.Warehouse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentDialog(
    product: Product,
    warehouses: List<Warehouse>,
    activeWarehouseId: String,
    onSubmitAdjustment: (productId: String, warehouseId: String, delta: Int, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedWarehouse by remember {
        mutableStateOf(warehouses.find { it.id == activeWarehouseId } ?: warehouses.firstOrNull())
    }
    var warehouseDropdownExpanded by remember { mutableStateOf(false) }

    var isAddingStock by remember { mutableStateOf(true) }
    var quantityText by remember { mutableStateOf("5") }

    val reasons = listOf(
        "Restock / Purchase Order Receive",
        "Damage / Breakage Loss",
        "Audit / Cycle Count Correction",
        "Customer Return",
        "Store Transfer"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }

    val qtyValue = quantityText.toIntOrNull() ?: 0
    val delta = if (isAddingStock) qtyValue else -qtyValue

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adjust Stock Level",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "SKU: ${product.sku} | Total Stock: ${product.getTotalStock()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Select Warehouse Dropdown
                ExposedDropdownMenuBox(
                    expanded = warehouseDropdownExpanded,
                    onExpandedChange = { warehouseDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedWarehouse?.name ?: "Select Warehouse",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Warehouse") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = warehouseDropdownExpanded,
                        onDismissRequest = { warehouseDropdownExpanded = false }
                    ) {
                        warehouses.forEach { wh ->
                            DropdownMenuItem(
                                text = { Text("${wh.name} (Current: ${product.getStockForWarehouse(wh.id)})") },
                                onClick = {
                                    selectedWarehouse = wh
                                    warehouseDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add vs Deduct Segmented Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isAddingStock = true },
                        modifier = Modifier.weight(1f),
                        colors = if (isAddingStock) androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("+ Add Stock")
                    }

                    Button(
                        onClick = { isAddingStock = false },
                        modifier = Modifier.weight(1f),
                        colors = if (!isAddingStock) androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ) else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("- Reduce Stock")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity Input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity Delta") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reason Dropdown
                ExposedDropdownMenuBox(
                    expanded = reasonDropdownExpanded,
                    onExpandedChange = { reasonDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedReason,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Adjustment Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
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

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val whId = selectedWarehouse?.id ?: return@Button
                            if (qtyValue > 0) {
                                onSubmitAdjustment(product.id, whId, delta, selectedReason)
                            }
                        },
                        enabled = selectedWarehouse != null && qtyValue > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Adjustment")
                    }
                }
            }
        }
    }
}
