package com.braveboy.calcuapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.braveboy.calcuapp.ui.components.RestoButton
import com.braveboy.calcuapp.ui.components.RestoButtonVariant
import com.braveboy.calcuapp.ui.components.RestoTextField
import com.braveboy.calcuapp.ui.icons.Resto
import com.braveboy.calcuapp.ui.theme.CalcuappTheme
import com.braveboy.calcuapp.ui.theme.RestoShapes
import com.braveboy.calcuapp.ui.theme.RestoSpacing

@Composable
fun BusinessSetupScreen(
    viewModel: BusinessSetupViewModel,
    onSetupSuccess: () -> Unit,
    onOpenOnlineLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onSetupSuccess()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = RestoSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Resto Official Logo Icon
                    Surface(
                        shape = RestoShapes.large,
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Resto,
                                contentDescription = "Resto Logo",
                                modifier = Modifier.size(48.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(RestoSpacing.md))

                    Text(
                        text = "راه‌اندازی سریع کسب‌وکار",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(RestoSpacing.xs))

                    Text(
                        text = "بدون نیاز به اینترنت؛ نام فروشگاه خود را وارد کنید و بلافاصله کار خود را شروع کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(RestoSpacing.xl))

                    // Setup Card Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RestoShapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(RestoSpacing.lg)
                        ) {
                            // Error notification if present
                            if (uiState.errorMessage != null) {
                                Surface(
                                    shape = RestoShapes.medium,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = RestoSpacing.md)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(RestoSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(RestoSpacing.xs))
                                        Text(
                                            text = uiState.errorMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            // Store Name Input
                            RestoTextField(
                                value = uiState.storeName,
                                onValueChange = { viewModel.onStoreNameChanged(it) },
                                label = "نام فروشگاه / کسب‌وکار",
                                placeholder = "مثال: پوشاک ایران",
                                leadingIcon = Icons.Rounded.Storefront,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Next
                                ),
                                enabled = !uiState.isLoading,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(RestoSpacing.md))

                            // Owner Name Input
                            RestoTextField(
                                value = uiState.ownerName,
                                onValueChange = { viewModel.onOwnerNameChanged(it) },
                                label = "نام و نام خانوادگی مدیر",
                                placeholder = "مثال: ایران علوی",
                                leadingIcon = Icons.Rounded.Person,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        viewModel.setupBusiness()
                                    }
                                ),
                                enabled = !uiState.isLoading,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(RestoSpacing.lg))

                            // Primary Start Button
                            RestoButton(
                                text = "شروع به کار (رایگان و آفلاین)",
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.setupBusiness()
                                },
                                isLoading = uiState.isLoading,
                                enabled = !uiState.isLoading,
                                fullWidth = true,
                                variant = RestoButtonVariant.Primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(RestoSpacing.xl))

                    // Secondary Link to Online Login
                    Text(
                        text = "قبلاً حساب آنلاین ساخته‌اید؟ ورود به حساب ابری",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { onOpenOnlineLogin() }
                    )
                }
            }
        }
    }
}
