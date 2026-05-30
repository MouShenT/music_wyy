package com.example.music_wyy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val route: String) {
    data object Login : Route("login")
    data object Home : Route("home")
    data object Playlists : Route("playlists")
    data object Automation : Route("automation")
    data object Profile : Route("profile")
    data object BatchCreate : Route("batch_create")
    data object AiSearch : Route("ai_search")
    data object PlaylistDetail : Route("playlist/{playlistId}") {
        fun create(playlistId: String) = "playlist/$playlistId"
    }
    data object Lyric : Route("lyric/{songId}/{songName}/{artist}") {
        fun create(songId: String, songName: String, artist: String) = "lyric/$songId/$songName/$artist"
    }
    data object Messages : Route("messages")
    data object MessageDetail : Route("messages/{uid}/{nickname}") {
        fun create(uid: String, nickname: String) = "messages/$uid/$nickname"
    }
    data object Yunbei : Route("yunbei")
    data object Player : Route("player")
    data object Settings : Route("settings")
}

data class BottomNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem("首页", Route.Home.route, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem("歌单", Route.Playlists.route, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    BottomNavItem("签到", Route.Automation.route, Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt),
    BottomNavItem("我的", Route.Profile.route, Icons.Filled.Person, Icons.Outlined.Person),
)
