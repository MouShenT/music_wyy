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

    @GET("/cloudsearch")
    suspend fun search(
        @Query("keywords") keywords: String,
        @Query("type") type: Int = 1,
        @Query("limit") limit: Int = 5,
        @Query("cookie") cookie: String,
    ): ResponseBody

    @GET("/playlist/create")
    suspend fun createPlaylist(
        @Query("name") name: String,
        @Query("cookie") cookie: String,
    ): ResponseBody

    @GET("/playlist/tracks")
    suspend fun addTracksToPlaylist(
        @Query("op") op: String = "add",
        @Query("pid") pid: String,
        @Query("tracks") tracks: String,
        @Query("cookie") cookie: String,
    ): ResponseBody

    @GET("/playlist/subscribe")
    suspend fun subscribePlaylist(
        @Query("id") id: String,
        @Query("t") type: Int = 1,
        @Query("cookie") cookie: String,
    ): ResponseBody

    // ── 歌词 ──

    @GET("/lyric")
    suspend fun getLyric(
        @Query("id") id: String,
        @Query("cookie") cookie: String,
    ): ResponseBody

    // ── 私信 ──

    @GET("/msg/private")
    suspend fun getPrivateMsgs(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("cookie") cookie: String,
    ): ResponseBody

    @GET("/msg/private/history")
    suspend fun getPrivateMsgHistory(
        @Query("uid") uid: String,
        @Query("limit") limit: Int = 50,
        @Query("cookie") cookie: String,
    ): ResponseBody

    // ── 云贝 ──

    @GET("/yunbei")
    suspend fun getYunbei(@Query("cookie") cookie: String): ResponseBody

    @GET("/yunbei/info")
    suspend fun getYunbeiInfo(@Query("cookie") cookie: String): ResponseBody

    @GET("/yunbei/tasks")
    suspend fun getYunbeiTasks(@Query("cookie") cookie: String): ResponseBody

    // ── 歌曲 URL / 详情 ──

    @GET("/song/url/v1")
    suspend fun getSongUrl(
        @Query("id") id: String,
        @Query("level") level: String = "standard",
        @Query("cookie") cookie: String,
    ): ResponseBody

    @GET("/song/detail")
    suspend fun getSongDetail(
        @Query("ids") ids: String,
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
