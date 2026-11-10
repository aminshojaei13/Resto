package com.braveboy.resto.ui.supplier

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddBusiness
import androidx.compose.material.icons.rounded.Contacts
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.resto.R
import com.braveboy.resto.data.model.Supplier
import com.braveboy.resto.ui.components.RestoBottomSheet
import com.braveboy.resto.ui.components.RestoButton
import com.braveboy.resto.ui.components.RestoButtonVariant
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoEmptyState
import com.braveboy.resto.ui.components.RestoLoadingState
import com.braveboy.resto.ui.components.RestoSearchField
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.util.PersianFormatter

@Composable
fun SupplierScreen(
    viewModel: SupplierViewModel,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isAddSupplierDialogOpen by viewModel.isAddSupplierDialogOpen.collectAsState()
    val selectedSupplierForDetail by viewModel.selectedSupplierForDetail.collectAsState()
    val selectedSupplierForEdit by viewModel.selectedSupplierForEdit.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RestoTopBar(
                title = stringResource(R.string.suppliers_title),
                onNavigationClick = onBackClick
            )
        },
        floatingActionButton = {
            if (suppliers.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.openAddSupplierDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddBusiness,
                        contentDescription = stringResource(R.string.add_supplier_title)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .padding(horizontal = RestoSpacing.md)
        ) {
            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            RestoSearchField(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = "جستجوی تأمین‌کننده با نام، تلفن یا نشانی..."
            )

            Spacer(modifier = Modifier.height(RestoSpacing.sm))

            if (isLoading && suppliers.isEmpty()) {
                RestoLoadingState(
                    modifier = Modifier.weight(1f),
                    message = stringResource(R.string.loading_suppliers)
                )
            } else if (suppliers.isEmpty()) {
                RestoEmptyState(
                    title = stringResource(R.string.no_suppliers_title),
                    description = stringResource(R.string.no_suppliers_desc),
                    actionText = stringResource(R.string.add_supplier_title),
                    onActionClick = { viewModel.openAddSupplierDialog() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(suppliers, key = { it.id }) { supplier ->
                        SupplierItemCard(
                            supplier = supplier,
                            onClick = { viewModel.selectSupplierForDetail(supplier) }
                        )
                    }
                }
            }
        }
    }

    // Add Supplier Dialog
    if (isAddSupplierDialogOpen) {
        SupplierFormDialog(
            title = stringResource(R.string.add_supplier_title),
            initialSupplier = null,
            confirmLoading = isSaving,
            onSubmit = { name, email, phone, address ->
                viewModel.addSupplier(name, email, phone, address)
            },
            onDismiss = { viewModel.closeAddSupplierDialog() }
        )
    }

    // Edit Supplier Dialog
    selectedSupplierForEdit?.let { supplier ->
        SupplierFormDialog(
            title = stringResource(R.string.edit_supplier_title),
            initialSupplier = supplier,
            confirmLoading = isSaving,
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Domain,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplier.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (supplier.phone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = PersianFormatter.toPersianDigits(supplier.phone),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "جزئیات",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SupplierFormDialog(
    title: String,
    initialSupplier: Supplier?,
    onSubmit: (name: String, email: String, phone: String, address: String) -> Unit,
    onDismiss: () -> Unit,
    confirmLoading: Boolean = false
) {
    var name by remember { mutableStateOf(initialSupplier?.name ?: "") }
    var phone by remember { mutableStateOf(initialSupplier?.phone ?: "") }
    var email by remember { mutableStateOf(initialSupplier?.email ?: "") }
    var address by remember { mutableStateOf(initialSupplier?.address ?: "") }
    var showErrors by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val idIdx = c.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIdx = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val hasPhoneIdx = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                        if (nameIdx >= 0) {
                            val pickedName = c.getString(nameIdx)
                            if (!pickedName.isNullOrBlank()) {
                                name = pickedName
                            }
                        }

                        val contactId = if (idIdx >= 0) c.getString(idIdx) else ""
                        val hasPhone = if (hasPhoneIdx >= 0) c.getString(hasPhoneIdx) else "0"

                        if (hasPhone == "1" && contactId.isNotBlank()) {
                            val phoneCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                arrayOf(contactId),
                                null
                            )
                            phoneCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val phoneIdx = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (phoneIdx >= 0) {
                                        val rawPhone = pc.getString(phoneIdx) ?: ""
                                        val clean = rawPhone.replace(Regex("[^0-9+]"), "")
                                        if (clean.isNotBlank()) {
                                            phone = clean
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback if query fails
            }
        }
    }

    val isNameValid = name.trim().isNotBlank()
    val isPhoneValid = phone.trim().isNotBlank()

    RestoDialog(
        title = title,
        confirmText = stringResource(R.string.save_supplier),
        confirmLoading = confirmLoading,
        onConfirm = {
            if (isNameValid && isPhoneValid) {
                onSubmit(name.trim(), email.trim(), phone.trim(), address.trim())
            } else {
                showErrors = true
            }
        },
        confirmEnabled = true,
        onDismissRequest = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { contactPickerLauncher.launch(null) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Contacts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "انتخاب از مخاطبین دستگاه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "جستجو",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            RestoTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.company_supplier_label),
                errorText = if (showErrors && !isNameValid) "نام تأمین‌کننده نمی‌تواند خالی باشد" else null
            )

            RestoTextField(
                value = phone,
                onValueChange = { phone = it },
                label = stringResource(R.string.phone_number_label) + " (اجباری - یونیک)",
                errorText = if (showErrors && !isPhoneValid) "شماره موبایل جهت تمایز تامین‌کننده الزامی است" else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
            )

            RestoTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.email_address_label),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
            )

            RestoTextField(
                value = address,
                onValueChange = { address = it },
                label = stringResource(R.string.address_label),
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
    onEditClick: () -> Unit,
    onDismiss: () -> Unit
) {
    RestoBottomSheet(
        onDismissRequest = onDismiss,
        title = supplier.name,
        subtitle = stringResource(R.string.supplier_profile)
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
                        text = PersianFormatter.toPersianDigits(supplier.phone),
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
                    text = stringResource(R.string.edit_details),
                    onClick = onEditClick,
                    variant = RestoButtonVariant.Outlined,
                    modifier = Modifier.weight(1f)
                )

                RestoButton(
                    text = stringResource(R.string.close),
                    onClick = onDismiss,
                    variant = RestoButtonVariant.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
