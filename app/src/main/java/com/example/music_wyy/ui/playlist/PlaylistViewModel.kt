package com.example.music_wyy.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import com.example.music_wyy.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class PlaylistUiState(
    val playlists: List<PlaylistItem> = emptyList(),
    val totalPlaylists: Int = 0,
    val totalSongs: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class PlaylistItem(
    val id: String,
    val name: String,
    val songCount: Int,
    val coverUrl: String?,
    val creator: String?,
)

@Serializable
data class PlaylistApiItem(
    val id: Long = 0,
    val name: String = "",
    val trackCount: Int = 0,
    val coverImgUrl: String? = null,
    val creator: PlaylistCreator? = null,
)

@Serializable
data class PlaylistCreator(val nickname: String = "")

@Serializable
private data class UserPlaylistResponse(
    val more: Boolean = false,
    val playlist: List<PlaylistApiItem> = emptyList(),
)

class PlaylistViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
    private val userSession: UserSession,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaylistUiState())
    val state: StateFlow<PlaylistUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val cookie = cookieStore.cookie.first() ?: ""
                if (cookie.isBlank()) {
                    _state.update { it.copy(isLoading = false, error = "请先登录") }
                    return@launch
                }

                val uid = userSession.state.value.userId.toString()
                val response = api.getUserPlaylists(uid, "MUSIC_U=$cookie")
                val body = response.string()
                val result = json.decodeFromString<UserPlaylistResponse>(body)

                val playlists = result.playlist.map { item ->
                    PlaylistItem(
                        id = item.id.toString(),
                        name = item.name,
                        songCount = item.trackCount,
                        coverUrl = item.coverImgUrl,
                        creator = item.creator?.nickname,
                    )
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        playlists = playlists,
                        totalPlaylists = playlists.size,
                        totalSongs = playlists.sumOf { p -> p.songCount },
                    )
                }
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
