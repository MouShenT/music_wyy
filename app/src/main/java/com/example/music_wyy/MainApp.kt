package com.example.music_wyy

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.music_wyy.ui.navigation.NavGraph

@Composable
fun MainApp() {
    Surface(
        color = MaterialTheme.colorScheme.background,
    ) {
        val navController = rememberNavController()
        NavGraph(navController = navController)
    }
}
