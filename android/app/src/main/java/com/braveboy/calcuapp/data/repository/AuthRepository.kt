package com.braveboy.calcuapp.data.repository

import com.braveboy.calcuapp.data.local.datastore.SessionStore
import com.braveboy.calcuapp.data.local.datastore.TenantPreferences
import com.braveboy.calcuapp.data.local.db.AppDatabase
import com.braveboy.calcuapp.data.remote.CalcuappApiService
import com.braveboy.calcuapp.data.remote.NetworkModule
import com.braveboy.calcuapp.data.remote.dto.LoginRequestDto
import com.braveboy.calcuapp.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthResult {
    data class Success(val user: UserDto) : AuthResult
    data class Error(val message: String) : AuthResult
}

interface AuthRepository {
    val currentUser: Flow<UserDto?>
    val isSessionValid: Flow<Boolean>
    suspend fun login(email: String, password: String, locale: String = "fa"): AuthResult
    suspend fun getProfile(): Result<UserDto>
    suspend fun restoreSession(): Boolean
    suspend fun logout()
    suspend fun invalidateSession()
}

class AuthRepositoryImpl(
    private val apiService: CalcuappApiService,
    private val sessionStore: SessionStore,
    private val tenantPreferences: TenantPreferences,
    private val database: AppDatabase
) : AuthRepository {

    private val _currentUser = MutableStateFlow<UserDto?>(null)
    override val currentUser: Flow<UserDto?> = _currentUser.asStateFlow()

    override val isSessionValid: Flow<Boolean> = sessionStore.isSignedIn

    override suspend fun login(email: String, password: String, locale: String): AuthResult {
        return try {
            val response = apiService.login(
                LoginRequestDto(
                    email = email.trim(),
                    password = password,
                    locale = locale
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val token = body.accessToken
                val user = body.user

                // Persist session token
                sessionStore.save(token)
                sessionStore.updateCachedToken(token)
                _currentUser.value = user

                // Clear previous user's local database cache to prevent data leakage (Step 42 & 43)
                database.clearAllTables()

                // Sync memberships to local database & store user info
                syncUserMemberships(user)

                // Resolve primary organization, store, and warehouse from memberships
                val primaryOrg = user.memberships?.firstOrNull()
                val orgId = primaryOrg?.id ?: ""
                val primaryStore = primaryOrg?.stores?.firstOrNull()
                val storeId = primaryStore?.id ?: ""
                val primaryWarehouse = primaryStore?.warehouses?.firstOrNull()
                val warehouseId = primaryWarehouse?.id ?: ""

                // Update tenant context
                tenantPreferences.setActiveTenant(orgId, storeId, warehouseId)
                NetworkModule.setTenantContext(orgId, storeId)

                AuthResult.Success(user)
            } else {
                val errorMsg = when (response.code()) {
                    422, 401 -> "ایمیل یا رمز عبور وارد شده نادرست است."
                    429 -> "تعداد دفعات ورود بیش از حد مجاز است. لطفاً کمی صبر کنید."
                    500, 502, 503 -> "در حال حاضر امکان برقراری ارتباط با سرور وجود ندارد."
                    else -> "خطا در ورود به حساب کاربری (${response.code()})"
                }
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AuthResult.Error("خطا در اتصال به سرور. لطفاً اینترنت خود را بررسی کنید.")
        }
    }

    override suspend fun getProfile(): Result<UserDto> {
        return try {
            val response = apiService.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                _currentUser.value = user

                // Sync memberships to local database & store user info
                syncUserMemberships(user)

                val primaryOrg = user.memberships?.firstOrNull()
                val orgId = primaryOrg?.id ?: ""
                val primaryStore = primaryOrg?.stores?.firstOrNull()
                val storeId = primaryStore?.id ?: ""
                val primaryWarehouse = primaryStore?.warehouses?.firstOrNull()
                val warehouseId = primaryWarehouse?.id ?: ""

                if (orgId.isNotBlank()) {
                    tenantPreferences.setActiveTenant(orgId, storeId, warehouseId)
                    NetworkModule.setTenantContext(orgId, storeId)
                }

                Result.success(user)
            } else {
                if (response.code() == 401) {
                    invalidateSession()
                }
                Result.failure(Exception("Failed to fetch profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreSession(): Boolean {
        sessionStore.warmUp()
        val token = sessionStore.currentToken()
        if (token.isBlank()) {
            return false
        }

        // Validate token with real backend
        val profileResult = getProfile()
        return profileResult.isSuccess
    }

    override suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) {
            // Ignore network failures on logout; local cleanup must proceed
        } finally {
            invalidateSession()
        }
    }

    override suspend fun invalidateSession() {
        _currentUser.value = null
        sessionStore.clear()
        sessionStore.updateCachedToken("")
        tenantPreferences.clearActiveTenant()
        NetworkModule.clearTenantContext()
        // Invalidate database tables to prevent cross-account data leakage
        database.clearAllTables()
    }

    private suspend fun syncUserMemberships(user: UserDto) {
        tenantPreferences.setUserInfo(user.id, user.name, user.role ?: "Admin")

        val memberships = user.memberships ?: emptyList()
        if (memberships.isNotEmpty()) {
            val orgEntities = mutableListOf<com.braveboy.calcuapp.data.local.db.entity.OrganizationEntity>()
            val storeEntities = mutableListOf<com.braveboy.calcuapp.data.local.db.entity.StoreEntity>()
            val warehouseEntities = mutableListOf<com.braveboy.calcuapp.data.local.db.entity.WarehouseEntity>()

            for (m in memberships) {
                orgEntities.add(
                    com.braveboy.calcuapp.data.local.db.entity.OrganizationEntity(
                        id = m.id,
                        name = m.name,
                        code = m.id.take(8),
                        logoUrl = "",
                        currencySymbol = "تومان",
                        currencyCode = "IRT",
                        subscriptionTier = "PRO"
                    )
                )
                val stores = m.stores ?: emptyList()
                for (st in stores) {
                    storeEntities.add(
                        com.braveboy.calcuapp.data.local.db.entity.StoreEntity(
                            id = st.id,
                            orgId = m.id,
                            name = st.name,
                            code = st.id.take(8),
                            address = "",
                            phone = ""
                        )
                    )
                    val warehouses = st.warehouses ?: emptyList()
                    for (wh in warehouses) {
                        warehouseEntities.add(
                            com.braveboy.calcuapp.data.local.db.entity.WarehouseEntity(
                                id = wh.id,
                                storeId = st.id,
                                orgId = m.id,
                                name = wh.name,
                                code = wh.id.take(8),
                                address = ""
                            )
                        )
                    }
                }
            }

            database.tenantDao().insertOrganizations(orgEntities)
            database.tenantDao().insertStores(storeEntities)
            database.tenantDao().insertWarehouses(warehouseEntities)
        }
    }
}
