package com.example.music_wyy.ui.playlist

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.NeteaseRed
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
    val state by viewModel.state.collectAsState()

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
                Text("歌曲列表（每行一首，格式：歌名 - 歌手）", color = TextSecondary, fontSize = 12.sp)
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
