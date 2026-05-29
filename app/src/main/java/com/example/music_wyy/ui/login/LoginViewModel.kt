package com.example.music_wyy.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import com.example.music_wyy.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class LoginUiState(
    val cookie: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val nickname: String? = null,
    val avatarUrl: String? = null,
)

@Serializable
private data class LoginApiResponse(val data: LoginResponse? = null)

@Serializable
private data class LoginResponse(val code: Int = -1, val profile: ProfileInfo? = null)

@Serializable
data class ProfileInfo(val nickname: String = "", val avatarUrl: String = "")

class LoginViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
    private val userSession: UserSession,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        checkSavedCookie()
    }

    private fun checkSavedCookie() {
        viewModelScope.launch {
            cookieStore.cookie.collect { savedCookie ->
                if (!savedCookie.isNullOrBlank() && !_state.value.isLoggedIn) {
                    val stripped = savedCookie.removePrefix("MUSIC_U=").trim()
                    _state.update { it.copy(cookie = stripped) }
                    login(stripped)
                }
            }
        }
    }

    fun onCookieChange(cookie: String) {
        _state.update { it.copy(cookie = cookie, error = null) }
    }

    fun login(cookie: String? = null) {
        val raw = cookie ?: _state.value.cookie
        if (raw.isBlank()) return
        val c = raw.removePrefix("MUSIC_U=").trim()

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val response = api.checkLogin("MUSIC_U=$c")
                val body = response.string()
                val apiResp = json.decodeFromString<LoginApiResponse>(body)
                val result = apiResp.data

                if (result != null && result.code == 200 && result.profile != null) {
                    cookieStore.saveCookie(c)
                    userSession.setUser(result.profile.nickname, result.profile.avatarUrl)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            nickname = result.profile.nickname,
                            avatarUrl = result.profile.avatarUrl,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(isLoading = false, error = "Cookie 无效或已过期")
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "连接失败: ${e.localizedMessage ?: "未知错误"}",
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            cookieStore.clearCookie()
            userSession.clear()
            _state.update { LoginUiState() }
        }
    }
}
