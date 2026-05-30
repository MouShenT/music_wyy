package com.example.music_wyy.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

private val animSpec = tween<Float>(300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToYunbei: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Playlist picker dialog
    if (state.showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.hidePlaylistPicker() },
            title = { Text("添加到歌单", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                if (state.userPlaylists.isEmpty()) {
                    Text("加载歌单列表...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(state.userPlaylists) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.addToPlaylist(pl.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.MusicNote, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        pl.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${pl.trackCount} 首",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Icon(
                                    Icons.Filled.Add, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.hidePlaylistPicker() }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "首页",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
            actions = {
                IconButton(onClick = { viewModel.loadOverview() }) {
                    Icon(
                        Icons.Filled.Refresh, null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 用户问候
            item(key = "greeting") {
                Card(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.MusicNote, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp).clip(CircleShape).padding(6.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                state.nickname?.let { "Hi, $it" } ?: "欢迎使用",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                "网易云音乐助手",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            // 统计卡片
            item(key = "stats") {
                AnimatedVisibility(
                    visible = !state.isLoading,
                    modifier = Modifier.animateItem(),
                    enter = fadeIn(animationSpec = animSpec),
                    exit = fadeOut(animationSpec = animSpec),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard(
                            "我的歌单", state.totalPlaylists.toString(),
                            MaterialTheme.colorScheme.primary,
                            Modifier.weight(1f),
                        )
                        StatCard(
                            "总歌曲数", state.totalSongs.toString(),
                            MaterialTheme.colorScheme.onSurface,
                            Modifier.weight(1f),
                        )
                    }
                }
            }

            // 快捷入口
            item(key = "quick_entries") {
                AnimatedVisibility(
                    visible = !state.isLoading,
                    modifier = Modifier.animateItem(),
                    enter = fadeIn(animationSpec = animSpec),
                    exit = fadeOut(animationSpec = animSpec),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column {
                            QuickEntry(
                                icon = Icons.Filled.Cloud, title = "云贝中心",
                                subtitle = "查看云贝余额和任务", onClick = onNavigateToYunbei,
                            )
                            QuickEntry(
                                icon = Icons.Filled.MailOutline, title = "我的私信",
                                subtitle = "查看私信消息", onClick = onNavigateToMessages,
                            )
                        }
                    }
                }
            }

            // 签到状态
            item(key = "signin") {
                Crossfade(
                    targetState = state.todaySigned,
                    modifier = Modifier.animateItem(),
                    animationSpec = animSpec,
                ) { signed ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (signed) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "今日签到",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    if (signed) "已签到" else "未签到",
                                    color = if (signed) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (state.todayPoints > 0) {
                                    Text(
                                        "+${state.todayPoints} 积分",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 音乐搜索 ──
            item(key = "search_label") {
                AnimatedVisibility(
                    visible = !state.isLoading,
                    modifier = Modifier.animateItem(),
                    enter = fadeIn(animationSpec = animSpec),
                    exit = fadeOut(animationSpec = animSpec),
                ) {
                    Text(
                        "搜索歌曲",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            item(key = "search_input") {
                AnimatedVisibility(
                    visible = !state.isLoading,
                    modifier = Modifier.animateItem(),
                    enter = fadeIn(animationSpec = animSpec),
                    exit = fadeOut(animationSpec = animSpec),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = {
                                Text(
                                    "搜索歌曲或歌手...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.search() },
                            enabled = state.searchQuery.isNotBlank() && !state.isSearching,
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            if (state.isSearching) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Filled.Search, null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // 搜索结果消息
            state.searchMessage?.let { msg ->
                item(key = "search_message_$msg") {
                    AnimatedVisibility(
                        visible = !state.isLoading,
                        modifier = Modifier.animateItem(),
                        enter = fadeIn(animationSpec = animSpec),
                        exit = fadeOut(animationSpec = animSpec),
                    ) {
                        Text(
                            msg,
                            color = if (msg.startsWith("搜索失败")) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // 添加结果消息
            state.addResultMessage?.let { msg ->
                item(key = "add_result_$msg") {
                    AnimatedVisibility(
                        visible = !state.isLoading,
                        modifier = Modifier.animateItem(),
                        enter = fadeIn(animationSpec = animSpec),
                        exit = fadeOut(animationSpec = animSpec),
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Text(
                                msg,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }

            // 搜索结果
            if (state.searchResults.isNotEmpty()) {
                items(state.searchResults, key = { it.id }) { song ->
                    AnimatedVisibility(
                        visible = !state.isLoading,
                        modifier = Modifier.animateItem(),
                        enter = fadeIn(animationSpec = animSpec),
                        exit = fadeOut(animationSpec = animSpec),
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        song.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "${song.artist} · ${song.album}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.showPlaylistPicker(song.id, song.name) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Add, "添加到歌单",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 加载指示器
            item(key = "loading") {
                AnimatedVisibility(
                    visible = state.isLoading,
                    modifier = Modifier.animateItem(),
                    enter = fadeIn(animationSpec = animSpec),
                    exit = fadeOut(animationSpec = animSpec),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun QuickEntry(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
) {
    Card(
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge,
                color = valueColor,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
