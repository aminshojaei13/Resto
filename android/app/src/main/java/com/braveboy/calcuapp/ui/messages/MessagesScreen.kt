package com.braveboy.calcuapp.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel,
    modifier: Modifier = Modifier,
    initialSharedText: String? = null
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val rawText by viewModel.rawText.collectAsState()
    val parsedPayload by viewModel.parsedPayload.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialSharedText) {
        if (!initialSharedText.isNullOrBlank()) {
            viewModel.setRawText(initialSharedText)
            viewModel.parseMessage(initialSharedText, source = "android_share")
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    val sampleTemplate = "RESTO_ORDER\nCustomer: Ali Rezaei\nPhone: +1 (555) 888-9999\nSKU: APX-LAP-001\nQuantity: 2\nAddress: Tehran, Freedom Square\nPayment: Cash"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Social Message Import",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Org: ${tenantState.activeOrgId}",
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paste Received Order Text",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = { viewModel.setRawText(sampleTemplate) }
                        ) {
                            Icon(imageVector = Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sample", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { viewModel.setRawText(it) },
                        placeholder = { Text("Paste message text from Instagram, Telegram, or WhatsApp...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        maxLines = 6
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.parseMessage(rawText, "manual_paste") },
                        enabled = !isParsing && rawText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isParsing) "Parsing Message..." else "Parse Order Message")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Parsed Preview
            parsedPayload?.let { payload ->
                val customerMap = payload["customer"] as? Map<*, *>
                val itemsList = payload["items"] as? List<*>
                val grandTotal = (payload["grand_total"] as? Number)?.toDouble() ?: 0.0

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parsed Order Preview",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Customer: ${customerMap?.get("name") ?: "Social Customer"} (${customerMap?.get("phone") ?: "No phone"})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "Address: ${customerMap?.get("address") ?: "None"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Items:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)

                        itemsList?.forEach { item ->
                            val map = item as? Map<*, *>
                            if (map != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${map["product_name"]} (x${map["quantity"]})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$${String.format("%.2f", (map["subtotal"] as? Number)?.toDouble() ?: 0.0)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Grand Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$${String.format("%.2f", grandTotal)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val custName = (customerMap?.get("name") as? String) ?: "Social Customer"
                                val custId = customerMap?.get("id") as? String
                                val cartItems = (itemsList ?: emptyList<Any>()).mapNotNull { raw ->
                                    val m = raw as? Map<*, *> ?: return@mapNotNull null
                                    val prodId = (m["product_id"] as? String) ?: ""
                                    if (prodId.isBlank()) return@mapNotNull null
                                    CartItem(
                                        orgId = tenantState.activeOrgId,
                                        storeId = tenantState.activeStoreId,
                                        productId = prodId,
                                        productName = (m["product_name"] as? String) ?: "",
                                        sku = (m["sku"] as? String) ?: "",
                                        barcode = "",
                                        price = (m["price"] as? Number)?.toDouble() ?: 0.0,
                                        quantity = (m["quantity"] as? Number)?.toInt() ?: 1
                                    )
                                }

                                if (cartItems.isNotEmpty()) {
                                    viewModel.checkoutParsedOrder(
                                        customerName = custName,
                                        customerId = custId,
                                        paymentMethod = PaymentMethod.CASH,
                                        items = cartItems
                                    )
                                }
                            },
                            enabled = !(itemsList.isNullOrEmpty()),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Rounded.ShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm & Checkout Order")
                        }
                    }
                }
            }
        }
    }
}
