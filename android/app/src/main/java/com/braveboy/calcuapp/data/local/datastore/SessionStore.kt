package com.braveboy.calcuapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * The only place the signed-in session lives on the device.
 *
 * Being "signed in" means holding a token — not having picked a business. The
 * two were previously conflated, which let a person reach the app without ever
 * authenticating.
 *
 * No identity is cached here. The person's name always comes from the profile
 * endpoint, so a stale name cannot survive a sign-out or an account switch.
 */
class SessionStore(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val accessToken: Flow<String> = context.sessionDataStore.data.map { it[Keys.ACCESS_TOKEN] ?: "" }

    val language: Flow<String> = context.sessionDataStore.data.map { it[Keys.LANGUAGE] ?: "fa" }

    val isSignedIn: Flow<Boolean> = accessToken.map { it.isNotEmpty() }

    suspend fun currentToken(): String = accessToken.first()

    suspend fun currentLanguage(): String = language.first()

    suspend fun save(token: String) {
        context.sessionDataStore.edit { prefs -> prefs[Keys.ACCESS_TOKEN] = token }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { prefs -> prefs.remove(Keys.ACCESS_TOKEN) }
    }

    suspend fun setLanguage(value: String) {
        context.sessionDataStore.edit { prefs -> prefs[Keys.LANGUAGE] = value }
    }

    /** Read synchronously for the OkHttp interceptor, which is not a coroutine. */
    @Volatile
    private var cachedToken: String = ""

    suspend fun warmUp() {
        cachedToken = currentToken()
    }

    fun tokenSnapshot(): String = cachedToken

    fun updateCachedToken(token: String) {
        cachedToken = token
    }
}
