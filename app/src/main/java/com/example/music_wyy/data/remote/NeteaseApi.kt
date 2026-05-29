package com.example.music_wyy.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NeteaseApi {

    @GET("/login/status")
    suspend fun checkLogin(@Query("cookie") cookie: String): ResponseBody

    @GET("/login/refresh")
    suspend fun refreshLogin(@Query("cookie") cookie: String): ResponseBody

    @GET("/user/playlist")
    suspend fun getUserPlaylists(
        @Query("uid") uid: String,
        @Query("cookie") cookie: String,
    ): ResponseBody

    @GET("/daily_signin")
    suspend fun dailySignin(@Query("cookie") cookie: String): ResponseBody

    @GET("/playlist/detail")
    suspend fun getPlaylistDetail(
        @Query("id") id: String,
        @Query("cookie") cookie: String,
    ): ResponseBody
}

@Serializable
data class LoginStatusResponse(
    val code: Int = -1,
    val account: JsonObject? = null,
    val profile: JsonObject? = null,
)

@Serializable
data class UserPlaylistResponse(
    val code: Int = -1,
    val playlist: List<JsonObject>? = null,
)

@Serializable
data class DailySigninResponse(
    val code: Int = -1,
    val point: Int? = null,
    val msg: String? = null,
)
