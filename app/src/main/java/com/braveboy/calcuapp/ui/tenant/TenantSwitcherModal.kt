package com.braveboy.calcuapp.ui.tenant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Warehouse
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.braveboy.calcuapp.data.model.Organization
import com.braveboy.calcuapp.data.model.Store
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.model.Warehouse
import com.braveboy.calcuapp.data.repository.TenantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TenantSwitcherModal(
    tenantRepository: TenantRepository,
    onDismiss: () -> Unit
) {
    val tenantState by tenantRepository.getTenantState().collectAsState(initial = TenantState())
    val organizations by tenantRepository.getOrganizations().collectAsState(initial = emptyList())
    val activeStores by tenantRepository.getStoresForOrg(tenantState.activeOrgId).collectAsState(initial = emptyList())
    val activeWarehouses by tenantRepository.getWarehousesForStore(tenantState.activeStoreId).collectAsState(initial = emptyList())

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Switch Tenant & Store",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Organization selection
                Text(
                    text = "1. Select Active Organization",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.height(120.dp)) {
                    items(organizations) { org ->
                        val isSelected = org.id == tenantState.activeOrgId
                        Card(
                            onClick = {
                                val firstStore = org.stores.firstOrNull()
                                val firstWh = firstStore?.warehouses?.firstOrNull()
                                CoroutineScope(Dispatchers.IO).launch {
                                    tenantRepository.setActiveTenant(
                                        orgId = org.id,
                                        storeId = firstStore?.id ?: "store_apex_1",
                                        warehouseId = firstWh?.id ?: "wh_apex_1a"
                                    )
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = org.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Store selection
                Text(
                    text = "2. Select Store Branch",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.height(100.dp)) {
                    items(activeStores) { store ->
                        val isSelected = store.id == tenantState.activeStoreId
                        Card(
                            onClick = {
                                val firstWh = store.warehouses.firstOrNull()
                                CoroutineScope(Dispatchers.IO).launch {
                                    tenantRepository.setActiveStore(
                                        storeId = store.id,
                                        warehouseId = firstWh?.id ?: "wh_apex_1a"
                                    )
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Warehouse selection
                Text(
                    text = "3. Select Active Warehouse",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.height(100.dp)) {
                    items(activeWarehouses) { warehouse ->
                        val isSelected = warehouse.id == tenantState.activeWarehouseId
                        Card(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    tenantRepository.setActiveWarehouse(warehouse.id)
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = warehouse.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
