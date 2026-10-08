package com.braveboy.calcuapp.ui.customer

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.braveboy.calcuapp.data.model.Customer
import com.braveboy.calcuapp.ui.components.RestoBadge
import com.braveboy.calcuapp.ui.components.RestoBadgeVariant
import com.braveboy.calcuapp.ui.components.RestoBottomSheet
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoDialog
import com.braveboy.calcuapp.ui.components.RestoEmptyState
import com.braveboy.calcuapp.ui.components.RestoSearchField
import com.braveboy.calcuapp.ui.components.RestoTextField
import com.braveboy.calcuapp.ui.components.RestoTopBar
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: CustomerViewModel,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val isAddCustomerDialogOpen by viewModel.isAddCustomerDialogOpen.collectAsState()
    val selectedCustomerForDetail by viewModel.selectedCustomerForDetail.collectAsState()
    val selectedCustomerForEdit by viewModel.selectedCustomerForEdit.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val isPersian = tenantState.language == "fa"
    val currencyUnit = if (isPersian) "تومان" else "$"
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
            RestoTopBar(
                title = if (isPersian) "مدیریت مشتریان و حساب‌ها" else "Customer CRM",
                subtitle = if (isPersian) "لیست مشتریان، شماره تماس و تاریخچه خرید" else "Profiles, contact info & purchase history"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddCustomerDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonAdd,
                    contentDescription = if (isPersian) "افزودن مشتری جدید" else "Add Customer"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = RestoSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            RestoSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = if (isPersian) "جستجوی مشتری با نام یا شماره تماس..." else "Search by name, phone, email..."
            )

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            if (customers.isEmpty()) {
                RestoEmptyState(
                    title = if (isPersian) "هیچ مشتری ثبت نشده است" else "No customers found",
                    description = if (isPersian)
                        "با افزودن اولین مشتری، سوابق سفارشات و اطلاعات تماس او ثبت خواهد شد."
                    else
                        "When you add customers or checkout orders, customer profiles will appear here.",
                    actionText = if (isPersian) "+ افزودن مشتری" else "+ Add Customer",
                    onActionClick = { viewModel.openAddCustomerDialog() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        CustomerItemCard(
                            customer = customer,
                            isPersian = isPersian,
                            currencyUnit = currencyUnit,
                            onClick = { viewModel.selectCustomerForDetail(customer) },
                            onEditClick = { viewModel.openEditCustomer(customer) }
                        )
                    }
                }
            }
        }
    }

    // Consolidated Customer Form Dialog (For Add and Edit)
    if (isAddCustomerDialogOpen) {
        CustomerFormDialog(
            title = if (isPersian) "افزودن مشتری جدید" else "Add New Customer",
            initialCustomer = null,
            isPersian = isPersian,
            onSubmit = { name, email, phone, address ->
                viewModel.addCustomer(name, email, phone, address)
            },
            onDismiss = { viewModel.closeAddCustomerDialog() }
        )
    }

    selectedCustomerForEdit?.let { customer ->
        CustomerFormDialog(
            title = if (isPersian) "ویرایش اطلاعات مشتری" else "Edit Customer Profile",
            initialCustomer = customer,
            isPersian = isPersian,
            onSubmit = { name, email, phone, address ->
                viewModel.updateCustomer(
                    customer.copy(name = name, email = email, phone = phone, address = address)
                )
            },
            onDismiss = { viewModel.closeEditCustomer() }
        )
    }

    // Customer Detail Sheet
    selectedCustomerForDetail?.let { customer ->
        CustomerDetailSheet(
            customer = customer,
            isPersian = isPersian,
            currencyUnit = currencyUnit,
            onEditClick = {
                viewModel.selectCustomerForDetail(null)
                viewModel.openEditCustomer(customer)
            },
            onDismiss = { viewModel.selectCustomerForDetail(null) }
        )
    }
}

@Composable
fun CustomerItemCard(
    customer: Customer,
    isPersian: Boolean,
    currencyUnit: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val formattedSpent = if (isPersian) {
        "${PersianFormatter.formatTomans(customer.totalPurchases)} $currencyUnit"
    } else {
        "$${String.format("%.2f", customer.totalPurchases)}"
    }

    RestoCard(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customer.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(RestoSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (customer.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPersian) PersianFormatter.toPersianDigits(customer.phone) else customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedSpent,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isPersian) "مجموع خرید" else "Total spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CustomerFormDialog(
    title: String,
    initialCustomer: Customer?,
    isPersian: Boolean,
    onSubmit: (name: String, email: String, phone: String, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var email by remember { mutableStateOf(initialCustomer?.email ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }

    RestoDialog(
        title = title,
        confirmText = if (isPersian) "ذخیره اطلاعات" else "Save Profile",
        onConfirm = {
            if (name.isNotBlank()) {
                onSubmit(name.trim(), email.trim(), phone.trim(), address.trim())
            }
        },
        confirmEnabled = name.isNotBlank(),
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
            RestoTextField(
                value = name,
                onValueChange = { name = it },
                label = if (isPersian) "نام و نام خانوادگی *" else "Full Name *"
            )

            RestoTextField(
                value = phone,
                onValueChange = { phone = it },
                label = if (isPersian) "شماره تماس" else "Phone Number"
            )

            RestoTextField(
                value = email,
                onValueChange = { email = it },
                label = if (isPersian) "آدرس ایمیل" else "Email Address"
            )

            RestoTextField(
                value = address,
                onValueChange = { address = it },
                label = if (isPersian) "آدرس پستی" else "Address",
                singleLine = false,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailSheet(
    customer: Customer,
    isPersian: Boolean,
    currencyUnit: String,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedSpent = if (isPersian) {
        "${PersianFormatter.formatTomans(customer.totalPurchases)} $currencyUnit"
    } else {
        "$${String.format("%.2f", customer.totalPurchases)}"
    }

    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = customer.name,
        subtitle = if (isPersian) "پروفایل مشتری" else "Customer Profile"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RestoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            RestoCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPersian) "مجموع خرید مشتری" else "Total Purchases",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formattedSpent,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPersian) "امتیاز وفاداری" else "Loyalty Points",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isPersian) "${PersianFormatter.toPersianDigits(customer.loyaltyPoints)} امتیاز" else "${customer.loyaltyPoints} pts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (customer.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                    Text(
                        text = if (isPersian) PersianFormatter.toPersianDigits(customer.phone) else customer.phone,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (customer.email.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                    Text(text = customer.email, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (customer.address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                    Text(text = customer.address, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoButton(
                    text = if (isPersian) "ویرایش اطلاعات" else "Edit Profile",
                    onClick = onEditClick,
                    variant = RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )

                RestoButton(
                    text = if (isPersian) "بستن" else "Close",
                    onClick = onDismiss,
                    variant = RestoButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
