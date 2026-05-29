package com.example.music_wyy.ui.home

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

data class HomeUiState(
    val nickname: String? = null,
    val totalPlaylists: Int = 0,
    val totalSongs: Int = 0,
    val isLoading: Boolean = false,
    val todaySigned: Boolean = false,
    val todayPoints: Int = 0,
)

@Serializable
private data class SigninResponse(val code: Int = -1, val point: Int? = null)
@Serializable
private data class UserPlaylistResponse(val more: Boolean = false, val playlist: List<PlaylistCount> = emptyList())
@Serializable
private data class PlaylistCount(val trackCount: Int = 0)

class HomeViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
    private val userSession: UserSession,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        val session = userSession.state.value
        if (session.isLoggedIn) {
            _state.update { it.copy(nickname = session.nickname) }
            loadOverview()
        }
    }

    fun loadOverview() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val cookie = cookieStore.cookie.first() ?: ""
                if (cookie.isBlank()) return@launch

                // Load playlists count
                val resp = api.getUserPlaylists("", "MUSIC_U=$cookie")
                val body = resp.string()
                val result = json.decodeFromString<UserPlaylistResponse>(body)
                _state.update {
                    it.copy(
                        totalPlaylists = result.playlist.size,
                        totalSongs = result.playlist.sumOf { p -> p.trackCount },
                    )
                }

                // Check signin status
                val signResp = api.dailySignin("MUSIC_U=$cookie")
                val signBody = signResp.string()
                val signResult = json.decodeFromString<SigninResponse>(signBody)
                if (signResult.code == -2) {
                    _state.update { it.copy(todaySigned = true) }
                } else if (signResult.code == 200) {
                    _state.update { it.copy(todaySigned = true, todayPoints = signResult.point ?: 0) }
                }
            } catch (_: Exception) {
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
