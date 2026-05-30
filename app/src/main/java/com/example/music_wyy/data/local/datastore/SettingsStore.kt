package com.example.music_wyy.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val cacheDirKey = stringPreferencesKey("cache_dir")
    private val downloadDirKey = stringPreferencesKey("download_dir")
    private val maxCacheSizeKey = longPreferencesKey("max_cache_bytes")

    val cacheDir: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[cacheDirKey] ?: defaultCacheDir()
    }

    val downloadDir: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[downloadDirKey] ?: defaultDownloadDir()
    }

    val maxCacheBytes: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[maxCacheSizeKey] ?: DEFAULT_MAX_CACHE
    }

    suspend fun setCacheDir(path: String) {
        context.settingsDataStore.edit { it[cacheDirKey] = path }
    }

    suspend fun setDownloadDir(path: String) {
        context.settingsDataStore.edit { it[downloadDirKey] = path }
    }

    suspend fun setMaxCacheBytes(bytes: Long) {
        context.settingsDataStore.edit { it[maxCacheSizeKey] = bytes }
    }

    private fun defaultCacheDir(): String {
        return context.cacheDir.resolve("song_cache").absolutePath
    }

    private fun defaultDownloadDir(): String {
        return android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_MUSIC
        ).absolutePath
    }

    companion object {
        const val DEFAULT_MAX_CACHE = 500L * 1024 * 1024 // 500MB
    }
}
