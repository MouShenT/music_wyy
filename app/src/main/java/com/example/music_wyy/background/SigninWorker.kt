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

class SigninWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val api: NeteaseApi by inject()
    private val cookieStore: CookieStore by inject()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override suspend fun doWork(): Result {
        val cookie = cookieStore.cookie.first() ?: ""
        if (cookie.isBlank()) return Result.failure()

        return try {
            val response = api.dailySignin("MUSIC_U=$cookie")
            val body = response.string()
            val result = json.decodeFromString<SigninResult>(body)
            if (result.code == 200 || result.code == -2) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

@Serializable
private data class SigninResult(val code: Int = -1, val point: Int? = null, val msg: String? = null)
