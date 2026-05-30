package com.example.music_wyy.ui.playlist

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
import androidx.compose.ui.text.input.ImeAction
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
fun BatchCreateScreen(
    onBack: () -> Unit,
    viewModel: BatchCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("批量创建歌单", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // AI 找歌按钮
            item {
                Button(
                    onClick = { viewModel.toggleAiDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeteaseRedLight.copy(alpha = 0.2f),
                    ),
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = NeteaseRedLight, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI 对话找歌", color = NeteaseRedLight)
                }
            }

            // AI 对话框（内嵌）
            if (state.showAiDialog) {
                item {
                    AiSearchDialog(state = state, viewModel = viewModel)
                }
            }

            // 歌单名称
            item {
                OutlinedTextField(
                    value = state.playlistName,
                    onValueChange = viewModel::updatePlaylistName,
                    label = { Text("歌单名称") },
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }

            // 歌曲输入
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("歌曲列表（每行一首，格式：歌名 - 歌手）", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = state.songInput,
                    onValueChange = viewModel::updateSongInput,
                    label = { Text("粘贴歌曲列表") },
                    minLines = 6,
                    maxLines = 12,
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

            // 运行按钮
            item {
                Button(
                    onClick = viewModel::runBatchCreate,
                    enabled = !state.isRunning && state.playlistName.isNotBlank() && state.songInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
                ) {
                    if (state.isRunning) {
                        CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("处理中...", color = TextPrimary)
                    } else {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("开始创建", color = TextPrimary)
                    }
                }
            }

            // 结果消息
            val msg = state.resultMessage
            if (msg != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = NeteaseRed.copy(alpha = 0.15f)),
                    ) {
                        Text(msg, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // 日志
            if (state.log.isNotEmpty()) {
                item {
                    Text("处理日志", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                items(state.log) { line ->
                    Text(
                        line,
                        color = if (line.contains("✅")) NeteaseRed else TextTertiary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun AiSearchDialog(
    state: BatchCreateUiState,
    viewModel: BatchCreateViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, null, tint = NeteaseRedLight, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("AI 对话找歌", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { viewModel.toggleAiDialog() }) {
                    Text("关闭", color = TextSecondary, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.aiQuery,
                onValueChange = viewModel::updateAiQuery,
                placeholder = { Text("例：进击的巨人所有OP和ED", color = TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = NeteaseRed,
                    unfocusedBorderColor = BackgroundDark,
                    cursorColor = NeteaseRed,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::aiSearch,
                enabled = state.aiQuery.isNotBlank() && !state.aiLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed),
            ) {
                if (state.aiLoading) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("AI 搜索中...", color = TextPrimary, fontSize = 14.sp)
                } else {
                    Icon(Icons.Filled.Search, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI 搜索", color = TextPrimary, fontSize = 14.sp)
                }
            }

            // 缓存提示
            if (state.aiFromCache) {
                Text("来自缓存", color = NeteaseRedLight, fontSize = 11.sp)
            }

            // 错误
            state.aiError?.let { err ->
                Text(err, color = NeteaseRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            // AI 结果列表
            if (state.aiSongs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("找到 ${state.aiSongs.size} 首", color = TextPrimary, fontSize = 13.sp)
                    Row {
                        TextButton(onClick = viewModel::selectAllAiSongs) {
                            Text("全选", color = NeteaseRed, fontSize = 12.sp)
                        }
                        TextButton(onClick = viewModel::deselectAllAiSongs) {
                            Text("取消", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.aiSongs.take(15).forEachIndexed { index, song ->
                        val selected = index in state.selectedAiIndices
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleAiSong(index) }
                                .background(
                                    if (selected) NeteaseRed.copy(alpha = 0.1f) else CardDark,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (selected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                null,
                                tint = if (selected) NeteaseRed else TextTertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.name, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (state.aiSongs.size > 15) {
                        Text("... 还有 ${state.aiSongs.size - 15} 首", color = TextTertiary, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::fillSelectedAiSongs,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeteaseRedLight),
                ) {
                    Text("填入歌曲输入框 (${state.selectedAiIndices.size} 首)", color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}
