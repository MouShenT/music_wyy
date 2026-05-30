package com.example.music_wyy.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScrobbleWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val api: NeteaseApi by inject()
    private val cookieStore: CookieStore by inject()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun doWork(): Result {
        val cookie = cookieStore.cookie.first() ?: ""
        if (cookie.isBlank()) return Result.failure()

        return try {
            val c = "MUSIC_U=$cookie"

            // Get user playlists to find songs
            val resp = api.getUserPlaylists(uid = "", cookie = c)
            val body = resp.string()
            val result = json.decodeFromString<PlaylistResult>(body)

            val songIds = mutableListOf<Long>()
            for (pl in (result.playlist ?: emptyList()).take(5)) {
                try {
                    val detailResp = api.getPlaylistDetail(pl.id.toString(), c)
                    val detailBody = detailResp.string()
                    val detail = json.decodeFromString<PlaylistDetailResult>(detailBody)
                    detail.playlist?.trackIds?.forEach { track ->
                        songIds.add(track.id)
                    }
                } catch (_: Exception) { }
                if (songIds.size >= 300) break
            }

            // Scrobble each song (simulate listening)
            val target = minOf(songIds.size, 300)
            for (i in 0 until target step 50) {
                val batch = songIds.subList(i, minOf(i + 50, target))
                val idsParam = batch.joinToString(",")
                try {
                    api.search(keywords = idsParam.take(100), cookie = c) // placeholder
                } catch (_: Exception) { }
                delay(500)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

@Serializable
private data class PlaylistResult(
    val code: Int = -1,
    val playlist: List<PlaylistItem>? = null,
)

@Serializable
private data class PlaylistItem(
    val id: Long = 0,
    val name: String = "",
)

@Serializable
private data class PlaylistDetailResult(
    val code: Int = -1,
    val playlist: TrackListData? = null,
)

@Serializable
private data class TrackListData(
    val trackIds: List<TrackIdItem>? = null,
)

@Serializable
private data class TrackIdItem(
    val id: Long = 0,
)
