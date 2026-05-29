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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    viewModel: AutomationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("自动签到", fontWeight = FontWeight.Bold, color = TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 状态卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.todaySigned)
                            NeteaseRed.copy(alpha = 0.15f) else CardDark
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle, null,
                            tint = if (state.todaySigned) NeteaseRed else TextSecondary,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "今日签到状态",
                                color = TextSecondary,
                                fontSize = 13.sp,
                            )
                            Text(
                                if (state.todaySigned) "已签到" else "尚未签到",
                                color = if (state.todaySigned) NeteaseRed else TextSecondary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (state.todayPoints > 0) {
                                Text(
                                    "+${state.todayPoints} 积分",
                                    color = NeteaseRed,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.runSignin() },
                            enabled = !state.isRunning,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeteaseRed,
                                disabledContainerColor = CardDark,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            if (state.isRunning) {
                                CircularProgressIndicator(
                                    color = TextPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("立即签到", color = TextPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 结果消息
            val message = state.resultMessage
            if (message != null) {
                item {
                    Text(
                        message,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // 任务列表
            item {
                Text(
                    "任务管理",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            items(state.tasks) { task ->
                TaskCard(
                    task = task,
                    onToggle = { enabled -> viewModel.toggleTask(task.id, enabled) },
                )
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskState, onToggle: (Boolean) -> Unit) {
    val icon = when (task.id) {
        "sign" -> Icons.Filled.CheckCircle
        "yunbei" -> Icons.Filled.Star
        "scrobble" -> Icons.Filled.MusicNote
        else -> Icons.Filled.Schedule
    }

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
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(task.subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = task.enabled,
                onCheckedChange = onToggle,
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
