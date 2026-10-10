package com.braveboy.calcuapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.rounded.Logout
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.repository.AuthRepository
import com.braveboy.calcuapp.data.repository.TenantRepository
import com.braveboy.calcuapp.ui.components.RestoCard
import com.braveboy.calcuapp.ui.components.RestoListItem
import com.braveboy.calcuapp.ui.components.RestoSection
import com.braveboy.calcuapp.ui.components.RestoTopBar
import com.braveboy.calcuapp.ui.theme.RestoSpacing
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    tenantRepository: TenantRepository,
    authRepository: AuthRepository,
    onOpenTenantSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tenantState by tenantRepository.getTenantState().collectAsState(initial = TenantState())
    val isPersian = tenantState.language == "fa"
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            RestoTopBar(
                title = if (isPersian) "تنظیمات و حساب کاربری" else "Settings & Profile",
                subtitle = if (isPersian) "مدیریت کسب‌وکار و ترجیحات برنامه" else "Manage business and app preferences"
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(RestoSpacing.md),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(RestoSpacing.md)
        ) {
            // Profile Card
            item {
                RestoCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RestoListItem(
                            title = tenantState.userName.ifBlank { if (isPersian) "مدیر سیستم" else "Manager" },
                            subtitle = "${if (isPersian) "نقش:" else "Role:"} ${tenantState.userRole}",
                            leadingText = tenantState.userName.ifBlank { "M" }
                        )
                    }
                }
            }

            // Organization & Branch Section
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

            // Preferences Section
            item {
                RestoSection(
                    title = if (isPersian) "ترجیحات برنامه" else "App Preferences"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        // Language toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = RestoSpacing.md, vertical = RestoSpacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.padding(start = RestoSpacing.md))
                                Column {
                                    Text(
                                        text = if (isPersian) "زبان فارسی (راست‌چین)" else "Persian Language (RTL)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isPersian) "استفاده از تقویم و ارقام فارسی" else "Use Persian calendar & digits",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isPersian,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        tenantRepository.setLanguage(if (checked) "fa" else "en")
                                    }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = RestoSpacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Currency info
                        RestoListItem(
                            title = if (isPersian) "واحد پولی پیش‌فرض" else "Default Currency",
                            subtitle = if (isPersian) "تومان (محاسبات دقیق مالی)" else "USD / Precision Monetary",
                            leadingIcon = Icons.Rounded.Tune
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = RestoSpacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        // Onboarding Walkthrough toggle
                        val isOnboardingDismissed by tenantRepository.isOnboardingDismissed.collectAsState(initial = false)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RestoSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.padding(start = RestoSpacing.md))
                                Column {
                                    Text(
                                        text = if (isPersian) "راهنمای شروع به کار و آموزش" else "Getting Started Guide",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isPersian) "نمایش کارت چک‌لیست ۴ مرحله‌ای در داشبورد" else "Show 4-step checklist on Dashboard",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = !isOnboardingDismissed,
                                onCheckedChange = { show ->
                                    scope.launch {
                                        tenantRepository.setOnboardingDismissed(!show)
                                        if (show) {
                                            tenantRepository.setSupplierStepSkipped(false)
                                            tenantRepository.setHasSeenOnboardingDialog(false)
                                        }
                                    }
                                }
                            )
                        }
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
                RestoSection(
                    title = if (isPersian) "حساب کاربری و امنیت" else "Account & Security"
                )
                Spacer(modifier = Modifier.height(RestoSpacing.xs))
                RestoCard {
                    Column {
                        RestoListItem(
                            title = if (isPersian) "خروج از حساب کاربری" else "Sign Out",
                            subtitle = if (isPersian) "اتمام نشست فعلی و خروج امن از سیستم" else "Terminate active session",
                            leadingIcon = Icons.AutoMirrored.Rounded.Logout,
                            onClick = {
                                scope.launch {
                                    authRepository.logout()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
