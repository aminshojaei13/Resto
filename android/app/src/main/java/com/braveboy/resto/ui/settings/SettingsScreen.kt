package com.braveboy.resto.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.braveboy.resto.R
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.sync.SyncStatus
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoDialog
import com.braveboy.resto.ui.components.RestoListItem
import com.braveboy.resto.ui.components.RestoSection
import com.braveboy.resto.ui.components.RestoTextField
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoShapes
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.PersianFormatter

@Composable
fun SettingsScreen(
    tenantState: TenantState,
    syncStatus: SyncStatus,
    unsyncedCount: Int,
    isDarkMode: Boolean = true,
    currentTaxRate: Double = 0.0,
    isBackupInProgress: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onCreateBackup: () -> Unit = {},
    onUpdateTaxRate: (Double) -> Unit = {},
    onTriggerSync: () -> Unit = {},
    onLogout: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onOpenTenantSwitcher: () -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SettingsScreenContent(
        tenantState = tenantState,
        syncStatus = syncStatus,
        unsyncedCount = unsyncedCount,
        isDarkMode = isDarkMode,
        currentTaxRate = currentTaxRate,
        isBackupInProgress = isBackupInProgress,
        onToggleTheme = onToggleTheme,
        onCreateBackup = onCreateBackup,
        onUpdateTaxRate = onUpdateTaxRate,
        onTriggerSync = onTriggerSync,
        onLogout = onLogout,
        onAvatarClick = onAvatarClick,
        onOpenTenantSwitcher = onOpenTenantSwitcher,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
fun SettingsScreenContent(
    tenantState: TenantState,
    syncStatus: SyncStatus,
    unsyncedCount: Int,
    isDarkMode: Boolean = true,
    currentTaxRate: Double = 0.0,
    isBackupInProgress: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onCreateBackup: () -> Unit = {},
    onUpdateTaxRate: (Double) -> Unit = {},
    onTriggerSync: () -> Unit = {},
    onLogout: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onOpenTenantSwitcher: () -> Unit = {},
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isTaxDialogOpened by remember { mutableStateOf(false) }
    var taxRateInputText by remember { mutableStateOf(if (currentTaxRate % 1.0 == 0.0) currentTaxRate.toInt().toString() else currentTaxRate.toString()) }

    Scaffold(
        topBar = {
            RestoTopBar(
                title = "تنظیمات",
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
                                    text = tenantState.userName.ifBlank { "مدیر سیستم" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "نقش: ${tenantState.userRole}  •  لمس برای تغییر تصویر",
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
                    title = "ظاهر و پوسته برنامه"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        RestoListItem(
                            title = "تم برنامه (روشن / تاریک)",
                            subtitle = if (isDarkMode) "حالت تاریک (شب) فعال است" else "حالت روشن (روز) فعال است",
                            leadingIcon = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            onClick = onToggleTheme
                        )
                    }
                }
            }

            item {
                RestoSection(
                    title = "همگام‌سازی و دیتابیس آفلاین"
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
                                        text = "موارد منتظر ارسال به سرور: ${PersianFormatter.toPersianDigits(unsyncedCount)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = when (syncStatus) {
                                            SyncStatus.SYNCING -> "در حال ارسال اطلاعات..."
                                            SyncStatus.OFFLINE -> "آفلاین - ثبت محلی انجام می‌شود"
                                            SyncStatus.SUCCESS -> "همه اطلاعات همگام است"
                                            SyncStatus.ERROR -> "خطا در همگام‌سازی شبکه"
                                            SyncStatus.IDLE -> "سیستم آماده همگام‌سازی"
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
                            Text(text = "همگام‌سازی خودکار با سرور")
                        }

                        Spacer(modifier = Modifier.height(RestoSpacing.sm))

                        com.braveboy.resto.ui.components.RestoButton(
                            text = "پشتیبان‌گیری محلی و استخراج فایل (JSON)",
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
                    title = "اطلاعات کسب‌وکار و شعب"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        RestoListItem(
                            title = "تغییر سازمان و شعبه فعال",
                            subtitle = "شعبه جاری: ${tenantState.activeStoreId}",
                            leadingIcon = Icons.Rounded.Storefront,
                            onClick = onOpenTenantSwitcher
                        )
                    }
                }
            }

            item {
                RestoSection(
                    title = "ترجیحات برنامه و مالیات"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        val taxRateDisplayStr = if (currentTaxRate % 1.0 == 0.0) currentTaxRate.toInt().toString() else currentTaxRate.toString()
                        RestoListItem(
                            title = "نرخ مالیات بر ارزش افزوده (VAT)",
                            subtitle = "${PersianFormatter.toPersianDigits(taxRateDisplayStr)}٪ (محاسبه خودکار در فاکتورهای فروش)",
                            leadingIcon = Icons.Rounded.ReceiptLong,
                            onClick = {
                                taxRateInputText = taxRateDisplayStr
                                isTaxDialogOpened = true
                            }
                        )

                        RestoListItem(
                            title = "واحد پولی پیش‌فرض",
                            subtitle = "تومان (محاسبات دقیق مالی)",
                            leadingIcon = Icons.Rounded.Tune
                        )
                    }
                }
            }

            // About Resto Section
            item {
                RestoSection(
                    title = "درباره رستو"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        RestoListItem(
                            title = "Resto Business OS",
                            subtitle = "نسخه ۱.۰ (طراحی استاندارد سال ۲۰۲۶)",
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
                            title = "خروج از حساب کاربری",
                            subtitle = "اتمام نشست فعلی و خروج امن از سیستم",
                            leadingIcon = Icons.AutoMirrored.Rounded.Logout,
                            onClick = onLogout
                        )
                    }
                }
            }
        }

        if (isTaxDialogOpened) {
            RestoDialog(
                title = "تنظیم نرخ مالیات بر ارزش افزوده",
                confirmText = "ثبت و ذخیره",
                onConfirm = {
                    val parsedRate = PersianFormatter.parsePrice(taxRateInputText)
                    onUpdateTaxRate(parsedRate)
                    isTaxDialogOpened = false
                },
                onDismissRequest = { isTaxDialogOpened = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RestoSpacing.sm)) {
                    Text(
                        text = "درصد مالیات بر ارزش افزوده را وارد کنید. این درصد به طور خودکار در محاسبات فاکتور فروش و ثبت کالا اعمال می‌شود.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    RestoTextField(
                        value = taxRateInputText,
                        onValueChange = { taxRateInputText = it.filter { ch -> ch.isDigit() || ch == '.' || ch in '۰'..'۹' } },
                        label = "درصد مالیات (مثلاً ۹ یا ۰)",
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
