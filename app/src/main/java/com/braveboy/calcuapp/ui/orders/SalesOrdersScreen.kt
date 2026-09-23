package com.braveboy.calcuapp.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.model.FulfillmentStatus
import com.braveboy.calcuapp.data.model.PaymentStatus
import com.braveboy.calcuapp.data.model.SalesOrder
import com.braveboy.calcuapp.ui.pos.ReceiptDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesOrdersScreen(
    viewModel: SalesOrdersViewModel,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPaymentStatus by viewModel.selectedPaymentStatus.collectAsState()
    val selectedFulfillmentStatus by viewModel.selectedFulfillmentStatus.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val selectedOrderForDetail by viewModel.selectedOrderForDetail.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sales Orders & Receipts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Org: ${tenantState.activeOrgId} | Store: ${tenantState.activeStoreId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by Order # or Customer Name...") },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedPaymentStatus == null && selectedFulfillmentStatus == null,
                        onClick = {
                            viewModel.filterPaymentStatus(null)
                            viewModel.filterFulfillmentStatus(null)
                        },
                        label = { Text("All Orders") }
                    )
                }

                items(PaymentStatus.values().toList()) { status ->
                    FilterChip(
                        selected = selectedPaymentStatus == status,
                        onClick = { viewModel.filterPaymentStatus(status) },
                        label = { Text("Pay: ${status.displayName}") }
                    )
                }

                items(FulfillmentStatus.values().toList()) { status ->
                    FilterChip(
                        selected = selectedFulfillmentStatus == status,
                        onClick = { viewModel.filterFulfillmentStatus(status) },
                        label = { Text("Fulfill: ${status.displayName}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sales orders match your criteria.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(orders, key = { it.id }) { order ->
                        SalesOrderCard(
                            order = order,
                            onClick = { viewModel.selectOrderForDetail(order) }
                        )
                    }
                }
            }
        }
    }

    // Selected Order Detail Modal (Receipt)
    selectedOrderForDetail?.let { order ->
        ReceiptDialog(
            order = order,
            onDismiss = { viewModel.selectOrderForDetail(null) }
        )
    }
}

@Composable
fun SalesOrderCard(
    order: SalesOrder,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(order.createdAt))

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "$${String.format("%.2f", order.totalAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Customer: ${order.customerName} • ${order.items.sumOf { it.quantity }} items",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (order.paymentStatus) {
                                PaymentStatus.PAID -> MaterialTheme.colorScheme.primaryContainer
                                PaymentStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.errorContainer
                            },
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Pay: ${order.paymentStatus.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (order.paymentStatus) {
                            PaymentStatus.PAID -> MaterialTheme.colorScheme.onPrimaryContainer
                            PaymentStatus.PENDING -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }

                // Fulfillment Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (order.fulfillmentStatus) {
                                FulfillmentStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                                FulfillmentStatus.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Fulfill: ${order.fulfillmentStatus.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Text(
                    text = "Via ${order.paymentMethod.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
