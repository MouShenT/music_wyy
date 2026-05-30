package com.example.music_wyy.ui.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentSong: PlayingSong? = null,
    val position: Long = 0,
    val duration: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val playlist: List<PlayingSong> = emptyList(),
    val playMode: PlayMode = PlayMode.LIST,
)

data class PlayingSong(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
    val url: String? = null,
    val duration: Int = 0,
)

enum class PlayMode { SINGLE, LIST, SHUFFLE }

class PlayerViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun setPlayer(player: ExoPlayer) {
        exoPlayer = player
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }
            override fun onPlaybackStateChanged(state: Int) {
                _state.update { it.copy(isLoading = state == Player.STATE_BUFFERING) }
                if (state == Player.STATE_ENDED) next()
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                _state.update { it.copy(position = player.currentPosition) }
            }
        })
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _state.update { it.copy(currentSong = null, isPlaying = false, position = 0, duration = 0) }
    }

    fun playSong(song: PlayingSong) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                var url = song.url
                if (url == null) {
                    val cookie = cookieStore.cookie.first() ?: ""
                    val resp = api.getSongUrl(song.id, cookie = "MUSIC_U=$cookie")
                    val body = resp.string()
                    val result = json.decodeFromString<SongUrlResponse>(body)
                    url = result.data?.firstOrNull()?.url
                }

                if (url == null) {
                    _state.update { it.copy(isLoading = false, error = "该歌曲暂无播放资源") }
                    return@launch
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId(song.id)
                    .setUri(Uri.parse(url))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.name)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(if (song.coverUrl != null) Uri.parse(song.coverUrl) else null)
                            .build()
                    )
                    .build()

                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()

                val playlist = _state.value.playlist.toMutableList()
                if (playlist.none { it.id == song.id }) {
                    playlist.add(song)
                }
                _state.update {
                    it.copy(
                        currentSong = song.copy(url = url),
                        isLoading = false,
                        playlist = playlist,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "播放失败: ${e.localizedMessage}") }
            }
        }
    }

    fun addToPlaylist(song: PlayingSong) {
        _state.update { state ->
            val list = state.playlist.toMutableList()
            if (list.none { it.id == song.id }) {
                list.add(song)
            }
            state.copy(playlist = list)
        }
    }

    fun togglePlay() {
        val player = exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        val s = _state.value
        if (s.playlist.isEmpty()) return
        val idx = s.playlist.indexOfFirst { it.id == s.currentSong?.id }
        val nextIdx = when (s.playMode) {
            PlayMode.SINGLE -> idx.coerceAtLeast(0)
            PlayMode.LIST -> (idx + 1) % s.playlist.size
            PlayMode.SHUFFLE -> {
                if (s.playlist.size <= 1) 0
                else {
                    val others = s.playlist.indices.filter { it != idx }
                    others[others.indices.random()]
                }
            }
        }
        s.playlist.getOrNull(nextIdx)?.let { playSong(it) }
    }

    fun previous() {
        val s = _state.value
        if (s.playlist.isEmpty()) return
        val idx = s.playlist.indexOfFirst { it.id == s.currentSong?.id }
        val prevIdx = if (idx <= 0) s.playlist.size - 1 else idx - 1
        s.playlist.getOrNull(prevIdx)?.let { playSong(it) }
    }

    fun cyclePlayMode() {
        _state.update {
            it.copy(playMode = when (it.playMode) {
                PlayMode.LIST -> PlayMode.SHUFFLE
                PlayMode.SHUFFLE -> PlayMode.SINGLE
                PlayMode.SINGLE -> PlayMode.LIST
            })
        }
    }

    fun seekTo(pos: Long) {
        exoPlayer?.seekTo(pos)
    }

    fun removeFromPlaylist(songId: String) {
        _state.update { state ->
            state.copy(playlist = state.playlist.filter { it.id != songId })
        }
    }

    override fun onCleared() {
        exoPlayer?.release()
        exoPlayer = null
    }
}

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        mediaSession?.player?.stop()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession!!
}

@Serializable
private data class SongUrlResponse(
    val code: Int = -1,
    val data: List<SongUrlData>? = null,
)

@Serializable
private data class SongUrlData(
    val id: Long = 0,
    val url: String? = null,
    val time: Int = 0,
)
