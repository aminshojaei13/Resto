package com.braveboy.calcuapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.braveboy.calcuapp.data.model.TenantState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tenantDataStore: DataStore<Preferences> by preferencesDataStore(name = "tenant_preferences")

/**
 * Which business, store and warehouse the person is currently working in.
 *
 * This deliberately holds no identity: the person's name and role come from the
 * authenticated profile, never from this file. "Signed in" is decided by
 * [SessionStore], not by whether a business happens to be selected.
 */
class TenantPreferences(private val context: Context) {

    private object PreferenceKeys {
        val ACTIVE_ORG_ID = stringPreferencesKey("active_org_id")
        val ACTIVE_STORE_ID = stringPreferencesKey("active_store_id")
        val ACTIVE_WAREHOUSE_ID = stringPreferencesKey("active_warehouse_id")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val tenantState: Flow<TenantState> = context.tenantDataStore.data.map { prefs ->
        val orgId = prefs[PreferenceKeys.ACTIVE_ORG_ID] ?: ""
        val userId = prefs[PreferenceKeys.USER_ID] ?: ""
        TenantState(
            activeOrgId = orgId,
            activeStoreId = prefs[PreferenceKeys.ACTIVE_STORE_ID] ?: "",
            activeWarehouseId = prefs[PreferenceKeys.ACTIVE_WAREHOUSE_ID] ?: "",
            userId = userId,
            userName = prefs[PreferenceKeys.USER_NAME] ?: "",
            userRole = prefs[PreferenceKeys.USER_ROLE] ?: "",
            language = prefs[PreferenceKeys.LANGUAGE] ?: "fa",
            isLoggedIn = userId.isNotBlank() || orgId.isNotBlank()
        )
    }

    suspend fun setUserInfo(userId: String, userName: String, userRole: String) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.USER_ID] = userId
            prefs[PreferenceKeys.USER_NAME] = userName
            prefs[PreferenceKeys.USER_ROLE] = userRole
        }
    }

    suspend fun setActiveTenant(orgId: String, storeId: String, warehouseId: String) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.ACTIVE_ORG_ID] = orgId
            prefs[PreferenceKeys.ACTIVE_STORE_ID] = storeId
            prefs[PreferenceKeys.ACTIVE_WAREHOUSE_ID] = warehouseId
        }
    }

    suspend fun setActiveStore(storeId: String, warehouseId: String) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.ACTIVE_STORE_ID] = storeId
            prefs[PreferenceKeys.ACTIVE_WAREHOUSE_ID] = warehouseId
        }
    }

    suspend fun setActiveWarehouse(warehouseId: String) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.ACTIVE_WAREHOUSE_ID] = warehouseId
        }
    }

    suspend fun setLanguage(language: String) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.LANGUAGE] = language
        }
    }

    /** Choosing a different business must not leave the previous person's context behind. */
    suspend fun clearActiveTenant() {
        context.tenantDataStore.edit { prefs ->
            prefs.remove(PreferenceKeys.ACTIVE_ORG_ID)
            prefs.remove(PreferenceKeys.ACTIVE_STORE_ID)
            prefs.remove(PreferenceKeys.ACTIVE_WAREHOUSE_ID)
            prefs.remove(PreferenceKeys.USER_ID)
            prefs.remove(PreferenceKeys.USER_NAME)
            prefs.remove(PreferenceKeys.USER_ROLE)
        }
    }
}