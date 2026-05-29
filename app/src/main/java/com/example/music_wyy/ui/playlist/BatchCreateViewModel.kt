package com.example.music_wyy.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class BatchCreateUiState(
    val playlistName: String = "",
    val songInput: String = "",
    val isRunning: Boolean = false,
    val log: List<String> = emptyList(),
    val resultMessage: String? = null,
)

class BatchCreateViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BatchCreateUiState())
    val state: StateFlow<BatchCreateUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun updatePlaylistName(name: String) { _state.update { it.copy(playlistName = name) } }
    fun updateSongInput(input: String) { _state.update { it.copy(songInput = input) } }

    fun runBatchCreate() {
        val s = _state.value
        if (s.playlistName.isBlank() || s.songInput.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isRunning = true, log = emptyList(), resultMessage = null) }
            val cookie = cookieStore.cookie.first() ?: ""
            if (cookie.isBlank()) {
                _state.update { it.copy(isRunning = false, resultMessage = "请先登录") }
                return@launch
            }

            val log = mutableListOf<String>()
            fun log(msg: String) { log.add(msg); _state.update { it.copy(log = log.toList()) } }

            try {
                // Parse input
                val lines = s.songInput.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (lines.isEmpty()) {
                    _state.update { it.copy(isRunning = false, resultMessage = "请输入至少一首歌曲") }
                    return@launch
                }
                val songs = lines.map { line ->
                    val parts = line.split("-", "—", "–", limit = 2)
                    if (parts.size == 2) parts[0].trim() to parts[1].trim()
                    else line to ""
                }

                log("共 ${songs.size} 首歌，开始搜索匹配...")

                // Search each song
                val matched = mutableListOf<Long>()
                for ((i, song) in songs.withIndex()) {
                    val keyword = if (song.second.isNotBlank()) "${song.first} ${song.second}" else song.first
                    val resp = api.search(keyword, cookie = "MUSIC_U=$cookie")
                    val body = resp.string()
                    val result = json.decodeFromString<SearchResponse>(body)
                    val trackId = result.result?.songs?.firstOrNull()?.id
                    if (trackId != null) {
                        matched.add(trackId)
                        log("  ✅ [${i + 1}/${songs.size}] ${song.first} → 已匹配")
                    } else {
                        log("  ❌ [${i + 1}/${songs.size}] ${song.first} → 未找到")
                    }
                    delay(400) // rate limit
                }

                if (matched.isEmpty()) {
                    _state.update { it.copy(isRunning = false, resultMessage = "没有匹配到任何歌曲") }
                    return@launch
                }

                log("匹配完成: ${matched.size}/${songs.size}")

                // Create playlist
                val createResp = api.createPlaylist(s.playlistName, "MUSIC_U=$cookie")
                val createBody = createResp.string()
                val createResult = json.decodeFromString<CreatePlaylistResponse>(createBody)
                val playlistId = createResult.id ?: createResult.playlist?.id
                if (playlistId == null) {
                    _state.update { it.copy(isRunning = false, resultMessage = "创建歌单失败: ${createResult.code}") }
                    return@launch
                }
                log("歌单「${s.playlistName}」创建成功 (id: $playlistId)")

                // Add songs in batches
                val batchSize = 100
                val batches = matched.chunked(batchSize)
                for ((i, batch) in batches.withIndex()) {
                    val tracksParam = batch.joinToString(",")
                    api.addTracksToPlaylist(op = "add", pid = playlistId.toString(), tracks = tracksParam, cookie = "MUSIC_U=$cookie")
                    log("  添加第 ${i + 1}/${batches.size} 批 (${batch.size} 首)")
                    if (batches.size > 1) delay(300)
                }

                _state.update {
                    it.copy(
                        isRunning = false,
                        resultMessage = "完成! 创建歌单「${s.playlistName}」，添加 ${matched.size} 首歌",
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRunning = false,
                        resultMessage = "失败: ${e.localizedMessage ?: "未知错误"}",
                    )
                }
            }
        }
    }
}

@Serializable
private data class SearchResponse(val result: SearchResult? = null)
@Serializable
private data class SearchResult(val songs: List<SearchSong>? = null)
@Serializable
private data class SearchSong(val id: Long = 0, val name: String = "")

@Serializable
private data class CreatePlaylistResponse(
    val code: Int = -1,
    val id: Long? = null,
    val playlist: CreatePlaylistData? = null,
)
@Serializable
private data class CreatePlaylistData(val id: Long? = null)
