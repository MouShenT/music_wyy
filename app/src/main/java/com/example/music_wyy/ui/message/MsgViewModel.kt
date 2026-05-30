package com.example.music_wyy.ui.message

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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class MsgUiState(
    val conversations: List<MsgConversation> = emptyList(),
    val messages: List<PrivateMsg> = emptyList(),
    val currentChatUid: String = "",
    val currentChatName: String = "",
    val isLoading: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val error: String? = null,
)

data class MsgConversation(
    val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val lastMsg: String,
    val lastTime: Long,
    val unread: Int,
)

data class PrivateMsg(
    val id: Long,
    val fromUid: Long,
    val msg: String,
    val time: Long,
    val isMine: Boolean,
)

class MsgViewModel(
    private val api: NeteaseApi,
    private val cookieStore: CookieStore,
    private val userSession: UserSession,
) : ViewModel() {

    private val _state = MutableStateFlow(MsgUiState())
    val state: StateFlow<MsgUiState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun loadConversations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val cookie = cookieStore.cookie.first() ?: ""
                val resp = api.getPrivateMsgs(cookie = "MUSIC_U=$cookie")
                val body = resp.string()
                val result = json.decodeFromString<PrivateMsgListResponse>(body)

                val myUid = userSession.state.value.userId
                val convs = result.msgs.map { msg ->
                    val isFromMe = msg.fromUser.userId == myUid
                    val other = if (isFromMe) msg.toUser else msg.fromUser
                    MsgConversation(
                        uid = other.userId.toString(),
                        nickname = other.nickname,
                        avatarUrl = other.avatarUrl,
                        lastMsg = extractLastMsgText(msg.lastMsg),
                        lastTime = msg.lastTime,
                        unread = if (isFromMe) 0 else msg.unread,
                    )
                }.distinctBy { it.uid }
                _state.update { it.copy(isLoading = false, conversations = convs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "加载失败: ${e.localizedMessage}") }
            }
        }
    }

    fun loadHistory(uid: String, nickname: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingDetail = true, currentChatUid = uid, currentChatName = nickname, messages = emptyList()) }
            try {
                val myUid = userSession.state.value.userId
                val cookie = cookieStore.cookie.first() ?: ""
                val resp = api.getPrivateMsgHistory(uid = uid, cookie = "MUSIC_U=$cookie")
                val body = resp.string()
                val result = json.decodeFromString<MsgHistoryResponse>(body)

                val msgs = result.msgs.map { msg ->
                    PrivateMsg(
                        id = msg.id,
                        fromUid = msg.fromUid,
                        msg = msg.msg,
                        time = msg.time,
                        isMine = msg.fromUid == myUid,
                    )
                }.sortedBy { it.time }
                _state.update { it.copy(isLoadingDetail = false, messages = msgs) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingDetail = false, error = "加载历史失败: ${e.localizedMessage}") }
            }
        }
    }

    private fun extractLastMsgText(raw: String): String {
        // lastMsg may be JSON-encoded; extract human-readable portion
        if (raw.startsWith("{") || raw.startsWith("[")) {
            return try {
                val el = json.parseToJsonElement(raw)
                when {
                    el is kotlinx.serialization.json.JsonObject -> {
                        el["msg"]?.jsonPrimitive?.content
                            ?: el["content"]?.jsonPrimitive?.content
                            ?: el["lastMsg"]?.jsonPrimitive?.content
                            ?: raw
                    }
                    else -> raw
                }
            } catch (_: Exception) {
                raw
            }
        }
        return raw
    }
}

@Serializable
private data class PrivateMsgListResponse(
    val code: Int = -1,
    val msgs: List<ConvItem> = emptyList(),
    val more: Boolean = false,
)

@Serializable
private data class ConvItem(
    val fromUser: ConvUser = ConvUser(),
    val toUser: ConvUser = ConvUser(),
    val lastMsg: String = "",
    val lastTime: Long = 0,
    val unread: Int = 0,
)

@Serializable
private data class ConvUser(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = "",
)

@Serializable
private data class MsgHistoryResponse(
    val code: Int = -1,
    val msgs: List<MsgDetailItem> = emptyList(),
)

@Serializable
private data class MsgDetailItem(
    val id: Long = 0,
    val fromUid: Long = 0,
    val msg: String = "",
    val time: Long = 0,
)
