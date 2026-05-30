package com.example.music_wyy.data.local

import android.content.Context
import com.example.music_wyy.data.local.datastore.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class SongCache(
    private val context: Context,
    private val settingsStore: SettingsStore,
) {
    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun hashKey(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private suspend fun cacheDir(): File {
        val dir = settingsStore.cacheDir.first()
        return File(dir).also { if (!it.exists()) it.mkdirs() }
    }

    suspend fun downloadDir(): File {
        val dir = settingsStore.downloadDir.first()
        return File(dir).also { if (!it.exists()) it.mkdirs() }
    }

    fun getCachedFile(songId: String): File? {
        val dir = File(context.cacheDir, "song_cache")
        if (!dir.exists()) return null
        val cached = dir.resolve(hashKey(songId))
        return if (cached.exists()) cached else null
    }

    suspend fun cacheSong(songId: String, url: String): File? = withContext(Dispatchers.IO) {
        try {
            val dir = cacheDir()
            val file = dir.resolve(hashKey(songId))
            if (file.exists()) {
                file.setLastModified(System.currentTimeMillis())
                return@withContext file
            }

            // Evict old files before downloading
            evictIfNeeded(dir, file.length())

            val request = Request.Builder().url(url).build()
            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            response.body?.source()?.let { source ->
                file.sink().buffer().use { sink ->
                    sink.writeAll(source)
                    sink.flush()
                }
            }
            response.close()

            if (file.exists() && file.length() > 0) file else null
        } catch (e: IOException) {
            null
        }
    }

    suspend fun downloadSong(songName: String, artist: String, url: String): File? = withContext(Dispatchers.IO) {
        try {
            val dir = downloadDir()
            val safeName = "$artist - $songName".replace(Regex("[/\\\\:*?\"<>|]"), "_")
            val file = dir.resolve("$safeName.mp3")

            // Skip if already exists
            if (file.exists()) return@withContext file

            val request = Request.Builder().url(url).build()
            val response = downloadClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            response.body?.source()?.let { source ->
                file.sink().buffer().use { sink ->
                    sink.writeAll(source)
                    sink.flush()
                }
            }
            response.close()

            if (file.exists() && file.length() > 0) file else null
        } catch (e: IOException) {
            null
        }
    }

    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "song_cache")
        if (!dir.exists()) return@withContext 0L
        dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "song_cache")
        if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
    }

    private fun evictIfNeeded(dir: File, incomingSize: Long) {
        val maxBytes = runCatching {
            kotlinx.coroutines.runBlocking { settingsStore.maxCacheBytes.first() }
        }.getOrDefault(SettingsStore.DEFAULT_MAX_CACHE)

        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() } + incomingSize

        for (f in files) {
            if (total <= maxBytes) break
            total -= f.length()
            f.delete()
        }
    }
}
