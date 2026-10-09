package com.braveboy.calcuapp

import com.braveboy.calcuapp.data.model.CartItem
import com.braveboy.calcuapp.data.model.TenantState
import com.braveboy.calcuapp.data.remote.dto.MembershipDto
import com.braveboy.calcuapp.data.remote.dto.MembershipStoreDto
import com.braveboy.calcuapp.data.remote.dto.MembershipWarehouseDto
import com.braveboy.calcuapp.data.remote.dto.UserDto
import com.braveboy.calcuapp.data.repository.AuthRepository
import com.braveboy.calcuapp.data.repository.AuthResult
import com.braveboy.calcuapp.ui.auth.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthAndSessionUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultTenantState_hasNoHardcodedAdmin() {
        val state = TenantState()
        assertEquals("", state.userId)
        assertEquals("", state.userName)
        assertEquals("", state.userRole)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun cartItem_supportsDynamicTaxRates() {
        // Zero tax rate test
        val zeroTaxItem = CartItem(
            id = "c1",
            orgId = "org_1",
            storeId = "st_1",
            productId = "p1",
            productName = "Product A",
            sku = "SKU-A",
            barcode = "123",
            price = 100000.0,
            quantity = 2,
            taxRate = 0.0
        )
        assertEquals(200000.0, zeroTaxItem.subtotal, 0.01)
        assertEquals(0.0, zeroTaxItem.taxAmount, 0.01)
        assertEquals(200000.0, zeroTaxItem.total, 0.01)

        // 9% dynamic Iranian VAT tax test
        val vatItem = CartItem(
            id = "c2",
            orgId = "org_1",
            storeId = "st_1",
            productId = "p2",
            productName = "Product B",
            sku = "SKU-B",
            barcode = "456",
            price = 100000.0,
            quantity = 1,
            taxRate = 0.09
        )
        assertEquals(100000.0, vatItem.subtotal, 0.01)
        assertEquals(9000.0, vatItem.taxAmount, 0.01)
        assertEquals(109000.0, vatItem.total, 0.01)
    }

    @Test
    fun loginViewModel_validatesEmptyFields() = runTest {
        val fakeRepo = FakeAuthRepository(AuthResult.Success(createSampleUser()))
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.login()
        assertEquals("لطفاً ایمیل خود را وارد کنید.", viewModel.uiState.value.errorMessage)

        viewModel.onEmailChanged("admin@gmail.com")
        viewModel.login()
        assertEquals("لطفاً رمز عبور خود را وارد کنید.", viewModel.uiState.value.errorMessage)

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loginViewModel_successfulLogin_updatesUiState() = runTest {
        val sampleUser = createSampleUser()
        val fakeRepo = FakeAuthRepository(AuthResult.Success(sampleUser))
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onEmailChanged("admin@gmail.com")
        viewModel.onPasswordChanged("2233")
        viewModel.login()

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loginViewModel_failedLogin_displaysErrorMessage() = runTest {
        val fakeRepo = FakeAuthRepository(AuthResult.Error("ایمیل یا رمز عبور وارد شده نادرست است."))
        val viewModel = LoginViewModel(fakeRepo)

        viewModel.onEmailChanged("wrong@gmail.com")
        viewModel.onPasswordChanged("wrongpass")
        viewModel.login()

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("ایمیل یا رمز عبور وارد شده نادرست است.", viewModel.uiState.value.errorMessage)
    }

    private fun createSampleUser(): UserDto {
        return UserDto(
            id = "usr_real_1",
            name = "مدیر سیستم",
            email = "admin@gmail.com",
            role = "admin",
            memberships = listOf(
                MembershipDto(
                    id = "org_real_1",
                    name = "فروشگاه مرکزی",
                    role = "admin",
                    stores = listOf(
                        MembershipStoreDto(
                            id = "store_real_1",
                            name = "شعبه اصلی",
                            warehouses = listOf(
                                MembershipWarehouseDto(
                                    id = "wh_real_1",
                                    name = "انبار مرکزی"
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    private class FakeAuthRepository(
        private val loginResult: AuthResult
    ) : AuthRepository {
        override val currentUser: Flow<UserDto?> = flowOf(null)
        override val isSessionValid: Flow<Boolean> = flowOf(false)

        override suspend fun login(email: String, password: String, locale: String): AuthResult {
            return loginResult
        }

        override suspend fun getProfile(): Result<UserDto> {
            return Result.failure(NotImplementedError())
        }

        override suspend fun restoreSession(): Boolean = false
        override suspend fun logout() {}
        override suspend fun invalidateSession() {}
    }
}
