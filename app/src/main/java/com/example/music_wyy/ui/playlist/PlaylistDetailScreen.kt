package com.example.music_wyy.ui.playlist

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onSongClick: (songId: String, songName: String, artist: String) -> Unit = { _, _, _ -> },
    onPlaySong: (songId: String, songName: String, artist: String, album: String, coverUrl: String?) -> Unit = { _, _, _, _, _ -> },
    viewModel: PlaylistDetailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(playlistId) { viewModel.loadPlaylist(playlistId) }

    val exportText = remember(state.songs) {
        if (state.songs.isEmpty()) ""
        else state.songs.joinToString("\n") { "${it.name} - ${it.artists}" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    state.name.ifBlank { "歌单详情" },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            actions = {
                if (state.songs.isNotEmpty()) {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, state.name)
                            putExtra(Intent.EXTRA_TEXT, exportText)
                        }
                        context.startActivity(Intent.createChooser(intent, "导出歌单"))
                    }) {
                        Icon(
                            Icons.Filled.Share, null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                when {
                    state.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                state.error!!,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 72.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            // Playlist header
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (state.coverUrl != null) {
                                            AsyncImage(
                                                model = state.coverUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize()
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                            )
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    Icons.Filled.MusicNote, null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(48.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
                                        Text(
                                            state.name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "${state.songs.size} 首歌曲",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        if (state.songs.isNotEmpty()) {
                                            Text(
                                                "总时长: ${formatTotalDuration(state.songs.sumOf { it.duration.toLong() })}",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }

                            // Song list
                            itemsIndexed(state.songs, key = { _, song -> song.id }) { index, song ->
                                Box(Modifier) {
                                    SongRow(
                                        index = index + 1,
                                        song = song,
                                        onPlay = {
                                            onPlaySong(song.id, song.name, song.artists, song.album, state.coverUrl)
                                        },
                                        onLyric = {
                                            onPlaySong(song.id, song.name, song.artists, song.album, state.coverUrl)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    index: Int,
    song: SongItem,
    onPlay: () -> Unit,
    onLyric: () -> Unit,
) {
    val minutes = song.duration / 1000 / 60
    val seconds = (song.duration / 1000) % 60
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 12.dp)
            .drawWithContent {
                drawContent()
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx(),
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%02d".format(index),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(28.dp),
        )
        Spacer(Modifier.width(8.dp))

        // Play button
        IconButton(onClick = onPlay, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.PlayArrow, "播放",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.name,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${song.artists} · ${song.album}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Lyrics button
        IconButton(onClick = onLyric, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.Lyrics, "歌词",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(4.dp))
        Text(
            if (song.duration > 0) "%d:%02d".format(minutes, seconds) else "",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun formatTotalDuration(totalMs: Long): String {
    val totalSec = totalMs / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes} 分钟"
}
