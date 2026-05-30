package com.example.music_wyy.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UserSessionState(
    val isLoggedIn: Boolean = false,
    val userId: Long = 0,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val todaySigned: Boolean = false,
    val todayPoints: Int = 0,
)

class UserSession {
    private val _state = MutableStateFlow(UserSessionState())
    val state: StateFlow<UserSessionState> = _state.asStateFlow()

    fun setUser(userId: Long, nickname: String, avatarUrl: String) {
        _state.update {
            UserSessionState(isLoggedIn = true, userId = userId, nickname = nickname, avatarUrl = avatarUrl)
        }
    }

    fun setSignedIn(signed: Boolean, points: Int = 0) {
        _state.update { it.copy(todaySigned = signed, todayPoints = points) }
    }

    fun clear() {
        _state.update { UserSessionState() }
    }
}
