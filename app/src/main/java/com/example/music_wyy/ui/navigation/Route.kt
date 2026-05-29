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
    data object PlaylistDetail : Route("playlist/{playlistId}") {
        fun create(playlistId: String) = "playlist/$playlistId"
    }
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
