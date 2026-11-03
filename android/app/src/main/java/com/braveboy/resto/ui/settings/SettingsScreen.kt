package com.braveboy.resto.ui.settings

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import com.braveboy.resto.ui.theme.RestoShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.braveboy.resto.data.local.datastore.TenantPreferences
import com.braveboy.resto.data.local.db.AppDatabase
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.repository.AuthRepository
import com.braveboy.resto.data.repository.TenantRepository
import com.braveboy.resto.data.sync.SyncManager
import com.braveboy.resto.data.sync.SyncStatus
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoListItem
import com.braveboy.resto.ui.components.RestoSection
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.ImageStorageManager
import com.braveboy.resto.util.LocalBackupManager
import com.braveboy.resto.util.PersianFormatter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    tenantRepository: TenantRepository,
    authRepository: AuthRepository,
    tenantPreferences: TenantPreferences? = null,
    syncManager: SyncManager? = null,
    database: AppDatabase? = null,
    onOpenTenantSwitcher: () -> Unit,
    onBackClick: () -> Unit = {},
) {
    val tenantState by tenantRepository.getTenantState().collectAsState(initial = TenantState())
    val currentTaxRate by tenantPreferences?.defaultTaxRate?.collectAsState(initial = 9.0)
        ?: remember { mutableStateOf(9.0) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val response = com.braveboy.resto.data.remote.NetworkModule.apiService.getBusinessSettings()
            if (response.isSuccessful && response.body() != null) {
                val map = response.body()!!
                val rateNum = (map["default_tax_rate"] as? Number)?.toDouble() ?: 9.0
                tenantPreferences?.setDefaultTaxRate(rateNum)
            }
        } catch (_: Exception) {}
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageManager.saveImageLocally(
                context = context,
                uri = uri,
                folderName = "user_avatars",
                fileName = "profile_${System.currentTimeMillis()}"
            )
            if (!savedPath.isNullOrBlank()) {
                scope.launch {
                    tenantRepository.setUserAvatarUrl(savedPath)
                }
            }
        }
    }

    val syncStatus by syncManager?.syncState?.collectAsState() ?: remember {
        mutableStateOf(SyncStatus.IDLE)
    }
    val unsyncedCount by syncManager?.unsyncedCount?.collectAsState(initial = 0)
        ?: remember { mutableIntStateOf(0) }

    val isDarkModeState = tenantPreferences?.isDarkMode?.collectAsState(initial = true)
    val isDarkMode = isDarkModeState?.value ?: true

    var isCheckingLogout by remember { mutableStateOf(false) }
    var showOnlineLogoutDialog by remember { mutableStateOf(false) }
    var showOfflineLogoutDialog by remember { mutableStateOf(false) }
    var isLogoutInProgress by remember { mutableStateOf(false) }
    var isBackupInProgress by remember { mutableStateOf(false) }

    fun handleCreateBackup() {
        if (database == null) return
        scope.launch {
            isBackupInProgress = true
            val backupFile = LocalBackupManager.createLocalBackup(context, database, tenantPreferences)
            isBackupInProgress = false
            if (backupFile != null) {
                LocalBackupManager.shareBackup(context, backupFile)
            }
        }
    }

    SettingsScreenContent(
        tenantState = tenantState,
        syncStatus = syncStatus,
        unsyncedCount = unsyncedCount,
        currentTaxRate = currentTaxRate,
        isBackupInProgress = isBackupInProgress,
        isDarkMode = isDarkMode,
        onToggleTheme = {
            scope.launch {
                tenantPreferences?.setDarkMode(!isDarkMode)
            }
        },
        onCreateBackup = { handleCreateBackup() },
        onUpdateTaxRate = { newRate ->
            scope.launch {
                tenantPreferences?.setDefaultTaxRate(newRate)
                try {
                    com.braveboy.resto.data.remote.NetworkModule.apiService.updateBusinessSettings(
                        mapOf("default_tax_rate" to newRate)
                    )
                } catch (_: Exception) {}
            }
        },
        onTriggerSync = {
            scope.launch {
                syncManager?.triggerSync(tenantState.activeOrgId)
            }
        },
        onLogout = {
            if (!isCheckingLogout && !isLogoutInProgress) {
                scope.launch {
                    isCheckingLogout = true
                    val isOnline = authRepository.isOnlineUserAndExists()
                    isCheckingLogout = false
                    if (isOnline) {
                        showOnlineLogoutDialog = true
                    } else {
                        showOfflineLogoutDialog = true
                    }
                }
            }
        },
        onAvatarClick = {
            avatarPickerLauncher.launch("image/*")
        },
        onOpenTenantSwitcher = onOpenTenantSwitcher,
        modifier = modifier,
        onBackClick = onBackClick,
    )

    if (showOnlineLogoutDialog) {
        RestoDialog(
            title = if (tenantState.language == "fa") "همگام‌سازی و خروج از حساب" else "Sync & Sign Out",
            onDismissRequest = {
                if (!isLogoutInProgress && !isBackupInProgress) showOnlineLogoutDialog = false
            },
            confirmText = if (tenantState.language == "fa") "همگام‌سازی و خروج" else "Sync & Sign Out",
            confirmLoading = isLogoutInProgress,
            onConfirm = {
                scope.launch {
                    isLogoutInProgress = true
                    try {
                        syncManager?.triggerSync(tenantState.activeOrgId)
                    } catch (_: Exception) {}
                    authRepository.logout()
                    isLogoutInProgress = false
                    showOnlineLogoutDialog = false
                }
            },
            dismissText = if (tenantState.language == "fa") "انصراف" else "Cancel",
            stackButtons = true
        ) {
            Column {
                Text(
                    text = if (tenantState.language == "fa")
                        "حساب کاربری ابری شما فعال است. آیا مایلید اطلاعات با سرور ابری همگام‌سازی شده و سپس از حساب کاربری خارج شوید؟"
                    else
                        "Your cloud account is active. Would you like to sync data before signing out?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(RestoSpacing.md))
                com.braveboy.resto.ui.components.RestoButton(
                    text = if (tenantState.language == "fa") "پشتیبان‌گیری محلی (دانلود JSON)" else "Local Backup (JSON)",
                    onClick = { handleCreateBackup() },
                    isLoading = isBackupInProgress,
                    enabled = !isLogoutInProgress && !isBackupInProgress,
                    variant = com.braveboy.resto.ui.components.RestoButtonVariant.Outlined,
                    fullWidth = true
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                com.braveboy.resto.ui.components.RestoButton(
                    text = if (tenantState.language == "fa") "خروج بدون همگام‌سازی" else "Sign Out Without Sync",
                    onClick = {
                        scope.launch {
                            isLogoutInProgress = true
                            authRepository.logout()
                            isLogoutInProgress = false
                            showOnlineLogoutDialog = false
                        }
                    },
                    enabled = !isLogoutInProgress && !isBackupInProgress,
                    variant = com.braveboy.resto.ui.components.RestoButtonVariant.Outlined,
                    fullWidth = true
                )
            }
        }
    }

    if (showOfflineLogoutDialog) {
        RestoDialog(
            title = if (tenantState.language == "fa") "پشتیبان‌گیری و خروج از حساب محلی" else "Backup & Sign Out",
            onDismissRequest = {
                if (!isLogoutInProgress && !isBackupInProgress) showOfflineLogoutDialog = false
            },
            confirmText = if (tenantState.language == "fa") "خروج و پاک‌سازی داده‌های محلی" else "Sign Out & Clear Data",
            confirmLoading = isLogoutInProgress,
            onConfirm = {
                scope.launch {
                    isLogoutInProgress = true
                    authRepository.logout()
                    isLogoutInProgress = false
                    showOfflineLogoutDialog = false
                }
            },
            dismissText = if (tenantState.language == "fa") "انصراف (ماندن در برنامه)" else "Cancel",
            stackButtons = true
        ) {
            Column {
                Text(
                    text = if (tenantState.language == "fa")
                        "شما به صورت آفلاین وارد شده‌اید و حساب ابری ندارید.\n\nدر صورت خروج، تمام اطلاعات محلی و فاکتورهای شما در این دستگاه پاک خواهد شد!\n\nپیشنهاد می‌شود ابتدا فایل پشتیبان محلی ذخیره کنید یا به اینترنت متصل شوید تا اطلاعات شما حفظ شود."
                    else
                        "You are signed in offline without a cloud account.\nSigning out will wipe all local data!\nWe recommend taking a local backup or connecting online to sync first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(RestoSpacing.md))
                com.braveboy.resto.ui.components.RestoButton(
                    text = if (tenantState.language == "fa") "پشتیبان‌گیری محلی و اشتراک‌گذاری" else "Backup & Share Local File",
                    onClick = { handleCreateBackup() },
                    isLoading = isBackupInProgress,
                    enabled = !isLogoutInProgress && !isBackupInProgress,
                    variant = com.braveboy.resto.ui.components.RestoButtonVariant.Outlined,
                    fullWidth = true
                )
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreenContent(
    tenantState: TenantState,
    syncStatus: SyncStatus,
    unsyncedCount: Int,
    currentTaxRate: Double = 9.0,
    isBackupInProgress: Boolean = false,
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onCreateBackup: () -> Unit = {},
    onUpdateTaxRate: (Double) -> Unit = {},
    onTriggerSync: () -> Unit,
    onLogout: () -> Unit,
    onAvatarClick: () -> Unit = {},
    onOpenTenantSwitcher: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    val isPersian = tenantState.language == "fa"
    var isTaxDialogOpened by remember { mutableStateOf(false) }
    var taxRateInputText by remember { mutableStateOf(if (currentTaxRate % 1.0 == 0.0) currentTaxRate.toInt().toString() else currentTaxRate.toString()) }

    Scaffold(
        topBar = {
            RestoTopBar(
                title = if (isPersian) "تنظیمات" else "Settings",
                onNavigationClick = onBackClick,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize()
                .padding(RestoSpacing.md),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            item {
                RestoCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RestoSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RestoShapes.medium)
                                .clickable { onAvatarClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tenantState.userAvatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = tenantState.userAvatarUrl,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = tenantState.userName.ifBlank { "M" }.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 20.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = tenantState.userName.ifBlank { if (isPersian) "مدیر سیستم" else "Manager" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${if (isPersian) "نقش:" else "Role:"} ${tenantState.userRole}  •  ${if (isPersian) "لمس برای تغییر تصویر" else "Tap to change photo"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onAvatarClick) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            item {
                RestoSection(
                    title = if (isPersian) "ظاهر و پوسته برنامه" else "Appearance & Theme"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        RestoListItem(
                            title = if (isPersian) "تم برنامه (روشن / تاریک)" else "App Theme",
                            subtitle = if (isPersian) {
                                if (isDarkMode) "حالت تاریک (شب) فعال است" else "حالت روشن (روز) فعال است"
                            } else {
                                if (isDarkMode) "Dark theme enabled" else "Light theme enabled"
                            },
                            leadingIcon = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            onClick = onToggleTheme
                        )
                    }
                }
            }

            item {
                RestoSection(
                    title = if (isPersian) "همگام‌سازی و دیتابیس آفلاین" else "Offline Sync & Data"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RestoSpacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when (syncStatus) {
                                        SyncStatus.SYNCING -> Icons.Rounded.CloudSync
                                        SyncStatus.OFFLINE -> Icons.Rounded.CloudOff
                                        SyncStatus.SUCCESS -> Icons.Rounded.CloudDone
                                        else -> Icons.Rounded.Sync
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.padding(start = RestoSpacing.md))
                                Column {
                                    Text(
                                        text = if (isPersian) "موارد منتظر ارسال به سرور: $unsyncedCount" else "Pending Unsynced Items: $unsyncedCount",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = when (syncStatus) {
                                            SyncStatus.SYNCING -> if (isPersian) "در حال ارسال اطلاعات..." else "Syncing..."
                                            SyncStatus.OFFLINE -> if (isPersian) "آفلاین - ثبت محلی انجام می‌شود" else "Offline mode"
                                            SyncStatus.SUCCESS -> if (isPersian) "همه اطلاعات همگام است" else "All data synced"
                                            SyncStatus.ERROR -> if (isPersian) "خطا در همگام‌سازی شبکه" else "Sync error"
                                            SyncStatus.IDLE -> if (isPersian) "سیستم آماده همگام‌سازی" else "Ready to sync"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (syncStatus == SyncStatus.SYNCING) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.md))

                        Button(
                            onClick = onTriggerSync,
                            enabled = syncStatus != SyncStatus.SYNCING,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = if (isPersian) "همگام‌سازی خودکار با سرور" else "Sync Now With Server")
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        com.braveboy.resto.ui.components.RestoButton(
                            text = if (isPersian) "پشتیبان‌گیری محلی و استخراج فایل (JSON)" else "Create & Share Local Backup (JSON)",
                            onClick = onCreateBackup,
                            isLoading = isBackupInProgress,
                            enabled = !isBackupInProgress,
                            variant = com.braveboy.resto.ui.components.RestoButtonVariant.Outlined,
                            fullWidth = true
                        )
                    }
                }
            }

            item {
                RestoSection(
                    title = if (isPersian) "اطلاعات کسب‌وکار و شعب" else "Business & Branches"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        RestoListItem(
                            title = if (isPersian) "تغییر سازمان و شعبه فعال" else "Switch Active Organization",
                            subtitle = if (isPersian) "شعبه جاری: ${tenantState.activeStoreId}" else "Store: ${tenantState.activeStoreId}",
                            leadingIcon = Icons.Rounded.Storefront,
                            onClick = onOpenTenantSwitcher
                        )
                    }
                }
            }

            item {
                RestoSection(
                    title = if (isPersian) "ترجیحات برنامه و مالیات" else "App & Tax Preferences"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        val taxRateDisplayStr = if (currentTaxRate % 1.0 == 0.0) currentTaxRate.toInt().toString() else currentTaxRate.toString()
                        RestoListItem(
                            title = if (isPersian) "نرخ مالیات بر ارزش افزوده (VAT)" else "Default Sales Tax Rate",
                            subtitle = if (isPersian)
                                "${PersianFormatter.toPersianDigits(taxRateDisplayStr)}٪ (محاسبه خودکار در فاکتورهای فروش)"
                            else
                                "$taxRateDisplayStr% (Applied automatically on sales)",
                            leadingIcon = Icons.Rounded.ReceiptLong,
                            onClick = {
                                taxRateInputText = taxRateDisplayStr
                                isTaxDialogOpened = true
                            }
                        )

                        RestoListItem(
                            title = if (isPersian) "واحد پولی پیش‌فرض" else "Default Currency",
                            subtitle = if (isPersian) "تومان (محاسبات دقیق مالی)" else "USD / Precision Monetary",
                            leadingIcon = Icons.Rounded.Tune
                        )
                    }
                }
            }

            // About Resto Section
            item {
                RestoSection(
                    title = if (isPersian) "درباره رستو" else "About Resto"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        RestoListItem(
                            title = "Resto Business OS",
                            subtitle = if (isPersian) "نسخه ۱.۰ (طراحی استاندارد سال ۲۰۲۶)" else "Version 1.0 (2026 Standards)",
                            leadingIcon = Icons.Rounded.Info
                        )
                    }
                }
            }

            // Account & Sign Out Section
            item {
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        RestoListItem(
                            title = if (isPersian) "خروج از حساب کاربری" else "Sign Out",
                            subtitle = if (isPersian) "اتمام نشست فعلی و خروج امن از سیستم" else "Terminate active session",
                            leadingIcon = Icons.AutoMirrored.Rounded.Logout,
                            onClick = onLogout
                        )
                    }
                }
            }
        }

        if (isTaxDialogOpened) {
            RestoDialog(
                title = if (isPersian) "تنظیم نرخ مالیات بر ارزش افزوده" else "Set Sales Tax Rate",
                confirmText = if (isPersian) "ثبت و ذخیره" else "Save Rate",
                onConfirm = {
                    val parsedRate = PersianFormatter.parsePrice(taxRateInputText)
                    onUpdateTaxRate(parsedRate)
                    isTaxDialogOpened = false
                },
                onDismissRequest = { isTaxDialogOpened = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
                    Text(
                        text = if (isPersian)
                            "درصد مالیات بر ارزش افزوده را وارد کنید. این درصد به طور خودکار در محاسبات فاکتور فروش و ثبت کالا اعمال می‌شود."
                        else
                            "Enter sales tax rate percentage. This rate will apply automatically on sales calculations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    RestoTextField(
                        value = taxRateInputText,
                        onValueChange = { taxRateInputText = it.filter { ch -> ch.isDigit() || ch == '.' || ch in '۰'..'۹' } },
                        label = if (isPersian) "درصد مالیات (مثلاً ۹ یا ۰)" else "Tax Rate (%)",
                        placeholder = "9"
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    RestoTheme {
        SettingsScreenContent(
            tenantState = TenantState(
                userName = "مدیر سیستم",
                userRole = "ADMIN",
                activeOrgId = "org_1",
                activeStoreId = "شعبه مرکزی",
                language = "fa"
            ),
            syncStatus = SyncStatus.IDLE,
            unsyncedCount = 0,
            onTriggerSync = {},
            onLogout = {},
            onOpenTenantSwitcher = {},
        )
    }
}
