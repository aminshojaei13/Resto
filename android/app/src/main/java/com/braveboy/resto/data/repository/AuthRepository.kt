package com.braveboy.resto.data.repository

import com.braveboy.resto.data.local.datastore.SessionStore
import com.braveboy.resto.data.local.datastore.TenantPreferences
import com.braveboy.resto.data.local.db.AppDatabase
import com.braveboy.resto.data.remote.RestoApiService
import com.braveboy.resto.data.remote.NetworkModule
import com.braveboy.resto.data.remote.dto.LoginRequestDto
import com.braveboy.resto.data.remote.dto.MembershipDto
import com.braveboy.resto.data.remote.dto.MembershipStoreDto
import com.braveboy.resto.data.remote.dto.MembershipWarehouseDto
import com.braveboy.resto.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

sealed interface AuthResult {
    data class Success(val user: UserDto) : AuthResult
    data class Error(val message: String) : AuthResult
}

interface AuthRepository {
    val currentUser: Flow<UserDto?>
    val isSessionValid: Flow<Boolean>
    suspend fun login(email: String, password: String, locale: String = "fa"): AuthResult
    suspend fun startOfflineMode(): AuthResult
    suspend fun setupLocalBusiness(storeName: String, ownerName: String): AuthResult
    suspend fun getProfile(): Result<UserDto>
    suspend fun restoreSession(): Boolean
    suspend fun logout()
    suspend fun invalidateSession()
}

