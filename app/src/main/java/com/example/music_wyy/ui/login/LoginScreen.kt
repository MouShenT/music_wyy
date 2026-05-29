package com.example.music_wyy.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_wyy.ui.theme.BackgroundDark
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary
import com.example.music_wyy.ui.theme.TextTertiary

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit = {}) {
    var cookie by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(80.dp))

        // Logo 区域
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(NeteaseRed, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(40.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "网易云工具",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Text(
            text = "歌单管理 · 自动化签到 · 数据分析",
            fontSize = 14.sp,
            color = TextSecondary,
        )

        Spacer(Modifier.height(48.dp))

        // Cookie 输入
        OutlinedTextField(
            value = cookie,
            onValueChange = { cookie = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("粘贴 Cookie (MUSIC_U)", color = TextSecondary) },
            placeholder = { Text("MUSIC_U=xxxxxxxxxxxxx...", color = TextTertiary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = NeteaseRed,
                unfocusedBorderColor = CardDark,
                cursorColor = NeteaseRed,
                focusedContainerColor = CardDark,
                unfocusedContainerColor = CardDark,
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = 3,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "在浏览器登录 music.163.com → F12 → Cookies → 复制 MUSIC_U 字段的值",
            fontSize = 12.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(32.dp))

        // 登录按钮
        Button(
            onClick = { onLoginSuccess() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = cookie.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeteaseRed,
                contentColor = TextPrimary,
                disabledContainerColor = CardDark,
                disabledContentColor = TextTertiary,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(80.dp))
    }
}
