package com.boom.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.core.database.SessionStorage
import com.boom.core.model.ChatMessage
import com.boom.core.model.DeliveryState
import com.boom.core.model.MessageVisibility
import com.boom.core.model.SecurityEvent
import com.boom.core.model.SecurityEventType
import com.boom.core.model.SecuritySeverity
import com.boom.core.network.api.BoomApiClient
import com.boom.core.network.model.SendChatMessageRequest
import com.boom.core.security.BoomGuardian
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ChatFilter {
    ALL,
    HOST_ONLY,
    ANNOUNCEMENTS
}

sealed class ChatNavigationEvent {
    object NavigateBack : ChatNavigationEvent()
}

data class ChatUiState(
    val partyId: String = "",
    val currentUserId: String = "",
    val currentDisplayName: String = "",
    val isHost: Boolean = false,
    val hostUserId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val activeFilter: ChatFilter = ChatFilter.ALL,
    val inputText: String = "",
    val selectedVisibility: MessageVisibility = MessageVisibility.EVERYONE,
    val replyingToMessage: ChatMessage? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Defense-in-depth object-level chat privacy filtering.
     * Enforces CHAT-01 through CHAT-06:
     * - EVERYONE: visible to all party members.
     * - HOST_ANNOUNCEMENT: visible to all party members.
     * - HOST_ONLY: strictly visible ONLY to host and the specific participant sender.
     *   Participant B can NEVER see Participant A's private messages to the host.
     */
    val filteredMessages: List<ChatMessage>
        get() {
            val visibleToUser = messages.filter { msg ->
                when (msg.visibility) {
                    MessageVisibility.EVERYONE -> true
                    MessageVisibility.HOST_ANNOUNCEMENT -> true
                    MessageVisibility.HOST_ONLY -> {
                        // Visible if current user is the sender
                        msg.senderUserId == currentUserId ||
                        // Visible if current user is the host
                        isHost ||
                        // Visible if current user is the explicitly addressed recipient
                        msg.recipientUserId == currentUserId
                    }
                }
            }

            return when (activeFilter) {
                ChatFilter.ALL -> visibleToUser
                ChatFilter.HOST_ONLY -> visibleToUser.filter { it.visibility == MessageVisibility.HOST_ONLY }
                ChatFilter.ANNOUNCEMENTS -> visibleToUser.filter { it.visibility == MessageVisibility.HOST_ANNOUNCEMENT }
            }
        }
}

