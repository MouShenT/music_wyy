package com.example.music_wyy.ui.lyric

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_wyy.ui.player.PlayerViewModel
import com.example.music_wyy.ui.player.PlayMode
import com.example.music_wyy.ui.player.PlayingSong
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary
import com.example.music_wyy.ui.theme.TextTertiary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.abs

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

    // Display info from player state (reacts to next/prev), fallback to nav params on initial entry
    val currentId = playerState.currentSong?.id ?: songId
    val displayName = playerState.currentSong?.name ?: songName
    val displayArtist = playerState.currentSong?.artist ?: artist

    // Load lyrics whenever the current song changes (handles next/prev in playlist)
    LaunchedEffect(currentId) {
        lyricViewModel.loadLyric(currentId, displayName, displayArtist)
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

    // Track programmatic vs user-initiated scrolls
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var userScrolledAway by remember { mutableStateOf(false) }
    var autoReturnJob by remember { mutableStateOf<Job?>(null) }
    var lastScrolledLine by remember { mutableStateOf(-1) }

    // Detect user manually scrolling: isScrollInProgress without programmatic flag
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isProgrammaticScroll) {
            userScrolledAway = true
            autoReturnJob?.cancel()
        } else if (!listState.isScrollInProgress && userScrolledAway) {
            autoReturnJob = scope.launch {
                delay(3000)
                userScrolledAway = false
            }
        }
    }

    // Auto-scroll lyrics: poll position every 250ms and snap to current line
    LaunchedEffect(lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        lastScrolledLine = -1
        while (true) {
            delay(250)
            if (userScrolledAway) continue
            val idx = lines.indexOfLast { it.timeMs <= playerState.position.toInt() }
            if (idx >= 0 && idx != lastScrolledLine) {
                lastScrolledLine = idx
                isProgrammaticScroll = true
                try {
                    val target = (idx - 3).coerceAtLeast(0) + 1
                    listState.scrollToItem(index = target, scrollOffset = 0)
                } finally {
                    isProgrammaticScroll = false
                }
            }
        }
    }

    // Immersive mode: tap lyrics area to toggle bars
    var isImmersive by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Top bar — hides in immersive mode
        AnimatedVisibility(
            visible = !isImmersive,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            displayName.ifBlank { "歌词" },
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (displayArtist.isNotBlank()) {
                            Text(displayArtist, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        }

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
            // Lyrics area with tap-to-toggle immersive mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { isImmersive = !isImmersive }
                    },
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { Spacer(Modifier.height(80.dp)) }

                    itemsIndexed(lines, key = { i, _ -> i }) { i, line ->
                        val isCurrent = i == currentLineIndex
                        val distance = abs(i - currentLineIndex)
                        val alpha = when {
                            isCurrent -> 1f
                            distance <= 2 -> 0.55f
                            distance <= 4 -> 0.35f
                            distance <= 7 -> 0.2f
                            else -> 0.12f
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                line.text.ifBlank { "♪" },
                                color = if (isCurrent) TextPrimary else TextPrimary.copy(alpha = alpha),
                                fontSize = if (isCurrent) 22.sp else 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isCurrent) 32.sp else 24.sp,
                            )
                            if (lyricState.showTranslation && line.transText.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    line.transText,
                                    color = if (isCurrent) TextPrimary.copy(alpha = 0.8f) else TextSecondary.copy(alpha = alpha),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // Bottom player bar — hides in immersive mode
        AnimatedVisibility(
            visible = !isImmersive && playerState.currentSong != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        ) {
            val progress = if (playerState.duration > 0) {
                playerState.position.toFloat() / playerState.duration.toFloat()
            } else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark)
                    .padding(top = 8.dp),
            ) {
                // Seekable progress slider
                Slider(
                    value = progress,
                    onValueChange = { ratio ->
                        playerViewModel.seekTo((ratio * playerState.duration).toLong())
                    },
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = NeteaseRed,
                        activeTrackColor = NeteaseRed,
                        inactiveTrackColor = CardDark,
                    ),
                )

                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatMs(playerState.position), color = TextTertiary, fontSize = 11.sp)
                    Text(formatMs(playerState.duration), color = TextTertiary, fontSize = 11.sp)
                }

                Spacer(Modifier.height(4.dp))

                // Song info + controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Song info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            playerState.currentSong?.name ?: "",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            playerState.currentSong?.artist ?: "",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Play mode toggle
                    val modeIcon: ImageVector = when (playerState.playMode) {
                        PlayMode.LIST -> Icons.Filled.Repeat
                        PlayMode.SHUFFLE -> Icons.Filled.Shuffle
                        PlayMode.SINGLE -> Icons.Filled.RepeatOne
                    }
                    val modeTint = when (playerState.playMode) {
                        PlayMode.LIST -> TextSecondary
                        PlayMode.SHUFFLE -> NeteaseRed
                        PlayMode.SINGLE -> NeteaseRed
                    }
                    IconButton(
                        onClick = { playerViewModel.cyclePlayMode() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(modeIcon, "播放模式", tint = modeTint, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    // Prev
                    IconButton(
                        onClick = { playerViewModel.previous() },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Filled.SkipPrevious, null, tint = TextPrimary, modifier = Modifier.size(28.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    // Play / Pause
                    IconButton(
                        onClick = { playerViewModel.togglePlay() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(NeteaseRed, CircleShape),
                    ) {
                        if (playerState.isLoading) {
                            CircularProgressIndicator(
                                color = TextPrimary,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null,
                                tint = TextPrimary,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Next
                    IconButton(
                        onClick = { playerViewModel.next() },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Filled.SkipNext, null, tint = TextPrimary, modifier = Modifier.size(28.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    // Download
                    IconButton(
                        onClick = { playerViewModel.downloadCurrentSong() },
                        modifier = Modifier.size(40.dp),
                    ) {
                        if (playerState.isDownloading) {
                            CircularProgressIndicator(
                                color = NeteaseRed,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.Download, "下载", tint = TextSecondary, modifier = Modifier.size(22.dp))
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
