package com.example.music_wyy.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.remote.AiSong
import com.example.music_wyy.data.remote.ChatRequest
import com.example.music_wyy.data.remote.NeteaseAiApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiSearchUiState(
    val query: String = "",
    val songs: List<AiSong> = emptyList(),
    val isLoading: Boolean = false,
    val fromCache: Boolean = false,
    val error: String? = null,
    val selectedSongs: Set<Int> = emptySet(),
    val history: List<HistoryItemUi> = emptyList(),
)

data class HistoryItemUi(
    val query: String,
    val songCount: Int,
    val createdAt: String,
)

sealed interface AiSearchEvent {
    data class UpdateQuery(val query: String) : AiSearchEvent
    data object Search : AiSearchEvent
    data object ClearResults : AiSearchEvent
    data class ToggleSong(val index: Int) : AiSearchEvent
    data object SelectAll : AiSearchEvent
    data object DeselectAll : AiSearchEvent
    data class SearchHistory(val query: String) : AiSearchEvent
}

class AiSearchViewModel(
    private val api: NeteaseAiApi,
) : ViewModel() {

    private val _state = MutableStateFlow(AiSearchUiState())
    val state: StateFlow<AiSearchUiState> = _state.asStateFlow()

    init { loadHistory() }

    fun onEvent(event: AiSearchEvent) {
        when (event) {
            is AiSearchEvent.UpdateQuery -> _state.update { it.copy(query = event.query) }
            is AiSearchEvent.Search -> search()
            is AiSearchEvent.ClearResults -> _state.update { it.copy(songs = emptyList(), error = null, selectedSongs = emptySet()) }
            is AiSearchEvent.ToggleSong -> toggleSong(event.index)
            is AiSearchEvent.SelectAll -> {
                val all = _state.value.songs.indices.toSet()
                _state.update { it.copy(selectedSongs = all) }
            }
            is AiSearchEvent.DeselectAll -> _state.update { it.copy(selectedSongs = emptySet()) }
            is AiSearchEvent.SearchHistory -> {
                _state.update { it.copy(query = event.query) }
                search()
            }
        }
    }

    private fun search() {
        val query = _state.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, songs = emptyList(), selectedSongs = emptySet()) }

            try {
                val response = api.chat(ChatRequest(query = query))
                _state.update {
                    it.copy(
                        songs = response.songs,
                        isLoading = false,
                        fromCache = response.fromCache,
                        error = if (response.songs.isEmpty()) "AI 未返回结果，请尝试更具体的描述" else null,
                    )
                }
                if (response.songs.isNotEmpty()) {
                    _state.update { it.copy(selectedSongs = response.songs.indices.toSet()) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "AI 服务连接失败: ${e.localizedMessage ?: "请检查网络"}",
                    )
                }
            }
        }
    }

    private fun toggleSong(index: Int) {
        _state.update { state ->
            val selected = state.selectedSongs.toMutableSet()
            if (selected.contains(index)) selected.remove(index) else selected.add(index)
            state.copy(selectedSongs = selected)
        }
    }

    fun getSelectedFormatted(): String {
        val state = _state.value
        return state.songs
            .filterIndexed { index, _ -> index in state.selectedSongs }
            .joinToString("\n") { "${it.name} - ${it.artist}" }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val resp = api.history(20)
                _state.update {
                    it.copy(history = resp.items.map { h ->
                        HistoryItemUi(query = h.query, songCount = h.songCount, createdAt = h.createdAt)
                    })
                }
            } catch (_: Exception) { }
        }
    }
}
