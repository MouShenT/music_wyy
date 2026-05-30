package com.example.music_wyy.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.NeteaseRedLight
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary
import com.example.music_wyy.ui.theme.TextTertiary
import org.koin.compose.viewmodel.koinViewModel

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
            title = { Text("添加到歌单", color = TextPrimary) },
            text = {
                if (state.userPlaylists.isEmpty()) {
                    Text("加载歌单列表...", color = TextSecondary)
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
                                Icon(Icons.Filled.MusicNote, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(pl.name, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${pl.trackCount} 首", color = TextTertiary, fontSize = 12.sp)
                                }
                                Icon(Icons.Filled.Add, null, tint = NeteaseRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.hidePlaylistPicker() }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = CardDark,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("首页", fontWeight = FontWeight.Bold, color = TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
            actions = {
                IconButton(onClick = { viewModel.loadOverview() }) {
                    Icon(Icons.Filled.Refresh, null, tint = TextPrimary)
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 用户问候
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.MusicNote, null,
                            tint = NeteaseRed,
                            modifier = Modifier.size(40.dp).clip(CircleShape).padding(6.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                state.nickname?.let { "Hi, $it" } ?: "欢迎使用",
                                color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            )
                            Text("网易云音乐助手", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 统计卡片
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard("我的歌单", state.totalPlaylists.toString(), NeteaseRed, Modifier.weight(1f))
                    StatCard("总歌曲数", state.totalSongs.toString(), TextPrimary, Modifier.weight(1f))
                }
            }

            // 快捷入口
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
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

            // 签到状态
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.todaySigned) NeteaseRed.copy(alpha = 0.15f) else CardDark
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("今日签到", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                if (state.todaySigned) "已签到" else "未签到",
                                color = if (state.todaySigned) NeteaseRed else TextTertiary,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            )
                            if (state.todayPoints > 0) {
                                Text("+${state.todayPoints} 积分", color = NeteaseRed, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // ── 音乐搜索 ──
            item {
                Text("搜索歌曲", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = { Text("搜索歌曲或歌手...", color = TextTertiary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeteaseRed,
                            unfocusedBorderColor = CardDark,
                            cursorColor = NeteaseRed,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.search() },
                        enabled = state.searchQuery.isNotBlank() && !state.isSearching,
                        colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (state.isSearching) {
                            CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Search, null, Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 搜索结果消息
            state.searchMessage?.let { msg ->
                item {
                    Text(msg, color = if (msg.startsWith("搜索失败")) NeteaseRed else NeteaseRedLight, fontSize = 12.sp)
                }
            }

            // 添加结果消息
            state.addResultMessage?.let { msg ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = NeteaseRed.copy(alpha = 0.15f)),
                    ) {
                        Text(msg, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // 搜索结果
            if (state.searchResults.isNotEmpty()) {
                items(state.searchResults, key = { it.id }) { song ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(song.name, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${song.artist} · ${song.album}", color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.showPlaylistPicker(song.id, song.name) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(Icons.Filled.Add, "添加到歌单", tint = NeteaseRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    CircularProgressIndicator(
                        color = NeteaseRed,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = NeteaseRedLight, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
