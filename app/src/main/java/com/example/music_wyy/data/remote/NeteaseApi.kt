package com.example.music_wyy.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NeteaseApi {

    @GET("/login/status")
    suspend fun checkLogin(): ResponseBody

    @GET("/login/refresh")
    suspend fun refreshLogin(): ResponseBody

    @GET("/user/playlist")
    suspend fun getUserPlaylists(@Query("uid") uid: String): ResponseBody

    @POST("/daily_signin")
    suspend fun dailySignin(): ResponseBody
}
