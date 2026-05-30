package com.example.music_wyy.ui.player

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.DividerDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary
import com.example.music_wyy.ui.theme.TextTertiary
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val song = state.currentSong

    if (song == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundDark),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.MusicNote, null, tint = TextTertiary, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("暂无播放歌曲", color = TextSecondary, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Text("从歌单详情页点击歌曲即可播放", color = TextTertiary, fontSize = 12.sp)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // 顶部栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text("正在播放", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            // 下载按钮
            IconButton(onClick = { viewModel.downloadCurrentSong() }) {
                if (state.isDownloading) {
                    CircularProgressIndicator(
                        color = NeteaseRed,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Filled.Download, "下载", tint = TextPrimary)
                }
            }
        }

        // 下载结果提示
        state.downloadResult?.let { msg ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = NeteaseRed.copy(alpha = 0.15f)),
            ) {
                Text(
                    msg,
                    color = if (msg.startsWith("下载失败")) TextPrimary else TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // 错误提示
        state.error?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = NeteaseRed.copy(alpha = 0.15f)),
            ) {
                Text(err, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }

        // 缓存状态
        if (song.cachedPath != null) {
            Text(
                "已缓存 · ${formatBytes(state.cacheSize)}",
                color = NeteaseRed,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // 专辑封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .weight(0.4f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark),
                contentAlignment = Alignment.Center,
            ) {
                if (song.coverUrl != null) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = TextTertiary, modifier = Modifier.size(80.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 歌曲信息 + 控制
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        song.name,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${song.artist} · ${song.album}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 进度条
            Slider(
                value = state.position.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = NeteaseRed,
                    activeTrackColor = NeteaseRed,
                    inactiveTrackColor = DividerDark,
                ),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(state.position), color = TextTertiary, fontSize = 11.sp)
                Text(formatMs(state.duration), color = TextTertiary, fontSize = 11.sp)
            }

            Spacer(Modifier.height(16.dp))

            // 播放控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.cyclePlayMode() }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        when (state.playMode) {
                            PlayMode.SINGLE -> Icons.Filled.RepeatOneOn
                            PlayMode.LIST -> Icons.Filled.Repeat
                            PlayMode.SHUFFLE -> Icons.Filled.Shuffle
                        },
                        null,
                        tint = when (state.playMode) {
                            PlayMode.LIST -> TextTertiary
                            else -> NeteaseRed
                        },
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = TextPrimary, modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = { viewModel.togglePlay() },
                    modifier = Modifier.size(64.dp).background(NeteaseRed, CircleShape),
                    enabled = !state.isLoading,
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipNext, null, tint = TextPrimary, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { viewModel.downloadCurrentSong() }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Download, "下载", tint = TextTertiary, modifier = Modifier.size(22.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 播放列表
        if (state.playlist.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(0.25f),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                itemsIndexed(state.playlist, key = { _, s -> s.id }) { _, songItem ->
                    val isCurrent = songItem.id == state.currentSong?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playSong(songItem) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            songItem.name,
                            color = if (isCurrent) NeteaseRed else TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            songItem.artist,
                            color = if (isCurrent) NeteaseRed.copy(alpha = 0.8f) else TextTertiary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.removeFromPlaylist(songItem.id) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Filled.Close, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}