class ChatViewModel(
    private val apiClient: BoomApiClient,
    private val sessionStorage: SessionStorage,
    private val guardian: BoomGuardian,
    partyId: String = "",
    isHost: Boolean = false,
    hostUserId: String = ""
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            partyId = partyId,
            isHost = isHost,
            hostUserId = hostUserId
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ChatNavigationEvent>()
    val navigationEvents: SharedFlow<ChatNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        sessionStorage.getSession()?.let { session ->
            _uiState.update {
                it.copy(
                    currentUserId = session.userId,
                    currentDisplayName = session.user.displayName,
                    isHost = if (isHost) true else (session.userId == hostUserId)
                )
            }
        }
        if (partyId.isNotBlank()) {
            loadMessages()
        }
    }

    fun setPartyContext(partyId: String, isHost: Boolean, hostUserId: String) {
        _uiState.update {
            it.copy(
                partyId = partyId,
                isHost = isHost,
                hostUserId = hostUserId
            )
        }
        loadMessages()
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun setFilter(filter: ChatFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    fun setSelectedVisibility(visibility: MessageVisibility) {
        // Only hosts can send announcements
        val finalVisibility = if (visibility == MessageVisibility.HOST_ANNOUNCEMENT && !_uiState.value.isHost) {
            MessageVisibility.EVERYONE
        } else {
            visibility
        }
        _uiState.update { it.copy(selectedVisibility = finalVisibility) }
    }

    fun startReplyingTo(message: ChatMessage) {
        val state = _uiState.value
        if (state.isHost) {
            // Host replying privately to participant
            _uiState.update {
                it.copy(
                    replyingToMessage = message,
                    selectedVisibility = MessageVisibility.HOST_ONLY
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    replyingToMessage = message,
                    selectedVisibility = if (message.visibility == MessageVisibility.HOST_ONLY) MessageVisibility.HOST_ONLY else MessageVisibility.EVERYONE
                )
            }
        }
    }

    fun clearReply() {
        _uiState.update {
            it.copy(
                replyingToMessage = null,
                selectedVisibility = MessageVisibility.EVERYONE
            )
        }
    }

    fun loadMessages() {
        val state = _uiState.value
        if (state.partyId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val response = apiClient.getMessages(state.partyId)
            if (response.success && response.data != null) {
                _uiState.update {
                    it.copy(
                        messages = response.data,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = response.error?.message
                    )
                }
            }
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val content = state.inputText.trim()
        if (content.isBlank() || state.partyId.isBlank()) return

        val messageId = "msg-${UUID.randomUUID().toString().take(8)}"
        val recipientUserId = when {
            state.selectedVisibility == MessageVisibility.HOST_ONLY && state.isHost -> {
                state.replyingToMessage?.senderUserId
            }
            state.selectedVisibility == MessageVisibility.HOST_ONLY && !state.isHost -> {
                state.hostUserId.ifBlank { null }
            }
            else -> null
        }

        val optimisticMessage = ChatMessage(
            messageId = messageId,
            partyId = state.partyId,
            senderUserId = state.currentUserId.ifBlank { "anon-user" },
            senderDisplayName = state.currentDisplayName.ifBlank { if (state.isHost) "Host" else "Guest" },
            isFromHost = state.isHost,
            visibility = state.selectedVisibility,
            recipientUserId = recipientUserId,
            content = content,
            deliveryState = DeliveryState.PENDING,
            timestampEpochMs = System.currentTimeMillis()
        )

        // Optimistically add message
        _uiState.update {
            it.copy(
                messages = it.messages + optimisticMessage,
                inputText = "",
                replyingToMessage = null,
                isSending = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val request = SendChatMessageRequest(
                partyId = state.partyId,
                content = content,
                visibility = state.selectedVisibility.name,
                recipientUserId = recipientUserId
            )

            val response = apiClient.sendMessage(request)
            if (response.success && response.data != null) {
                val delivered = response.data.copy(deliveryState = DeliveryState.DELIVERED)
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages.map { msg ->
                            if (msg.messageId == messageId) delivered else msg
                        },
                        isSending = false
                    )
                }
            } else {
                // Update to SENT or FAILED
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages.map { msg ->
                            if (msg.messageId == messageId) msg.copy(deliveryState = DeliveryState.SENT) else msg
                        },
                        isSending = false
                    )
                }
            }
        }
    }

    /**
     * Direct local packet / incoming message handler for Nearby P2P mesh
     */
    fun receiveIncomingMessage(message: ChatMessage) {
        val state = _uiState.value
        // Verify privacy boundaries before admitting message into local state
        val canReceive = when (message.visibility) {
            MessageVisibility.EVERYONE -> true
            MessageVisibility.HOST_ANNOUNCEMENT -> true
            MessageVisibility.HOST_ONLY -> {
                message.senderUserId == state.currentUserId ||
                state.isHost ||
                message.recipientUserId == state.currentUserId
            }
        }

        if (canReceive) {
            _uiState.update {
                // Avoid duplicates
                if (it.messages.any { existing -> existing.messageId == message.messageId }) {
                    it
                } else {
                    it.copy(messages = it.messages + message.copy(deliveryState = DeliveryState.DELIVERED))
                }
            }
        } else {
            // Defense-in-depth: Guardian audit event on blocked private leak attempt
            guardian.recordEvent(
                eventType = SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT,
                severity = SecuritySeverity.HIGH,
                actorReference = message.senderUserId,
                partyId = state.partyId,
                result = "BLOCKED",
                actionTaken = "DROPPED_PACKET",
                rawDetails = "Blocked unauthorized private chat delivery attempt from ${message.senderUserId}"
            )
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            _navigationEvents.emit(ChatNavigationEvent.NavigateBack)
        }
    }
}
