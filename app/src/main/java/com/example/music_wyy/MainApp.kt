package com.example.music_wyy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.music_wyy.ui.navigation.NavGraph
import com.example.music_wyy.ui.navigation.Route
import com.example.music_wyy.ui.navigation.bottomNavItems
import com.example.music_wyy.ui.player.MiniPlayer
import com.example.music_wyy.ui.player.PlayerViewModel
import java.net.URLEncoder
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route } + Route.Profile.route
    val isPlayerOrLyricRoute = currentRoute == Route.Player.route || currentRoute?.startsWith("lyric") == true

    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val player = ExoPlayer.Builder(context).build()
        playerViewModel.setPlayer(player)
        onDispose { player.release() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavGraph(
                navController = navController,
                playerViewModel = playerViewModel,
                onNavigateToPlayer = { navController.navigate(Route.Player.route) },
            )

            // Mini Player — inside content area, above the bottom bar
            if (!isPlayerOrLyricRoute && playerState.currentSong != null) {
                MiniPlayer(
                    state = playerState,
                    onTogglePlay = { playerViewModel.togglePlay() },
                    onNext = { playerViewModel.next() },
                    onPrevious = { playerViewModel.previous() },
                    onClick = {
                        playerState.currentSong?.let { s ->
                            navController.navigate(
                                Route.Lyric.create(
                                    songId = s.id,
                                    songName = URLEncoder.encode(s.name, "UTF-8"),
                                    artist = URLEncoder.encode(s.artist, "UTF-8"),
                                )
                            )
                        }
                    },
                    onClose = { playerViewModel.stop() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
