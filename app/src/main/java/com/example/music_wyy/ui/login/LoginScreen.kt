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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.music_wyy.ui.theme.CardDark
import com.example.music_wyy.ui.theme.NeteaseRed
import com.example.music_wyy.ui.theme.TextPrimary
import com.example.music_wyy.ui.theme.TextSecondary
import com.example.music_wyy.ui.theme.TextTertiary
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(60.dp))

        // Logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(NeteaseRed, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(36.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("网易云工具", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("歌单管理 · 自动化签到 · 数据分析", fontSize = 13.sp, color = TextSecondary)

        Spacer(Modifier.height(40.dp))

        // Cookie 输入
        OutlinedTextField(
            value = state.cookie,
            onValueChange = viewModel::onCookieChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("粘贴 Cookie", color = TextSecondary) },
            placeholder = { Text("粘贴从浏览器复制的 Cookie...", color = TextTertiary) },
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

        Spacer(Modifier.height(8.dp))
        Text(
            text = "浏览器登录 music.163.com → F12 → Cookies → 复制 Cookie 值",
            fontSize = 12.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        // 错误提示
        val errorMsg = state.error
        if (errorMsg != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMsg,
                fontSize = 13.sp,
                color = NeteaseRed,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(28.dp))

        // 登录按钮
        Button(
            onClick = { viewModel.login() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = state.cookie.isNotBlank() && !state.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeteaseRed,
                contentColor = TextPrimary,
                disabledContainerColor = CardDark,
                disabledContentColor = TextTertiary,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = TextPrimary,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(60.dp))
    }
}