class AuthRepositoryImpl(
    private val apiService: RestoApiService,
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

                // Clear previous user's local database cache to prevent data leakage
                database.clearAllTables()

                // Sync memberships to local database & store user info
                syncUserMemberships(user)

                // Resolve primary organization, store, and warehouse from memberships
                val primaryOrg = user.memberships?.firstOrNull()
                val orgId = primaryOrg?.id ?: "org_local_default"
                val primaryStore = primaryOrg?.stores?.firstOrNull()
                val storeId = primaryStore?.id ?: "store_local_default"
                val primaryWarehouse = primaryStore?.warehouses?.firstOrNull()
                val warehouseId = primaryWarehouse?.id ?: "wh_local_default"

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
            AuthResult.Error("خطا در اتصال به سرور. می‌توانید از حالت آفلاین استفاده کنید.")
        }
    }

    override suspend fun startOfflineMode(): AuthResult {
        return try {
            val offlineToken = "OFFLINE_SESSION_TOKEN"
            sessionStore.save(offlineToken)
            sessionStore.updateCachedToken(offlineToken)

            val user = ensureLocalOfflineTenantInitialized()
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("خطا در راه‌اندازی حالت آفلاین: ${e.localizedMessage}")
        }
    }

    override suspend fun setupLocalBusiness(storeName: String, ownerName: String): AuthResult {
        return try {
            val offlineToken = "OFFLINE_SESSION_TOKEN"
            sessionStore.save(offlineToken)
            sessionStore.updateCachedToken(offlineToken)

            val cleanStoreName = storeName.trim().ifBlank { "فروشگاه من" }
            val cleanOwnerName = ownerName.trim().ifBlank { "ایران علوی" }

            val timestamp = System.currentTimeMillis().toString().takeLast(6)
            val orgId = "org_local_$timestamp"
            val storeId = "store_local_$timestamp"
            val warehouseId = "wh_local_$timestamp"
            val userId = "usr_local_$timestamp"

            val user = UserDto(
                id = userId,
                name = cleanOwnerName,
                email = "offline@local",
                role = "admin",
                memberships = listOf(
                    MembershipDto(
                        id = orgId,
                        name = cleanStoreName,
                        role = "admin",
                        stores = listOf(
                            MembershipStoreDto(
                                id = storeId,
                                name = "شعبه اصلی",
                                warehouses = listOf(
                                    MembershipWarehouseDto(
                                        id = warehouseId,
                                        name = "انبار اصلی"
                                    )
                                )
                            )
                        )
                    )
                )
            )

            _currentUser.value = user
            syncUserMemberships(user)
            tenantPreferences.setActiveTenant(orgId, storeId, warehouseId)
            tenantPreferences.setUserInfo(userId, cleanOwnerName, "admin")
            NetworkModule.setTenantContext(orgId, storeId)

            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("خطا در ایجاد کسب‌وکار: ${e.localizedMessage}")
        }
    }

    override suspend fun getProfile(): Result<UserDto> {
        val token = sessionStore.currentToken()
        if (token == "OFFLINE_SESSION_TOKEN" || token.startsWith("OFFLINE_")) {
            val tenant = tenantPreferences.tenantState.first()
            val offlineUser = UserDto(
                id = tenant.userId.ifBlank { "usr_local_default" },
                name = tenant.userName.ifBlank { "کسب‌وکار من" },
                email = "offline@local",
                role = tenant.userRole.ifBlank { "admin" }
            )
            _currentUser.value = offlineUser
            return Result.success(offlineUser)
        }

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

        if (token == "OFFLINE_SESSION_TOKEN" || token.startsWith("OFFLINE_")) {
            ensureLocalOfflineTenantInitialized()
            return true
        }

        if (token.isBlank()) {
            val tenant = tenantPreferences.tenantState.first()
            if (tenant.activeOrgId.isNotBlank()) {
                sessionStore.save("OFFLINE_SESSION_TOKEN")
                sessionStore.updateCachedToken("OFFLINE_SESSION_TOKEN")
                ensureLocalOfflineTenantInitialized()
                return true
            }
            return false
        }

        // Stored token exists from online login
        val profileResult = getProfile()
        return if (profileResult.isSuccess) {
            true
        } else {
            val exception = profileResult.exceptionOrNull()
            if (exception != null && isNetworkException(exception)) {
                // Network is offline; keep offline session active without logging user out!
                val tenant = tenantPreferences.tenantState.first()
                val cachedUser = UserDto(
                    id = tenant.userId.ifBlank { "usr_local_default" },
                    name = tenant.userName.ifBlank { "کسب‌وکار من (آفلاین)" },
                    email = "offline@local",
                    role = tenant.userRole.ifBlank { "admin" }
                )
                _currentUser.value = cachedUser
                if (tenant.activeOrgId.isNotBlank()) {
                    NetworkModule.setTenantContext(tenant.activeOrgId, tenant.activeStoreId)
                } else {
                    ensureLocalOfflineTenantInitialized()
                }
                true
            } else {
                // Token explicitly rejected by server (401)
                invalidateSession()
                false
            }
        }
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
        database.clearAllTables()
    }

    private suspend fun ensureLocalOfflineTenantInitialized(): UserDto {
        val tenant = tenantPreferences.tenantState.first()
        val orgId = if (tenant.activeOrgId.isNotBlank()) tenant.activeOrgId else "org_local_default"
        val storeId = if (tenant.activeStoreId.isNotBlank()) tenant.activeStoreId else "store_local_default"
        val warehouseId = if (tenant.activeWarehouseId.isNotBlank()) tenant.activeWarehouseId else "wh_local_default"
        val userId = if (tenant.userId.isNotBlank()) tenant.userId else "usr_local_default"
        val userName = if (tenant.userName.isNotBlank()) tenant.userName else "کسب‌وکار من"
        val role = if (tenant.userRole.isNotBlank()) tenant.userRole else "admin"

        val offlineUser = UserDto(
            id = userId,
            name = userName,
            email = "offline@local",
            role = role,
            memberships = listOf(
                MembershipDto(
                    id = orgId,
                    name = userName,
                    role = role,
                    stores = listOf(
                        MembershipStoreDto(
                            id = storeId,
                            name = "شعبه اصلی",
                            warehouses = listOf(
                                MembershipWarehouseDto(
                                    id = warehouseId,
                                    name = "انبار اصلی"
                                )
                            )
                        )
                    )
                )
            )
        )

        _currentUser.value = offlineUser

        syncUserMemberships(offlineUser)
        tenantPreferences.setActiveTenant(orgId, storeId, warehouseId)
        NetworkModule.setTenantContext(orgId, storeId)

        return offlineUser
    }

    private fun isNetworkException(e: Throwable): Boolean {
        return e is java.io.IOException
    }

    private suspend fun syncUserMemberships(user: UserDto) {
        tenantPreferences.setUserInfo(user.id, user.name, user.role ?: "Admin")

        val memberships = user.memberships ?: emptyList()
        if (memberships.isNotEmpty()) {
            val orgEntities = mutableListOf<com.braveboy.resto.data.local.db.entity.OrganizationEntity>()
            val storeEntities = mutableListOf<com.braveboy.resto.data.local.db.entity.StoreEntity>()
            val warehouseEntities = mutableListOf<com.braveboy.resto.data.local.db.entity.WarehouseEntity>()

            for (m in memberships) {
                orgEntities.add(
                    com.braveboy.resto.data.local.db.entity.OrganizationEntity(
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
                        com.braveboy.resto.data.local.db.entity.StoreEntity(
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
                            com.braveboy.resto.data.local.db.entity.WarehouseEntity(
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
