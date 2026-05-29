package com.example.music_wyy.ui.automation

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.DividerDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "自动签到",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { TodayStatusCard() }
            item { Spacer(Modifier.height(8.dp)) }
            items(taskItems) { task ->
                TaskCard(task)
            }
        }
    }
}

@Composable
private fun TodayStatusCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NeteaseRed.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = NeteaseRed, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("今日签到状态", color = TextSecondary, fontSize = 13.sp)
                Text("尚未签到", color = NeteaseRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskItem) {
    var enabled by remember { mutableStateOf(task.defaultEnabled) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(task.icon, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(task.subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = NeteaseRed,
                    uncheckedThumbColor = TextPrimary,
                    uncheckedTrackColor = DividerDark,
                ),
            )
        }
    }
}

data class TaskItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val defaultEnabled: Boolean = true,
)

private val taskItems = listOf(
    TaskItem("每日签到", "Web + Android 双平台", Icons.Filled.CheckCircle),
    TaskItem("云贝签到", "自动完成云贝任务", Icons.Filled.Star),
    TaskItem("刷歌打卡", "每日自动播放 300 首", Icons.Filled.MusicNote),
    TaskItem("VIP 成长值", "领取会员任务奖励", Icons.Filled.Star),
)
