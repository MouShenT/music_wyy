package com.example.music_wyy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.music_wyy.ui.login.LoginScreen
import com.example.music_wyy.ui.playlist.PlaylistScreen
import com.example.music_wyy.ui.playlist.PlaylistDetailScreen
import com.example.music_wyy.ui.playlist.BatchCreateScreen
import com.example.music_wyy.ui.automation.AutomationScreen
import com.example.music_wyy.ui.profile.ProfileScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Login.route,
        modifier = modifier,
    ) {
        composable(Route.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Playlists.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                },
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
        composable(Route.Automation.route) {
            AutomationScreen()
        }
        composable(Route.Profile.route) {
            ProfileScreen(onLogout = {
                navController.navigate(Route.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
        composable(
            route = Route.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
