package com.example.music_wyy.ui.lyric

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_wyy.ui.player.PlayerViewModel
import com.example.music_wyy.ui.player.PlayingSong
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary
import com.example.music_wyy.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

data class ParsedLine(
    val timeMs: Int,
    val text: String,
    val transText: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricScreen(
    songId: String,
    songName: String,
    artist: String,
    album: String = "",
    coverUrl: String? = null,
    onBack: () -> Unit,
    lyricViewModel: LyricViewModel = koinViewModel(),
    playerViewModel: PlayerViewModel = koinViewModel(),
) {
    val lyricState by lyricViewModel.state.collectAsStateWithLifecycle()
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Load lyrics and start playback on entry
    LaunchedEffect(songId) {
        lyricViewModel.loadLyric(songId, songName, artist)
        // If not already playing this song, start it
        if (playerState.currentSong?.id != songId) {
            playerViewModel.playSong(
                PlayingSong(
                    id = songId,
                    name = songName,
                    artist = artist,
                    album = album,
                    coverUrl = coverUrl,
                )
            )
        }
    }

    // Parse lyrics
    val lines = remember(lyricState.lyricText, lyricState.tlyricText) {
        parseLyrics(lyricState.lyricText, lyricState.tlyricText)
    }

    // Find current line index from playback position
    val currentLineIndex by remember {
        derivedStateOf {
            if (lines.isEmpty()) -1
            else {
                val pos = playerState.position.toInt()
                lines.indexOfLast { it.timeMs <= pos }
            }
        }
    }

    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && lines.isNotEmpty()) {
            listState.animateScrollToItem(
                index = (currentLineIndex - 2).coerceAtLeast(0),
                scrollOffset = 0,
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Top bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        songName.ifBlank { "歌词" },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (artist.isNotBlank()) {
                        Text(artist, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
            },
            actions = {
                IconButton(onClick = { lyricViewModel.toggleTranslation() }) {
                    Icon(
                        Icons.Filled.Translate, null,
                        tint = if (lyricState.showTranslation) NeteaseRed else TextSecondary,
                    )
                }
                IconButton(onClick = { lyricViewModel.exportLyric(context) }) {
                    Icon(Icons.Filled.CloudDownload, null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        // Content area
        if (lyricState.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeteaseRed)
            }
        } else if (lines.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.MusicNote, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(lyricState.error ?: "暂无歌词", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            // Lyrics with auto-scroll
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Top spacer so first lyric can reach center
                item { Spacer(Modifier.height(80.dp)) }

                itemsIndexed(lines, key = { i, _ -> i }) { i, line ->
                    val isCurrent = i == currentLineIndex
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            line.text.ifBlank { "♪" },
                            color = if (isCurrent) NeteaseRed else TextPrimary,
                            fontSize = if (isCurrent) 18.sp else 15.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                        )
                        if (lyricState.showTranslation && line.transText.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                line.transText,
                                color = if (isCurrent) NeteaseRed.copy(alpha = 0.8f) else TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                // Bottom spacer
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        // Mini player bar at bottom
        if (playerState.currentSong != null) {
            val progress = if (playerState.duration > 0) {
                playerState.position.toFloat() / playerState.duration.toFloat()
            } else 0f

            Column(modifier = Modifier.fillMaxWidth().background(BackgroundDark)) {
                // Slim progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = NeteaseRed,
                    trackColor = CardDark,
                    strokeCap = StrokeCap.Round,
                )

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Song info
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    ) {
                        Text(
                            playerState.currentSong?.name ?: "",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            playerState.currentSong?.artist ?: "",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Time
                    Text(
                        formatMs(playerState.position) + " / " + formatMs(playerState.duration),
                        color = TextTertiary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(end = 8.dp),
                    )

                    // Playback controls
                    IconButton(onClick = { playerViewModel.previous() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.SkipPrevious, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                    }
                    IconButton(
                        onClick = { playerViewModel.togglePlay() },
                        modifier = Modifier.size(40.dp).background(NeteaseRed, CircleShape),
                    ) {
                        if (playerState.isLoading) {
                            CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null,
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    IconButton(onClick = { playerViewModel.next() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.SkipNext, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
                    }

                    // Download
                    IconButton(onClick = { playerViewModel.downloadCurrentSong() }, modifier = Modifier.size(36.dp)) {
                        if (playerState.isDownloading) {
                            CircularProgressIndicator(color = NeteaseRed, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Download, "下载", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun parseLyrics(lyricText: String, tlyricText: String): List<ParsedLine> {
    val timePattern = Regex("""\[(\d+):(\d+)[.:](\d+)]""")

    val transMap = tlyricText.lines().mapNotNull { line ->
        val match = timePattern.find(line) ?: return@mapNotNull null
        val ms = match.groupValues[1].toInt() * 60000 + match.groupValues[2].toInt() * 1000 + match.groupValues[3].toInt() * 10
        val text = line.removeRange(match.range.first, match.range.last + 1).trim()
        ms to text
    }.toMap()

    return lyricText.lines().mapNotNull { line ->
        val match = timePattern.find(line) ?: return@mapNotNull null
        val ms = match.groupValues[1].toInt() * 60000 + match.groupValues[2].toInt() * 1000 + match.groupValues[3].toInt() * 10
        val text = line.removeRange(match.range.first, match.range.last + 1).trim()
        ParsedLine(ms, text, transMap[ms] ?: "")
    }.sortedBy { it.timeMs }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
