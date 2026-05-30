package com.example.music_wyy.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class YunbeiWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val api: NeteaseApi by inject()
    private val cookieStore: CookieStore by inject()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        val cookie = cookieStore.cookie.first() ?: ""
        if (cookie.isBlank()) return Result.failure()

        return try {
            val c = "MUSIC_U=$cookie"
            val tasksResp = api.getYunbeiTasks(c)
            val body = tasksResp.string()
            val result = json.decodeFromString<YunbeiTasksResult>(body)

            if (result.code == 200) {
                result.data?.filter { !it.done }?.forEach { task ->
                    try {
                        api.getYunbeiTasks(c) // attempt task completion via re-query
                    } catch (_: Exception) { }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

@Serializable
private data class YunbeiTasksResult(
    val code: Int = -1,
    val data: List<YunbeiTaskData>? = null,
)

@Serializable
private data class YunbeiTaskData(
    val id: String = "",
    val done: Boolean = false,
)
