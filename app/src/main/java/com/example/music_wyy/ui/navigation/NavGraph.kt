package com.example.music_wyy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.music_wyy.ui.ai.AiSearchScreen
import com.example.music_wyy.ui.home.HomeScreen
import com.example.music_wyy.ui.login.LoginScreen
import com.example.music_wyy.ui.lyric.LyricScreen
import com.example.music_wyy.ui.message.MsgListScreen
import com.example.music_wyy.ui.message.MsgDetailScreen
import com.example.music_wyy.ui.player.PlayerScreen
import com.example.music_wyy.ui.player.PlayerViewModel
import com.example.music_wyy.ui.playlist.PlaylistScreen
import com.example.music_wyy.ui.playlist.PlaylistDetailScreen
import com.example.music_wyy.ui.playlist.BatchCreateScreen
import com.example.music_wyy.ui.automation.AutomationScreen
import com.example.music_wyy.ui.profile.ProfileScreen
import com.example.music_wyy.ui.settings.SettingsScreen
import com.example.music_wyy.ui.yunbei.YunbeiScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun NavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToPlayer: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Route.Login.route,
        modifier = modifier,
    ) {
        composable(Route.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Route.Home.route) {
            HomeScreen(
                playerViewModel = playerViewModel,
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate(Route.PlaylistDetail.create(playlistId))
                },
                onNavigateToYunbei = { navController.navigate(Route.Yunbei.route) },
                onNavigateToMessages = { navController.navigate(Route.Messages.route) },
            )
        }
        composable(Route.Playlists.route) {
            PlaylistScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Route.PlaylistDetail.create(playlistId))
                },
                onBatchCreate = {
                    navController.navigate(Route.BatchCreate.route)
                },
            )
        }
        composable(Route.BatchCreate.route) {
            BatchCreateScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.AiSearch.route) {
            AiSearchScreen(
                onBack = { navController.popBackStack() },
                onFillBatchCreate = { text ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("aiResult", text)
                    navController.popBackStack()
                },
            )
        }
        composable(Route.Automation.route) {
            AutomationScreen()
        }
        composable(Route.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToYunbei = { navController.navigate(Route.Yunbei.route) },
                onNavigateToMessages = { navController.navigate(Route.Messages.route) },
                onNavigateToSettings = { navController.navigate(Route.Settings.route) },
            )
        }
        composable(
            route = Route.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
                onSongClick = { songId, songName, artist ->
                    navController.navigate(
                        Route.Lyric.create(
                            songId = songId,
                            songName = URLEncoder.encode(songName, "UTF-8"),
                            artist = URLEncoder.encode(artist, "UTF-8"),
                        )
                    )
                },
                onPlaySong = { songs, coverUrl, startSongId ->
                    val playingSongs = songs.map { song ->
                        com.example.music_wyy.ui.player.PlayingSong(
                            id = song.id,
                            name = song.name,
                            artist = song.artists,
                            album = song.album,
                            coverUrl = coverUrl,
                        )
                    }
                    playerViewModel.playPlaylist(playingSongs, startSongId)
                    val song = songs.find { it.id == startSongId }
                    if (song != null) {
                        navController.navigate(
                            Route.Lyric.create(
                                songId = startSongId,
                                songName = URLEncoder.encode(song.name, "UTF-8"),
                                artist = URLEncoder.encode(song.artists, "UTF-8"),
                            )
                        )
                    }
                },
            )
        }
        composable(
            route = Route.Lyric.route,
            arguments = listOf(
                navArgument("songId") { type = NavType.StringType },
                navArgument("songName") { type = NavType.StringType },
                navArgument("artist") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getString("songId") ?: return@composable
            val songName = URLDecoder.decode(backStackEntry.arguments?.getString("songName") ?: "", "UTF-8")
            val artist = URLDecoder.decode(backStackEntry.arguments?.getString("artist") ?: "", "UTF-8")
            // Get album & coverUrl from PlayerViewModel (set by onPlaySong before navigation)
            val currentSong = playerViewModel.state.value.currentSong
            val album = if (currentSong?.id == songId) currentSong.album else ""
            val coverUrl = if (currentSong?.id == songId) currentSong.coverUrl else null
            LyricScreen(
                songId = songId,
                songName = songName,
                artist = artist,
                album = album,
                coverUrl = coverUrl,
                onBack = { navController.popBackStack() },
                playerViewModel = playerViewModel,
            )
        }
        composable(Route.Messages.route) {
            MsgListScreen(
                onBack = { navController.popBackStack() },
                onConversationClick = { uid, nickname ->
                    navController.navigate(Route.MessageDetail.create(uid, nickname))
                },
            )
        }
        composable(
            route = Route.MessageDetail.route,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("nickname") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
            val nickname = URLDecoder.decode(backStackEntry.arguments?.getString("nickname") ?: "", "UTF-8")
            MsgDetailScreen(
                uid = uid,
                nickname = nickname,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Route.Yunbei.route) {
            YunbeiScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Player.route) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
        composable(Route.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
