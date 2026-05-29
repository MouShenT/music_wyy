package com.example.music_wyy.ui.navigation

sealed class Route(val route: String) {
    data object Login : Route("login")
    data object Playlists : Route("playlists")
    data object PlaylistDetail : Route("playlist/{playlistId}") {
        fun create(playlistId: String) = "playlist/$playlistId"
    }
    data object Automation : Route("automation")
    data object Analytics : Route("analytics")
    data object Settings : Route("settings")
}
