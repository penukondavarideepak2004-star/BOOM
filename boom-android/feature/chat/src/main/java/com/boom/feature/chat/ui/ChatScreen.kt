package com.boom.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boom.core.designsystem.theme.BackgroundDark
import com.boom.core.designsystem.theme.ElectricIndigo
import com.boom.core.designsystem.theme.NeonCoral
import com.boom.core.designsystem.theme.NeonCyan
import com.boom.core.designsystem.theme.SurfaceDark
import com.boom.core.designsystem.theme.SurfaceVariantDark
import com.boom.core.model.ChatMessage
import com.boom.core.model.DeliveryState
import com.boom.core.model.MessageVisibility
import com.boom.feature.chat.ChatFilter
import com.boom.feature.chat.ChatNavigationEvent
import com.boom.feature.chat.ChatViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is ChatNavigationEvent.NavigateBack -> onBack()
            }
        }
    }

    // Auto scroll to bottom on new message
    LaunchedEffect(uiState.filteredMessages.size) {
        if (uiState.filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.filteredMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Unified Party Chat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (uiState.isHost) "Hosting Party • All Channels" else "Guest View • Multi-channel",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClicked() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips row (CHAT-08)
            FilterChipsRow(
                activeFilter = uiState.activeFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            // Dynamic Privacy Banner (CHAT-04, CHAT-07)
            PrivacyStatusBanner(
                visibility = uiState.selectedVisibility,
                isHost = uiState.isHost
            )

            // Chat timeline (CHAT-01, CHAT-02)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading && uiState.messages.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = ElectricIndigo
                    )
                } else if (uiState.filteredMessages.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No messages yet in this channel",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.filteredMessages, key = { it.messageId }) { message ->
                            val isOutgoing = message.senderUserId == uiState.currentUserId
                            ChatMessageBubble(
                                message = message,
                                isOutgoing = isOutgoing,
                                isHostViewer = uiState.isHost,
                                onReplyPrivately = { viewModel.startReplyingTo(message) }
                            )
                        }
                    }
                }
            }

            // Reply Preview Banner (CHAT-05)
            uiState.replyingToMessage?.let { replying ->
                Surface(
                    color = SurfaceVariantDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = null,
                            tint = NeonCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying privately to ${replying.senderDisplayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCoral,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = replying.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearReply() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel reply",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }

            // Bottom Input Row (CHAT-03, CHAT-06, CHAT-07, CHAT-09)
            ChatInputBar(
                inputText = uiState.inputText,
                selectedVisibility = uiState.selectedVisibility,
                isHost = uiState.isHost,
                isSending = uiState.isSending,
                onInputChanged = { viewModel.updateInputText(it) },
                onVisibilityChanged = { viewModel.setSelectedVisibility(it) },
                onSend = { viewModel.sendMessage() }
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    activeFilter: ChatFilter,
    onFilterSelected: (ChatFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = activeFilter == ChatFilter.ALL,
            onClick = { onFilterSelected(ChatFilter.ALL) },
            label = { Text("All Messages") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElectricIndigo,
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = activeFilter == ChatFilter.HOST_ONLY,
            onClick = { onFilterSelected(ChatFilter.HOST_ONLY) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            },
            label = { Text("Host Only") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = NeonCoral,
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = activeFilter == ChatFilter.ANNOUNCEMENTS,
            onClick = { onFilterSelected(ChatFilter.ANNOUNCEMENTS) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            },
            label = { Text("Announcements") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = NeonCyan,
                selectedLabelColor = Color.Black
            )
        )
    }
}

@Composable
private fun PrivacyStatusBanner(
    visibility: MessageVisibility,
    isHost: Boolean
) {
    when (visibility) {
        MessageVisibility.HOST_ONLY -> {
            Surface(
                color = NeonCoral.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, NeonCoral.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NeonCoral,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHost) {
                            "Private Channel: Replying directly to participant. Hidden from party."
                        } else {
                            "Private to Host: Only you and the Host can see this message."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
        MessageVisibility.HOST_ANNOUNCEMENT -> {
            Surface(
                color = NeonCyan.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Broadcast Announcement: Highlighted for all party members.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
        MessageVisibility.EVERYONE -> Unit
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isOutgoing: Boolean,
    isHostViewer: Boolean,
    onReplyPrivately: () -> Unit
) {
    val isAnnouncement = message.visibility == MessageVisibility.HOST_ANNOUNCEMENT
    val isPrivate = message.visibility == MessageVisibility.HOST_ONLY

    val bubbleColor = when {
        isAnnouncement -> ElectricIndigo.copy(alpha = 0.35f)
        isPrivate -> NeonCoral.copy(alpha = 0.25f)
        isOutgoing -> ElectricIndigo.copy(alpha = 0.85f)
        else -> SurfaceDark
    }

    val borderColor = when {
        isAnnouncement -> NeonCyan
        isPrivate -> NeonCoral
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAnnouncement) Alignment.CenterHorizontally else if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOutgoing) 16.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier
                .width(if (isAnnouncement) 340.dp else 280.dp)
                .border(
                    width = if (isAnnouncement || isPrivate) 1.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header: Sender name, Host tag, Privacy tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.senderDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (message.isFromHost) NeonCyan else Color.White
                        )
                        if (message.isFromHost) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = NeonCyan.copy(alpha = 0.2f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "HOST",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Visibility Badge
                    if (isPrivate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Private to Host",
                                tint = NeonCoral,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Host Only",
                                fontSize = 10.sp,
                                color = NeonCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (isAnnouncement) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Announcement",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Broadcast",
                                fontSize = 10.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message content
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Footer: Timestamp & delivery state indicator (CHAT-09)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Host Action: Reply privately button (CHAT-05)
                    if (isHostViewer && isPrivate && !isOutgoing) {
                        TextButton(
                            onClick = onReplyPrivately,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = "Reply Privately",
                                tint = NeonCoral,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Reply Privately",
                                fontSize = 10.sp,
                                color = NeonCoral
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTimestamp(message.timestampEpochMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color.LightGray.copy(alpha = 0.7f)
                        )

                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            DeliveryIndicator(state = message.deliveryState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryIndicator(state: DeliveryState) {
    when (state) {
        DeliveryState.PENDING -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Pending",
                tint = Color.LightGray,
                modifier = Modifier.size(12.dp)
            )
        }
        DeliveryState.SENT -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = Color.LightGray,
                modifier = Modifier.size(12.dp)
            )
        }
        DeliveryState.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = NeonCyan,
                modifier = Modifier.size(14.dp)
            )
        }
        DeliveryState.FAILED -> {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Failed",
                tint = NeonCoral,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    selectedVisibility: MessageVisibility,
    isHost: Boolean,
    isSending: Boolean,
    onInputChanged: (String) -> Unit,
    onVisibilityChanged: (MessageVisibility) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visibility Channel Selector Toggle (CHAT-03, CHAT-06, CHAT-07)
            IconButton(
                onClick = {
                    val next = when (selectedVisibility) {
                        MessageVisibility.EVERYONE -> MessageVisibility.HOST_ONLY
                        MessageVisibility.HOST_ONLY -> if (isHost) MessageVisibility.HOST_ANNOUNCEMENT else MessageVisibility.EVERYONE
                        MessageVisibility.HOST_ANNOUNCEMENT -> MessageVisibility.EVERYONE
                    }
                    onVisibilityChanged(next)
                }
            ) {
                Icon(
                    imageVector = when (selectedVisibility) {
                        MessageVisibility.EVERYONE -> Icons.Default.Public
                        MessageVisibility.HOST_ONLY -> Icons.Default.Lock
                        MessageVisibility.HOST_ANNOUNCEMENT -> Icons.Default.Campaign
                    },
                    contentDescription = "Change Channel",
                    tint = when (selectedVisibility) {
                        MessageVisibility.EVERYONE -> Color.White
                        MessageVisibility.HOST_ONLY -> NeonCoral
                        MessageVisibility.HOST_ANNOUNCEMENT -> NeonCyan
                    }
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        text = when (selectedVisibility) {
                            MessageVisibility.EVERYONE -> "Message everyone..."
                            MessageVisibility.HOST_ONLY -> "Private message to Host..."
                            MessageVisibility.HOST_ANNOUNCEMENT -> "Broadcast announcement..."
                        },
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = when (selectedVisibility) {
                        MessageVisibility.EVERYONE -> ElectricIndigo
                        MessageVisibility.HOST_ONLY -> NeonCoral
                        MessageVisibility.HOST_ANNOUNCEMENT -> NeonCyan
                    },
                    unfocusedBorderColor = SurfaceVariantDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = inputText.isNotBlank() && !isSending,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isSending) {
                            when (selectedVisibility) {
                                MessageVisibility.EVERYONE -> ElectricIndigo
                                MessageVisibility.HOST_ONLY -> NeonCoral
                                MessageVisibility.HOST_ANNOUNCEMENT -> NeonCyan
                            }
                        } else SurfaceVariantDark
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (selectedVisibility == MessageVisibility.HOST_ANNOUNCEMENT) Color.Black else Color.White
                )
            }
        }
    }
}

private fun formatTimestamp(epochMs: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMs))
}
