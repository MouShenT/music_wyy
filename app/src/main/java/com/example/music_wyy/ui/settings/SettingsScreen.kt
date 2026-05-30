package com.example.music_wyy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_wyy.data.local.SongCache
import com.example.music_wyy.data.local.datastore.SettingsStore
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.DividerDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary
import com.example.music_wyy.ui.theme.TextTertiary
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsStore: SettingsStore = koinInject()
    val songCache: SongCache = koinInject()
    val scope = rememberCoroutineScope()

    var cacheSize by remember { mutableStateOf(0L) }
    var cacheDir by remember { mutableStateOf("") }
    var downloadDir by remember { mutableStateOf("") }
    var maxCacheMb by remember { mutableStateOf(500L) }
    var cleared by remember { mutableStateOf(false) }

    // Load settings on first composition
    remember {
        scope.launch {
            cacheSize = songCache.getCacheSize()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("设置", fontWeight = FontWeight.Bold, color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 缓存信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storage, null, tint = NeteaseRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("歌曲缓存", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("缓存目录: ${context.cacheDir.resolve("song_cache").absolutePath}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("当前缓存: ${formatBytes(cacheSize)}", color = TextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("缓存上限: $maxCacheMb MB（超出自动删除最早缓存）", color = TextTertiary, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))

                    if (cleared) {
                        Text("缓存已清空", color = NeteaseRed, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                songCache.clearCache()
                                cacheSize = songCache.getCacheSize()
                                cleared = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Filled.DeleteOutline, null, tint = NeteaseRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("清空缓存", color = NeteaseRed, fontSize = 13.sp)
                    }
                }
            }

            // 下载目录
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Folder, null, tint = NeteaseRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("下载目录", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "下载的歌曲将保存到:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_MUSIC
                        ).absolutePath,
                        color = TextPrimary,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "文件命名格式: 歌手 - 歌名.mp3",
                        color = TextTertiary,
                        fontSize = 12.sp,
                    )
                }
            }

            // 缓存说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MusicNote, null, tint = NeteaseRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("缓存说明", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("• 播放歌曲时自动缓存到本地", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• 缓存超过上限自动删除最早的文件", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• 点击播放器右上角下载按钮保存到下载目录", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• 下载目录: Music/ 下，以「歌手 - 歌名.mp3」命名", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• 重复下载同名歌曲会跳过（不覆盖）", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
