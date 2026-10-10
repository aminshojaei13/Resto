package com.braveboy.calcuapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        val ONBOARDING_DISMISSED = booleanPreferencesKey("onboarding_dismissed")
        val ONBOARDING_STEP_SUPPLIERS_SKIPPED = booleanPreferencesKey("onboarding_step_suppliers_skipped")
        val HAS_SEEN_ONBOARDING_DIALOG = booleanPreferencesKey("has_seen_onboarding_dialog")
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

    val isOnboardingDismissed: Flow<Boolean> = context.tenantDataStore.data.map { prefs ->
        prefs[PreferenceKeys.ONBOARDING_DISMISSED] ?: false
    }

    suspend fun setOnboardingDismissed(dismissed: Boolean) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.ONBOARDING_DISMISSED] = dismissed
        }
    }

    val isSupplierStepSkipped: Flow<Boolean> = context.tenantDataStore.data.map { prefs ->
        prefs[PreferenceKeys.ONBOARDING_STEP_SUPPLIERS_SKIPPED] ?: false
    }

    suspend fun setSupplierStepSkipped(skipped: Boolean) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.ONBOARDING_STEP_SUPPLIERS_SKIPPED] = skipped
        }
    }

    val hasSeenOnboardingDialog: Flow<Boolean> = context.tenantDataStore.data.map { prefs ->
        prefs[PreferenceKeys.HAS_SEEN_ONBOARDING_DIALOG] ?: false
    }

    suspend fun setHasSeenOnboardingDialog(seen: Boolean) {
        context.tenantDataStore.edit { prefs ->
            prefs[PreferenceKeys.HAS_SEEN_ONBOARDING_DIALOG] = seen
        }
    }
}