package com.example.music_wyy.ui.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchCreateScreen(
    onBack: () -> Unit,
    viewModel: BatchCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "批量创建歌单",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── AI 对话找歌 (inline expandable at TOP) ──
            item(key = "ai_section") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    onClick = { viewModel.toggleAiDialog() },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "AI 对话找歌",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "用自然语言描述你想找的歌",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                // AI search panel — expands inline
                AnimatedVisibility(
                    visible = state.showAiDialog,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = state.aiQuery,
                                    onValueChange = viewModel::updateAiQuery,
                                    placeholder = {
                                        Text(
                                            "例：进击的巨人所有OP和ED",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = viewModel::aiSearch,
                                    enabled = state.aiQuery.isNotBlank() && !state.aiLoading,
                                    modifier = Modifier.height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    if (state.aiLoading) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (state.aiFromCache) {
                                Text(
                                    "来自缓存",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }

                            state.aiError?.let { err ->
                                Text(
                                    err,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }

                            if (state.aiSongs.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "找到 ${state.aiSongs.size} 首",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Row {
                                        TextButton(onClick = viewModel::selectAllAiSongs) {
                                            Text("全选", color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(onClick = viewModel::deselectAllAiSongs) {
                                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    state.aiSongs.take(10).forEachIndexed { index, song ->
                                        val selected = index in state.selectedAiIndices
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 42.dp)
                                                .clickable { viewModel.toggleAiSong(index) }
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(8.dp),
                                                )
                                                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                tint = if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    song.name,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    song.artist,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                    if (state.aiSongs.size > 10) {
                                        Text(
                                            "... 还有 ${state.aiSongs.size - 10} 首",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = viewModel::fillSelectedAiSongs,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                ) {
                                    Text(
                                        "填入歌曲输入框 (${state.selectedAiIndices.size} 首)",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 歌单名称 ──
            item(key = "playlist_name") {
                OutlinedTextField(
                    value = state.playlistName,
                    onValueChange = viewModel::updatePlaylistName,
                    label = { Text("歌单名称", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }

            // ── 歌曲输入 ──
            item(key = "song_input") {
                Text(
                    "歌曲列表（每行一首，格式：歌名 - 歌手）",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.songInput,
                    onValueChange = viewModel::updateSongInput,
                    label = { Text("粘贴歌曲列表", style = MaterialTheme.typography.bodyMedium) },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            // ── 运行按钮 ──
            item(key = "run_button") {
                Button(
                    onClick = viewModel::runBatchCreate,
                    enabled = !state.isRunning && state.playlistName.isNotBlank() && state.songInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (state.isRunning) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "处理中...",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    } else {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "开始创建",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            // ── 结果消息 ──
            state.resultMessage?.let { msg ->
                item(key = "result_msg") {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut(),
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Text(
                                msg,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }

            // ── 处理日志 (last 5 lines) ──
            if (state.log.isNotEmpty()) {
                item(key = "log_header") {
                    Text(
                        "处理日志",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.log) { line ->
                    Text(
                        line,
                        color = when {
                            line.contains("✅") -> MaterialTheme.colorScheme.primary
                            line.contains("❌") -> MaterialTheme.colorScheme.error
                            line.contains("⚠️") -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 1.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(16.dp)) }
        }
    }
}
