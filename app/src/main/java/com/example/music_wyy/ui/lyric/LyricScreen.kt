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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.example.music_wyy.ui.theme.Black
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
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

    val currentId = playerState.currentSong?.id ?: songId
    val displayName = playerState.currentSong?.name ?: songName
    val displayArtist = playerState.currentSong?.artist ?: artist

    LaunchedEffect(currentId) {
        lyricViewModel.loadLyric(currentId, displayName, displayArtist)
    }

    val lines = remember(lyricState.lyricText, lyricState.tlyricText) {
        parseLyrics(lyricState.lyricText, lyricState.tlyricText)
    }

    var currentLineIndex by remember { mutableStateOf(-1) }

    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var userScrolledAway by remember { mutableStateOf(false) }
    var autoReturnJob by remember { mutableStateOf<Job?>(null) }
    var scrollAnimJob by remember { mutableStateOf<Job?>(null) }

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

    LaunchedEffect(lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        playerViewModel.state.collect { s ->
            val idx = lines.indexOfLast { it.timeMs <= s.position.toInt() }
            if (idx != currentLineIndex) {
                currentLineIndex = idx
            }
        }
    }

    LaunchedEffect(lines) {
        if (lines.isEmpty()) return@LaunchedEffect
        snapshotFlow { currentLineIndex }
            .filter { it >= 0 }
            .collect { idx ->
                if (!userScrolledAway) {
                    scrollAnimJob?.cancel()
                    isProgrammaticScroll = true
                    scrollAnimJob = scope.launch {
                        try {
                            val viewportH = listState.layoutInfo.viewportSize.height
                            val centerOffset = -(viewportH / 3)
                            listState.animateScrollToItem(
                                index = (idx + 1).coerceAtLeast(0),
                                scrollOffset = centerOffset,
                            )
                        } finally {
                            isProgrammaticScroll = false
                        }
                    }
                }
            }
    }

    var isImmersive by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Black)) {
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
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (displayArtist.isNotBlank()) {
                            Text(displayArtist, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { lyricViewModel.toggleTranslation() }) {
                        Icon(
                            Icons.Filled.Translate, null,
                            tint = if (lyricState.showTranslation)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { lyricViewModel.exportLyric(context) }) {
                        Icon(Icons.Filled.CloudDownload, null,
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Black),
            )
        }

        if (lyricState.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (lines.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.MusicNote, null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(lyricState.error ?: "暂无歌词",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp)
                }
            }
        } else {
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
                                color = if (isCurrent) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                                fontSize = if (isCurrent) 22.sp else 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                lineHeight = if (isCurrent) 32.sp else 24.sp,
                            )
                            if (lyricState.showTranslation && line.transText.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    line.transText,
                                    color = if (isCurrent)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
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

        // Bottom player bar
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
                    .background(Black)
                    .padding(top = 8.dp),
            ) {
                Slider(
                    value = progress,
                    onValueChange = { ratio ->
                        playerViewModel.seekTo((ratio * playerState.duration).toLong())
                    },
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatMs(playerState.position),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp)
                    Text(formatMs(playerState.duration),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp)
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            playerState.currentSong?.name ?: "",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            playerState.currentSong?.artist ?: "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    val modeIcon: ImageVector = when (playerState.playMode) {
                        PlayMode.LIST -> Icons.Filled.Repeat
                        PlayMode.SHUFFLE -> Icons.Filled.Shuffle
                        PlayMode.SINGLE -> Icons.Filled.RepeatOne
                    }
                    val modeTint = when (playerState.playMode) {
                        PlayMode.LIST -> MaterialTheme.colorScheme.onSurfaceVariant
                        PlayMode.SHUFFLE -> MaterialTheme.colorScheme.primary
                        PlayMode.SINGLE -> MaterialTheme.colorScheme.primary
                    }
                    IconButton(
                        onClick = { playerViewModel.cyclePlayMode() },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(modeIcon, "播放模式", tint = modeTint, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = { playerViewModel.previous() },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Filled.SkipPrevious, null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = { playerViewModel.togglePlay() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    ) {
                        if (playerState.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = { playerViewModel.next() },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(Icons.Filled.SkipNext, null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp))
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick = { playerViewModel.downloadCurrentSong() },
                        modifier = Modifier.size(40.dp),
                    ) {
                        if (playerState.isDownloading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.Download, "下载",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp))
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
