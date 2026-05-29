package com.example.music_wyy.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cookies")

class CookieStore(
    private val context: Context,
) {
    private val cookieKey = stringPreferencesKey("music_u_cookie")

    val cookie: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[cookieKey]
    }

    suspend fun saveCookie(cookie: String) {
        context.dataStore.edit { prefs ->
            prefs[cookieKey] = cookie
        }
    }

    suspend fun clearCookie() {
        context.dataStore.edit { it.remove(cookieKey) }
    }
}
