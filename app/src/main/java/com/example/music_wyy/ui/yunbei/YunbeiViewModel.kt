package com.example.music_wyy.ui.yunbei

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class YunbeiUiState(
    val point: Int = 0,
    val mobileSigned: Boolean = false,
    val tasks: List<YunbeiTask> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class YunbeiTask(
    val id: String,
    val name: String,
    val maxPoint: Int,
    val done: Boolean,
)

class YunbeiViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
) : ViewModel() {

    private val _state = MutableStateFlow(YunbeiUiState())
    val state: StateFlow<YunbeiUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val cookie = cookieStore.cookie.first() ?: ""
                val c = "MUSIC_U=$cookie"

                // 并行获取云贝余额 + 签到信息 + 任务列表
                val infoDeferred = async { api.getYunbeiInfo(c) }
                val signDeferred = async { api.getYunbei(c) }
                val tasksDeferred = async { api.getYunbeiTasks(c) }

                val infoBody = infoDeferred.await().string()
                val infoResult = json.decodeFromString<YunbeiInfoResponse>(infoBody)

                val signBody = signDeferred.await().string()
                val signResult = json.decodeFromString<YunbeiSignResponse>(signBody)

                val tasksBody = tasksDeferred.await().string()
                val tasksResult = json.decodeFromString<YunbeiTasksResponse>(tasksBody)

                val tasks = if (tasksResult.code == 200) {
                    tasksResult.data?.map { t ->
                        YunbeiTask(
                            id = t.id,
                            name = t.name,
                            maxPoint = t.maxPoint,
                            done = t.done,
                        )
                    } ?: emptyList()
                } else emptyList()

                _state.update {
                    it.copy(
                        isLoading = false,
                        point = if (infoResult.code == 200) infoResult.point else it.point,
                        mobileSigned = signResult.mobileSign,
                        tasks = tasks,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "加载失败: ${e.localizedMessage}") }
            }
        }
    }
}

@Serializable
private data class YunbeiInfoResponse(
    val code: Int = -1,
    val point: Int = 0,
)

@Serializable
private data class YunbeiSignResponse(
    val code: Int = -1,
    val mobileSign: Boolean = false,
)

@Serializable
private data class YunbeiTasksResponse(
    val code: Int = -1,
    val data: List<YunbeiTaskItem>? = null,
)

@Serializable
private data class YunbeiTaskItem(
    val id: String = "",
    val name: String = "",
    @SerialName("max_point")
    val maxPoint: Int = 0,
    val done: Boolean = false,
)
