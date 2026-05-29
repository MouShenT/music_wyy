package com.example.music_wyy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.music_wyy.ui.login.LoginScreen
import com.example.music_wyy.ui.playlist.PlaylistScreen
import com.example.music_wyy.ui.automation.AutomationScreen
import com.example.music_wyy.ui.analytics.AnalyticsScreen
import com.example.music_wyy.ui.settings.SettingsScreen

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
            LoginScreen(onLoginSuccess = {
                navController.navigate(Route.Playlists.route) {
                    popUpTo(Route.Login.route) { inclusive = true }
                }
            })
        }
        composable(Route.Playlists.route) {
            PlaylistScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Route.PlaylistDetail.create(playlistId))
                },
            )
        }
        composable(Route.Automation.route) {
            AutomationScreen()
        }
        composable(Route.Analytics.route) {
            AnalyticsScreen()
        }
        composable(Route.Settings.route) {
            SettingsScreen()
        }
    }
}
