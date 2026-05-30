package com.example.music_wyy.ui.ai

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun AiSearchScreen(
    onBack: () -> Unit,
    onFillBatchCreate: (String) -> Unit,
    viewModel: AiSearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("AI 找歌", fontWeight = FontWeight.Bold, color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 搜索框
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onEvent(AiSearchEvent.UpdateQuery(it)) },
                    label = { Text("描述你想找的歌...") },
                    placeholder = { Text("例：进击的巨人所有OP和ED", color = TextTertiary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeteaseRed,
                        unfocusedBorderColor = CardDark,
                        focusedLabelColor = NeteaseRed,
                        unfocusedLabelColor = TextSecondary,
                        cursorColor = NeteaseRed,
                    ),
                )
            }

            // 搜索按钮
            item {
                Button(
                    onClick = { viewModel.onEvent(AiSearchEvent.Search) },
                    enabled = state.query.isNotBlank() && !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("AI 搜索中...", color = TextPrimary)
                    } else {
                        Icon(Icons.Filled.Search, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("搜索", color = TextPrimary)
                    }
                }
            }

            // 缓存提示
            if (state.fromCache) {
                item {
                    Text("来自缓存，秒级响应", color = NeteaseRedLight, fontSize = 12.sp)
                }
            }

            // 错误
            state.error?.let { err ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NeteaseRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(err, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // 结果列表
            if (state.songs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("找到 ${state.songs.size} 首", color = TextPrimary, fontWeight = FontWeight.Medium)
                        Row {
                            TextButton(onClick = { viewModel.onEvent(AiSearchEvent.SelectAll) }) {
                                Text("全选", color = NeteaseRed, fontSize = 13.sp)
                            }
                            TextButton(onClick = { viewModel.onEvent(AiSearchEvent.DeselectAll) }) {
                                Text("取消", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                }

                itemsIndexed(state.songs, key = { i, s -> "${s.name}-${s.artist}-$i" }) { index, song ->
                    val selected = index in state.selectedSongs
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onEvent(AiSearchEvent.ToggleSong(index)) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) NeteaseRed.copy(alpha = 0.12f) else CardDark,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                null,
                                tint = if (selected) NeteaseRed else TextTertiary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.name, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // 一键填入批量创建
                item {
                    Button(
                        onClick = {
                            onFillBatchCreate(viewModel.getSelectedFormatted())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeteaseRedLight),
                    ) {
                        Text("一键填入批量创建 (${state.selectedSongs.size} 首)", color = TextPrimary)
                    }
                }
            }

            // 历史记录
            if (state.history.isNotEmpty() && state.songs.isEmpty() && !state.isLoading) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.History, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("历史记录", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                itemsIndexed(state.history, key = { i, _ -> "h-$i" }) { _, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onEvent(AiSearchEvent.SearchHistory(item.query)) },
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.query, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${item.songCount} 首歌 · ${item.createdAt.take(10)}", color = TextTertiary, fontSize = 11.sp)
                            }
                            Icon(Icons.Filled.Search, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
