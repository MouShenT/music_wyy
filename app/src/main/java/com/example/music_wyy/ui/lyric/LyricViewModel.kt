package com.example.music_wyy.ui.lyric

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class LyricUiState(
    val songId: String = "",
    val songName: String = "",
    val artist: String = "",
    val lyricText: String = "",
    val tlyricText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showTranslation: Boolean = false,
)

class LyricViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
) : ViewModel() {

    private val _state = MutableStateFlow(LyricUiState())
    val state: StateFlow<LyricUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun loadLyric(songId: String, songName: String = "", artist: String = "") {
        _state.update { it.copy(songId = songId, songName = songName, artist = artist) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val cookie = cookieStore.cookie.first() ?: ""
                val resp = api.getLyric(songId, "MUSIC_U=$cookie")
                val body = resp.string()
                val result = json.decodeFromString<LyricResponse>(body)

                if (result.code == 200) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            lyricText = result.lrc?.lyric ?: "",
                            tlyricText = result.tlyric?.lyric ?: "",
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "暂无歌词") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "加载失败: ${e.localizedMessage}") }
            }
        }
    }

    fun toggleTranslation() {
        _state.update { it.copy(showTranslation = !it.showTranslation) }
    }

    fun exportLyric(context: Context) {
        val s = _state.value
        val text = buildString {
            appendLine(s.songName.ifBlank { "未知歌曲" })
            if (s.artist.isNotBlank()) appendLine("歌手: ${s.artist}")
            appendLine()
            appendLine("--- 原版歌词 ---")
            appendLine(s.lyricText.ifBlank { "(暂无)" })
            if (s.tlyricText.isNotBlank()) {
                appendLine()
                appendLine("--- 翻译歌词 ---")
                appendLine(s.tlyricText)
            }
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${s.songName} 歌词")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "导出歌词"))
    }
}

@Serializable
private data class LyricResponse(
    val code: Int = -1,
    val lrc: LyricLine? = null,
    val tlyric: LyricLine? = null,
)

@Serializable
private data class LyricLine(val lyric: String = "")
