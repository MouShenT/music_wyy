package com.example.music_wyy.ui.lyric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.NeteaseRedLight
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary
import com.example.music_wyy.ui.theme.TextTertiary
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricScreen(
    songId: String,
    songName: String,
    artist: String,
    onBack: () -> Unit,
    viewModel: LyricViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(songId) { viewModel.loadLyric(songId, songName, artist) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        state.songName.ifBlank { "歌词" },
                        fontWeight = FontWeight.Bold, color = TextPrimary,
                        maxLines = 1,
                    )
                    if (state.artist.isNotBlank()) {
                        Text(state.artist, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleTranslation() }) {
                    Icon(
                        Icons.Filled.Translate, null,
                        tint = if (state.showTranslation) NeteaseRed else TextSecondary,
                    )
                }
                IconButton(onClick = { viewModel.exportLyric(context) }) {
                    Icon(Icons.Filled.CloudDownload, null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeteaseRed)
            }
        } else if (state.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error!!, color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LyricContent(
                lyricText = state.lyricText,
                tlyricText = state.tlyricText,
                showTranslation = state.showTranslation,
            )
        }
    }
}

@Composable
private fun LyricContent(
    lyricText: String,
    tlyricText: String,
    showTranslation: Boolean,
) {
    // 解析歌词（[mm:ss.xx]text 格式）
    data class LyricLine(val time: Int, val text: String, val transText: String = "")

    val transMap = tlyricText.lines()
        .mapNotNull { line ->
            val match = Regex("""\[(\d+):(\d+)[.:](\d+)]""").find(line)
            if (match != null) {
                val ms = match.groupValues[1].toInt() * 60000 + match.groupValues[2].toInt() * 1000 + match.groupValues[3].toInt() * 10
                val text = line.removeRange(match.range.first, match.range.last + 1).trim()
                ms to text
            } else null
        }
        .toMap()

    val lines = lyricText.lines().mapNotNull { line ->
        val match = Regex("""\[(\d+):(\d+)[.:](\d+)]""").find(line)
        if (match != null) {
            val ms = match.groupValues[1].toInt() * 60000 + match.groupValues[2].toInt() * 1000 + match.groupValues[3].toInt() * 10
            val text = line.removeRange(match.range.first, match.range.last + 1).trim()
            LyricLine(ms, text, transMap[ms] ?: "")
        } else null
    }.sortedBy { it.time }

    if (lines.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无歌词", color = TextSecondary, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(lines, key = { i, _ -> i }) { _, line ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    line.text.ifBlank { "♪" },
                    color = if (line.text.isNotBlank()) TextPrimary else TextTertiary,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                )
                if (showTranslation && line.transText.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        line.transText,
                        color = NeteaseRedLight,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
