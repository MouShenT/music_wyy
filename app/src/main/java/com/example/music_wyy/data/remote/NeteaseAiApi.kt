package com.example.music_wyy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NeteaseAiApi {

    @POST("/api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    @GET("/api/history")
    suspend fun history(@Query("limit") limit: Int = 20): HistoryResponse

    @GET("/health")
    suspend fun health(): HealthResponse
}

@Serializable
data class ChatRequest(
    val query: String,
    val language: String = "zh",
)

@Serializable
data class ChatResponse(
    val songs: List<AiSong> = emptyList(),
    val total: Int = 0,
    @SerialName("from_cache")
    val fromCache: Boolean = false,
    @SerialName("keywords_used")
    val keywordsUsed: Int = 0,
)

@Serializable
data class AiSong(
    val name: String,
    val artist: String,
    val id: Long = 0,
    val album: String = "",
)

@Serializable
data class HistoryResponse(
    val items: List<HistoryItem> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class HistoryItem(
    val query: String,
    @SerialName("song_count")
    val songCount: Int,
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class HealthResponse(
    val status: String,
    val model: String,
    val redis: String = "",
    @SerialName("llm_mode")
    val llmMode: String = "",
    @SerialName("llm_ready")
    val llmReady: Boolean = false,
)
