package com.braveboy.resto.ui.guide

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.guideDataStore: DataStore<Preferences> by preferencesDataStore(name = "screen_guide_preferences")

class GuidePreferences(private val context: Context) {

    fun hasSeenInvitation(screenId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("invitation_seen_$screenId")
        return context.guideDataStore.data.map { prefs ->
            prefs[key] ?: false
        }
    }

    suspend fun setInvitationSeen(screenId: String, seen: Boolean = true) {
        val key = booleanPreferencesKey("invitation_seen_$screenId")
        context.guideDataStore.edit { prefs ->
            prefs[key] = seen
        }
    }

    fun isGuideCompleted(screenId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("guide_completed_$screenId")
        return context.guideDataStore.data.map { prefs ->
            prefs[key] ?: false
        }
    }

    suspend fun setGuideCompleted(screenId: String, completed: Boolean = true) {
        val key = booleanPreferencesKey("guide_completed_$screenId")
        context.guideDataStore.edit { prefs ->
            prefs[key] = completed
        }
    }

    suspend fun resetScreenGuide(screenId: String) {
        val invKey = booleanPreferencesKey("invitation_seen_$screenId")
        val compKey = booleanPreferencesKey("guide_completed_$screenId")
        context.guideDataStore.edit { prefs ->
            prefs.remove(invKey)
            prefs.remove(compKey)
        }
    }

    suspend fun resetAllGuides() {
        context.guideDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
