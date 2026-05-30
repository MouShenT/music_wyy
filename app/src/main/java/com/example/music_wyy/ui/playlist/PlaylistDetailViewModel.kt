package com.example.music_wyy.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class PlaylistDetailUiState(
    val name: String = "",
    val coverUrl: String? = null,
    val songs: List<SongItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class SongItem(
    val id: String,
    val name: String,
    val artists: String,
    val album: String,
    val duration: Int,
)

@Serializable
private data class PlaylistDetailResponse(
    val code: Int = -1,
    val playlist: PlaylistDetailData? = null,
)

@Serializable
private data class PlaylistDetailData(
    val name: String = "",
    val coverImgUrl: String? = null,
    val tracks: List<TrackItem> = emptyList(),
)

@Serializable
private data class TrackItem(
    val id: Long = 0,
    val name: String = "",
    val ar: List<ArtistItem> = emptyList(),
    val al: AlbumItem? = null,
    val dt: Int = 0,
)

@Serializable
private data class ArtistItem(val name: String = "")

@Serializable
private data class AlbumItem(val name: String = "")

class PlaylistDetailViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistDetailUiState())
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun loadPlaylist(playlistId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val cookie = cookieStore.cookie.first() ?: ""
                val response = api.getPlaylistDetail(playlistId, "MUSIC_U=$cookie")
                val body = response.string()
                val result = json.decodeFromString<PlaylistDetailResponse>(body)

                if (result.code == 200 && result.playlist != null) {
                    val playlist = result.playlist
                    val songs = playlist.tracks.map { track ->
                        SongItem(
                            id = track.id.toString(),
                            name = track.name,
                            artists = track.ar.joinToString(" / ") { it.name },
                            album = track.al?.name ?: "",
                            duration = track.dt,
                        )
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            name = playlist.name,
                            coverUrl = playlist.coverImgUrl,
                            songs = songs,
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "获取歌单详情失败 (code: ${result.code})") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.localizedMessage ?: "未知错误"}",
                    )
                }
            }
        }
    }
}
