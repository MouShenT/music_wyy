package com.example.music_wyy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.session.UserSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val cookieStore: CookieStore,
    private val userSession: UserSession,
) : ViewModel() {

    val sessionState: StateFlow<com.example.music_wyy.session.UserSessionState> = userSession.state

    fun logout() {
        viewModelScope.launch {
            cookieStore.clearCookie()
            userSession.clear()
        }
    }
}
