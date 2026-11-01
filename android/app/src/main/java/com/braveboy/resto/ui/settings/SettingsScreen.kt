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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.braveboy.resto.data.model.TenantState
import com.braveboy.resto.data.repository.AuthRepository
import com.braveboy.resto.data.repository.TenantRepository
import com.braveboy.resto.data.sync.SyncManager
import com.braveboy.resto.data.sync.SyncStatus
import com.braveboy.resto.ui.components.RestoCard
import com.braveboy.resto.ui.components.RestoListItem
import com.braveboy.resto.ui.components.RestoSection
import com.braveboy.resto.ui.components.RestoTopBar
import com.braveboy.resto.ui.theme.RestoSpacing
import com.braveboy.resto.ui.theme.RestoTheme
import com.braveboy.resto.util.ImageStorageManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    tenantRepository: TenantRepository,
    authRepository: AuthRepository,
    syncManager: SyncManager? = null,
    onOpenTenantSwitcher: () -> Unit,
    onBackClick: () -> Unit = {},
) {
    val tenantState by tenantRepository.getTenantState().collectAsState(initial = TenantState())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

    SettingsScreenContent(
        tenantState = tenantState,
        syncStatus = syncStatus,
        unsyncedCount = unsyncedCount,
        onTriggerSync = {
            scope.launch {
                syncManager?.triggerSync(tenantState.activeOrgId)
            }
        },
        onLogout = {
            scope.launch {
                authRepository.logout()
            }
        },
        onAvatarClick = {
            avatarPickerLauncher.launch("image/*")
        },
        onOpenTenantSwitcher = onOpenTenantSwitcher,
        modifier = modifier,
        onBackClick = onBackClick,
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreenContent(
    tenantState: TenantState,
    syncStatus: SyncStatus,
    unsyncedCount: Int,
    onTriggerSync: () -> Unit,
    onLogout: () -> Unit,
    onAvatarClick: () -> Unit = {},
    onOpenTenantSwitcher: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
    val isPersian = tenantState.language == "fa"

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
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = onAvatarClick
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
                            modifier = Modifier.weight(1f)
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

                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
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
                    title = if (isPersian) "ترجیحات برنامه" else "App Preferences"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
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
