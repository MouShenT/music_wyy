package com.example.music_wyy.ui.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AutomationUiState(
    val todaySigned: Boolean = false,
    val todayPoints: Int = 0,
    val tasks: List<TaskState> = defaultTasks,
    val isRunning: Boolean = false,
    val resultMessage: String? = null,
)

data class TaskState(
    val id: String,
    val title: String,
    val subtitle: String,
    val enabled: Boolean = true,
)

private val defaultTasks = listOf(
    TaskState("sign", "每日签到", "Web + Android 双平台"),
    TaskState("yunbei", "云贝签到", "自动完成云贝任务"),
    TaskState("scrobble", "刷歌打卡", "每日自动播放 300 首"),
    TaskState("vip", "VIP 成长值", "领取会员任务奖励"),
)

@Serializable
private data class SigninResponse(val code: Int = -1, val point: Int? = null, val msg: String? = null)

class AutomationViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
) : ViewModel() {

    private val _state = MutableStateFlow(AutomationUiState())
    val state: StateFlow<AutomationUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun toggleTask(id: String, enabled: Boolean) {
        _state.update {
            it.copy(tasks = it.tasks.map { t -> if (t.id == id) t.copy(enabled = enabled) else t })
        }
    }

    fun runSignin() {
        viewModelScope.launch {
            _state.update { it.copy(isRunning = true, resultMessage = null) }

            try {
                val cookie = cookieStore.cookie.first() ?: ""
                val response = api.dailySignin("MUSIC_U=$cookie")
                val body = response.string()
                val result = json.decodeFromString<SigninResponse>(body)

                if (result.code == 200) {
                    _state.update {
                        it.copy(
                            isRunning = false,
                            todaySigned = true,
                            todayPoints = result.point ?: 0,
                            resultMessage = "签到成功 +${result.point ?: 0} 积分",
                        )
                    }
                } else if (result.code == -2) {
                    _state.update {
                        it.copy(isRunning = false, resultMessage = "今日已签到，无需重复")
                    }
                } else {
                    _state.update {
                        it.copy(isRunning = false, resultMessage = result.msg ?: "签到失败")
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRunning = false,
                        resultMessage = "连接失败: ${e.localizedMessage ?: "未知错误"}",
                    )
                }
            }
        }
    }
}
