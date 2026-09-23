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

class TenantPreferences(private val context: Context) {

    private object PreferenceKeys {
        val ACTIVE_ORG_ID = stringPreferencesKey("active_org_id")
        val ACTIVE_STORE_ID = stringPreferencesKey("active_store_id")
        val ACTIVE_WAREHOUSE_ID = stringPreferencesKey("active_warehouse_id")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
    }

    val tenantState: Flow<TenantState> = context.tenantDataStore.data.map { prefs ->
        TenantState(
            activeOrgId = prefs[PreferenceKeys.ACTIVE_ORG_ID] ?: "org_apex",
            activeStoreId = prefs[PreferenceKeys.ACTIVE_STORE_ID] ?: "store_apex_1",
            activeWarehouseId = prefs[PreferenceKeys.ACTIVE_WAREHOUSE_ID] ?: "wh_apex_1a",
            userId = prefs[PreferenceKeys.USER_ID] ?: "usr_admin_1",
            userName = prefs[PreferenceKeys.USER_NAME] ?: "Alex Mercer",
            userRole = prefs[PreferenceKeys.USER_ROLE] ?: "Regional Store Manager",
            isLoggedIn = true
        )
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

    suspend fun updateUserProfile(userId: String, name: String, role: String) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.USER_ID] = userId
            prefs[PreferenceKeys.USER_NAME] = name
            prefs[PreferenceKeys.USER_ROLE] = role
        }
    }
}
