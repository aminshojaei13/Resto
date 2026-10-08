package com.braveboy.calcuapp.ui.supplier

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
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.braveboy.calcuapp.data.model.Supplier
import com.braveboy.calcuapp.ui.components.RestoBottomSheet
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoDialog
import com.braveboy.calcuapp.ui.components.RestoEmptyState
import com.braveboy.calcuapp.ui.components.RestoSearchField
import com.braveboy.calcuapp.ui.components.RestoTextField
import com.braveboy.calcuapp.ui.components.RestoTopBar
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import com.braveboy.calcuapp.util.PersianFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    viewModel: SupplierViewModel,
    modifier: Modifier = Modifier
) {
    val tenantState by viewModel.tenantState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val isAddSupplierDialogOpen by viewModel.isAddSupplierDialogOpen.collectAsState()
    val selectedSupplierForDetail by viewModel.selectedSupplierForDetail.collectAsState()
    val selectedSupplierForEdit by viewModel.selectedSupplierForEdit.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val isPersian = tenantState.language == "fa"
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
                title = if (isPersian) "مدیریت تأمین‌کنندگان" else "Supplier Management",
                subtitle = if (isPersian) "فهرست فروشندگان کالا و شرکت‌های طرف قرارداد" else "Suppliers, vendors & contact directory"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddSupplierDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddBusiness,
                    contentDescription = if (isPersian) "افزودن تأمین‌کننده" else "Add Supplier"
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
                placeholder = if (isPersian) "جستجوی تأمین‌کننده با نام، تلفن یا نشانی..." else "Search suppliers..."
            )

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            if (suppliers.isEmpty()) {
                RestoEmptyState(
                    title = if (isPersian) "هیچ تأمین‌کننده‌ای ثبت نشده است" else "No suppliers found",
                    description = if (isPersian)
                        "تأمین‌کنندگان را برای ثبت خریدهای انبار و ردیابی فاکتورها اضافه کنید."
                    else
                        "Add vendors and distributors to record inventory purchases and track payables.",
                    actionText = if (isPersian) "+ افزودن تأمین‌کننده" else "+ Add Supplier",
                    onActionClick = { viewModel.openAddSupplierDialog() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(suppliers, key = { it.id }) { supplier ->
                        SupplierItemCard(
                            supplier = supplier,
                            isPersian = isPersian,
                            onClick = { viewModel.selectSupplierForDetail(supplier) },
                            onEditClick = { viewModel.openEditSupplier(supplier) }
                        )
                    }
                }
            }
        }
    }

    // Add Supplier Dialog
    if (isAddSupplierDialogOpen) {
        SupplierFormDialog(
            title = if (isPersian) "افزودن تأمین‌کننده جدید" else "Add New Supplier",
            initialSupplier = null,
            isPersian = isPersian,
            onSubmit = { name, email, phone, address ->
                viewModel.addSupplier(name, email, phone, address)
            },
            onDismiss = { viewModel.closeAddSupplierDialog() }
        )
    }

    // Edit Supplier Dialog
    selectedSupplierForEdit?.let { supplier ->
        SupplierFormDialog(
            title = if (isPersian) "ویرایش اطلاعات تأمین‌کننده" else "Edit Supplier Details",
            initialSupplier = supplier,
            isPersian = isPersian,
            onSubmit = { name, email, phone, address ->
                viewModel.updateSupplier(
                    supplier.copy(name = name, email = email, phone = phone, address = address)
                )
            },
            onDismiss = { viewModel.closeEditSupplier() }
        )
    }

    // Detail BottomSheet
    selectedSupplierForDetail?.let { supplier ->
        SupplierDetailSheet(
            supplier = supplier,
            isPersian = isPersian,
            onEditClick = {
                viewModel.selectSupplierForDetail(null)
                viewModel.openEditSupplier(supplier)
            },
            onDismiss = { viewModel.selectSupplierForDetail(null) }
        )
    }
}

@Composable
fun SupplierItemCard(
    supplier: Supplier,
    isPersian: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
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
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Domain,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(RestoSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (supplier.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPersian) PersianFormatter.toPersianDigits(supplier.phone) else supplier.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RestoButton(
                text = if (isPersian) "جزئیات" else "View",
                onClick = onClick,
                variant = RestoButtonVariant.Outlined
            )
        }
    }
}

@Composable
fun SupplierFormDialog(
    title: String,
    initialSupplier: Supplier?,
    isPersian: Boolean,
    onSubmit: (name: String, email: String, phone: String, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialSupplier?.name ?: "") }
    var phone by remember { mutableStateOf(initialSupplier?.phone ?: "") }
    var email by remember { mutableStateOf(initialSupplier?.email ?: "") }
    var address by remember { mutableStateOf(initialSupplier?.address ?: "") }

    RestoDialog(
        title = title,
        confirmText = if (isPersian) "ذخیره تأمین‌کننده" else "Save Supplier",
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
                label = if (isPersian) "نام شرکت یا فروشنده *" else "Company / Supplier Name *"
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
                label = if (isPersian) "آدرس و دفتر فروش" else "Address",
                singleLine = false,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierDetailSheet(
    supplier: Supplier,
    isPersian: Boolean,
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = supplier.name,
        subtitle = if (isPersian) "مشخصات و نشانی تأمین‌کننده" else "Supplier Profile"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = RestoSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            if (supplier.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                    Text(
                        text = if (isPersian) PersianFormatter.toPersianDigits(supplier.phone) else supplier.phone,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (supplier.email.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                    Text(text = supplier.email, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (supplier.address.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(RestoSpacing.sm))
                    Text(text = supplier.address, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RestoSpacing.sm)
            ) {
                RestoButton(
                    text = if (isPersian) "ویرایش اطلاعات" else "Edit Details",
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
