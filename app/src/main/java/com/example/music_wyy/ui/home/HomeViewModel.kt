package com.example.music_wyy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.music_wyy.data.local.datastore.CookieStore
import com.example.music_wyy.data.remote.NeteaseApi
import com.example.music_wyy.session.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class HomeUiState(
    val nickname: String? = null,
    val totalPlaylists: Int = 0,
    val totalSongs: Int = 0,
    val isLoading: Boolean = false,
    val todaySigned: Boolean = false,
    val todayPoints: Int = 0,
    // Search
    val searchQuery: String = "",
    val searchResults: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val searchMessage: String? = null,
    // Playlist picker
    val showPlaylistPicker: Boolean = false,
    val pendingSongId: Long = 0,
    val pendingSongName: String = "",
    val userPlaylists: List<PlaylistOption> = emptyList(),
    val addResultMessage: String? = null,
)

data class SearchResultItem(
    val id: Long,
    val name: String,
    val artist: String,
    val album: String,
    val coverUrl: String?,
)

data class PlaylistOption(
    val id: Long,
    val name: String,
    val trackCount: Int,
)

@Serializable
private data class SigninResponse(val code: Int = -1, val point: Int? = null)
@Serializable
private data class UserPlaylistResponse(val more: Boolean = false, val playlist: List<PlaylistCount> = emptyList())
@Serializable
private data class PlaylistCount(val trackCount: Int = 0)
@Serializable
private data class SearchResponse(val result: SearchResultData? = null)
@Serializable
private data class SearchResultData(val songs: List<SearchSongData>? = null, val songCount: Int = 0)
@Serializable
private data class SearchSongData(
    val id: Long = 0,
    val name: String = "",
    val ar: List<ArtistData> = emptyList(),
    val al: AlbumData = AlbumData(),
)
@Serializable
private data class ArtistData(val name: String = "")
@Serializable
private data class AlbumData(val name: String = "", val picUrl: String? = null)
@Serializable
private data class PlaylistListResponse(val playlist: List<PlaylistItemData> = emptyList())
@Serializable
private data class PlaylistItemData(val id: Long = 0, val name: String = "", val trackCount: Int = 0)

class HomeViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
    private val userSession: UserSession,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    init {
        val session = userSession.state.value
        if (session.isLoggedIn) {
            _state.update {
                it.copy(
                    nickname = session.nickname,
                    todaySigned = session.todaySigned,
                    todayPoints = session.todayPoints,
                )
            }
            loadOverview()
        }
    }

    fun loadOverview() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val cookie = cookieStore.cookie.first() ?: ""
                if (cookie.isBlank()) {
                    _state.update { it.copy(isLoading = false) }
                    return@launch
                }

                val uid = userSession.state.value.userId.toString()
                val resp = api.getUserPlaylists(uid, "MUSIC_U=$cookie")
                val body = resp.string()
                val result = json.decodeFromString<UserPlaylistResponse>(body)
                _state.update {
                    it.copy(
                        totalPlaylists = result.playlist.size,
                        totalSongs = result.playlist.sumOf { p -> p.trackCount },
                    )
                }

                // Check signin status (calling dailySignin returns -2 if already signed in)
                val signResp = api.dailySignin("MUSIC_U=$cookie")
                val signBody = signResp.string()
                val signResult = json.decodeFromString<SigninResponse>(signBody)
                if (signResult.code == -2) {
                    _state.update { it.copy(todaySigned = true) }
                    userSession.setSignedIn(true)
                } else if (signResult.code == 200) {
                    val points = signResult.point ?: 0
                    _state.update { it.copy(todaySigned = true, todayPoints = points) }
                    userSession.setSignedIn(true, points)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, searchMessage = null) }
    }

    fun search() {
        val query = _state.value.searchQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchResults = emptyList(), searchMessage = null) }
            try {
                val cookie = cookieStore.cookie.first() ?: ""
                val resp = api.search(keywords = query, limit = 20, cookie = "MUSIC_U=$cookie")
                val body = resp.string()
                val result = json.decodeFromString<SearchResponse>(body)
                val songs = result.result?.songs?.map { s ->
                    SearchResultItem(
                        id = s.id,
                        name = s.name,
                        artist = s.ar.joinToString("/") { it.name },
                        album = s.al.name,
                        coverUrl = s.al.picUrl,
                    )
                } ?: emptyList()
                _state.update {
                    it.copy(
                        isSearching = false,
                        searchResults = songs,
                        searchMessage = if (songs.isEmpty()) "未找到匹配歌曲" else "找到 ${songs.size} 首",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSearching = false, searchMessage = "搜索失败: ${e.localizedMessage}")
                }
            }
        }
    }

    fun showPlaylistPicker(songId: Long, songName: String) {
        viewModelScope.launch {
            _state.update { it.copy(pendingSongId = songId, pendingSongName = songName, showPlaylistPicker = true, addResultMessage = null) }
            try {
                val cookie = cookieStore.cookie.first() ?: ""
                val uid = userSession.state.value.userId.toString()
                val resp = api.getUserPlaylists(uid, "MUSIC_U=$cookie")
                val body = resp.string()
                val result = json.decodeFromString<PlaylistListResponse>(body)
                _state.update {
                    it.copy(userPlaylists = result.playlist.map { p ->
                        PlaylistOption(id = p.id, name = p.name, trackCount = p.trackCount)
                    })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) { }
        }
    }

    fun hidePlaylistPicker() {
        _state.update { it.copy(showPlaylistPicker = false, pendingSongId = 0, pendingSongName = "") }
    }

    fun addToPlaylist(playlistId: Long) {
        val songId = _state.value.pendingSongId
        if (songId == 0L) return

        viewModelScope.launch {
            try {
                val cookie = cookieStore.cookie.first() ?: ""
                api.addTracksToPlaylist(
                    op = "add",
                    pid = playlistId.toString(),
                    tracks = songId.toString(),
                    cookie = "MUSIC_U=$cookie",
                )
                _state.update {
                    it.copy(
                        showPlaylistPicker = false,
                        addResultMessage = "已添加「${it.pendingSongName}」到歌单",
                        pendingSongId = 0,
                        pendingSongName = "",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(addResultMessage = "添加失败: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearSearch() {
        _state.update {
            it.copy(searchQuery = "", searchResults = emptyList(), searchMessage = null, isSearching = false)
        }
    }
}
