package com.boom.feature.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.core.audio.AudioSyncEngine
import com.boom.core.audio.SyncDiagnostics
import com.boom.core.audio.SyncQuality
import com.boom.core.connectivity.ConnectionEvent
import com.boom.core.connectivity.NearbyTransport
import com.boom.core.database.SessionStorage
import com.boom.core.model.Participant
import com.boom.core.model.ParticipantStatus
import com.boom.core.model.Party
import com.boom.core.model.PartyState
import com.boom.core.network.api.BoomApiClient
import com.boom.core.network.model.CreatePartyRequest
import com.boom.core.network.model.JoinPartyRequest
import com.boom.core.security.BoomGuardian
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiscoveredPartyInfo(
    val endpointId: String,
    val hostDisplayName: String,
    val shortCode: String
)

data class PartyUiState(
    // Host Configuration
    val createPartyTitle: String = "Friday Night Boom",
    val maxParticipants: Int = 10,
    val requireHostApproval: Boolean = true,
    val isCreatingParty: Boolean = false,
    val partyCreationError: String? = null,

    // Active Party Metadata
    val activeParty: Party? = null,
    val isHost: Boolean = false,

    // Participant lists (Host view)
    val pendingApprovals: List<Participant> = emptyList(),
    val connectedParticipants: List<Participant> = emptyList(),

    // Join Flow (Participant view)
    val joinCodeInput: String = "",
    val isJoiningParty: Boolean = false,
    val joinStatus: String = "IDLE", // "IDLE", "PENDING_APPROVAL", "CONNECTED", "REJECTED"
    val joinError: String? = null,
    val discoveredNearbyParties: List<DiscoveredPartyInfo> = emptyList(),
    val isDiscoveringNearby: Boolean = false,

    // Local Playback & Volume Control (Participant endpoint)
    val localVolume: Float = 0.85f,
    val isMutedLocally: Boolean = false,
    val syncDiagnostics: SyncDiagnostics = SyncDiagnostics(),

    val isLeavingParty: Boolean = false
)

sealed class PartyNavigationEvent {
    data class NavigateToHostControlPanel(val partyId: String) : PartyNavigationEvent()
    data class NavigateToParticipantDashboard(val partyId: String) : PartyNavigationEvent()
    object NavigateToDashboard : PartyNavigationEvent()
    data class OpenChat(val partyId: String) : PartyNavigationEvent()
    data class OpenSecurityCentre(val partyId: String) : PartyNavigationEvent()
}

class PartyViewModel(
    private val apiClient: BoomApiClient,
    private val sessionStorage: SessionStorage,
    private val nearbyTransport: NearbyTransport,
    private val audioSyncEngine: AudioSyncEngine,
    private val guardian: BoomGuardian
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartyUiState())
    val uiState: StateFlow<PartyUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<PartyNavigationEvent>()
    val navigationEvents: SharedFlow<PartyNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        // Collect nearby transport events (discovery, connection, packets)
        viewModelScope.launch {
            nearbyTransport.events.collect { event ->
                when (event) {
                    is ConnectionEvent.DeviceDiscovered -> {
                        val current = _uiState.value.discoveredNearbyParties.toMutableList()
                        if (current.none { it.endpointId == event.endpointId }) {
                            current.add(DiscoveredPartyInfo(event.endpointId, event.endpointName, event.partyShortCode))
                            _uiState.value = _uiState.value.copy(discoveredNearbyParties = current)
                        }
                    }
                    is ConnectionEvent.Connected -> {
                        // Endpoint connected locally
                    }
                    is ConnectionEvent.Disconnected -> {
                        // Mark participant disconnected in roster
                        handleParticipantDisconnect(event.endpointId)
                    }
                    else -> {}
                }
            }
        }

        // Collect audio sync engine diagnostics
        viewModelScope.launch {
            audioSyncEngine.diagnostics.collect { diag ->
                _uiState.value = _uiState.value.copy(syncDiagnostics = diag)
            }
        }
    }

    fun updatePartyTitle(title: String) {
        _uiState.value = _uiState.value.copy(createPartyTitle = title, partyCreationError = null)
    }

    fun updateJoinCodeInput(code: String) {
        _uiState.value = _uiState.value.copy(joinCodeInput = code.uppercase().trim().take(8), joinError = null)
    }

    // HOST-02: Create party with unique short code and QR payload
    fun createParty() {
        val title = _uiState.value.createPartyTitle
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(partyCreationError = "Please enter a party title")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingParty = true, partyCreationError = null)
            val response = apiClient.createParty(
                CreatePartyRequest(
                    title = title,
                    maxParticipants = _uiState.value.maxParticipants,
                    requireHostApproval = _uiState.value.requireHostApproval
                )
            )

            if (response.success && response.data != null) {
                val party = response.data
                _uiState.value = _uiState.value.copy(
                    isCreatingParty = false,
                    activeParty = party,
                    isHost = true
                )

                // Start advertising nearby for local device discovery
                nearbyTransport.startAdvertising(party.partyId, party.hostDisplayName, party.shortCode)

                _navigationEvents.emit(PartyNavigationEvent.NavigateToHostControlPanel(party.partyId))
            } else {
                _uiState.value = _uiState.value.copy(
                    isCreatingParty = false,
                    partyCreationError = response.error?.message ?: "Failed to create party"
                )
            }
        }
    }

    // HOST-03: Host approves participant join request
    fun approveParticipant(participantId: String) {
        val partyId = _uiState.value.activeParty?.partyId ?: return
        viewModelScope.launch {
            val response = apiClient.approveParticipant(partyId, participantId)
            if (response.success) {
                val pending = _uiState.value.pendingApprovals.toMutableList()
                val approved = pending.find { it.participantId == participantId }
                if (approved != null) {
                    pending.remove(approved)
                    val connected = _uiState.value.connectedParticipants.toMutableList()
                    connected.add(approved.copy(status = ParticipantStatus.CONNECTED))
                    _uiState.value = _uiState.value.copy(
                        pendingApprovals = pending,
                        connectedParticipants = connected
                    )
                }
            }
        }
    }

    // HOST-05: Host removes a participant and revokes their credentials
    fun removeParticipant(participantId: String) {
        val partyId = _uiState.value.activeParty?.partyId ?: return
        viewModelScope.launch {
            apiClient.removeParticipant(partyId, participantId)
            val connected = _uiState.value.connectedParticipants.toMutableList()
            connected.removeAll { it.participantId == participantId }
            _uiState.value = _uiState.value.copy(connectedParticipants = connected)
            nearbyTransport.disconnect(participantId)
        }
    }

    // JOIN-01 & JOIN-02: Participant join by code or QR
    fun joinParty(shortCodeOverride: String? = null) {
        val code = shortCodeOverride ?: _uiState.value.joinCodeInput
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(joinError = "Please enter the 6-character party code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isJoiningParty = true, joinError = null)
            val response = apiClient.joinParty(
                JoinPartyRequest(
                    shortCode = code,
                    userDisplayName = "Alex Guest",
                    reportedOutputRoute = "Built-in Speaker"
                )
            )

            if (response.success && response.data != null) {
                val party = response.data
                _uiState.value = _uiState.value.copy(
                    isJoiningParty = false,
                    activeParty = party,
                    isHost = false,
                    joinStatus = if (party.settings.requireHostApproval) "PENDING_APPROVAL" else "CONNECTED"
                )

                _navigationEvents.emit(PartyNavigationEvent.NavigateToParticipantDashboard(party.partyId))
            } else {
                _uiState.value = _uiState.value.copy(
                    isJoiningParty = false,
                    joinError = response.error?.message ?: "Unable to join party. Invalid code."
                )
            }
        }
    }

    // Nearby discovery toggle
    fun toggleNearbyDiscovery() {
        val currentlyDiscovering = _uiState.value.isDiscoveringNearby
        viewModelScope.launch {
            if (currentlyDiscovering) {
                nearbyTransport.stopDiscovery()
                _uiState.value = _uiState.value.copy(isDiscoveringNearby = false)
            } else {
                nearbyTransport.startDiscovery()
                _uiState.value = _uiState.value.copy(isDiscoveringNearby = true)
            }
        }
    }

    // FR-06: Local volume adjustment (independent of host or other participants)
    fun setLocalVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _uiState.value = _uiState.value.copy(localVolume = clamped, isMutedLocally = clamped == 0f)
    }

    // FR-06: Toggle local mute
    fun toggleLocalMute() {
        val isMuted = !_uiState.value.isMutedLocally
        _uiState.value = _uiState.value.copy(isMutedLocally = isMuted)
    }

    // JOIN-04 & FR-06: Instant leave without requiring host approval
    fun leaveParty() {
        val partyId = _uiState.value.activeParty?.partyId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLeavingParty = true)
            apiClient.leaveParty(partyId)
            nearbyTransport.stopAdvertising()
            nearbyTransport.stopDiscovery()

            _uiState.value = _uiState.value.copy(
                activeParty = null,
                isHost = false,
                isLeavingParty = false,
                joinStatus = "IDLE",
                connectedParticipants = emptyList(),
                pendingApprovals = emptyList()
            )

            _navigationEvents.emit(PartyNavigationEvent.NavigateToDashboard)
        }
    }

    private fun handleParticipantDisconnect(endpointId: String) {
        val connected = _uiState.value.connectedParticipants.toMutableList()
        val index = connected.indexOfFirst { it.participantId == endpointId }
        if (index != -1) {
            connected[index] = connected[index].copy(status = ParticipantStatus.DISCONNECTED)
            _uiState.value = _uiState.value.copy(connectedParticipants = connected)
        }
    }

    // Test simulation helpers
    fun simulateIncomingJoinRequest(participant: Participant) {
        val pending = _uiState.value.pendingApprovals.toMutableList()
        pending.add(participant)
        _uiState.value = _uiState.value.copy(pendingApprovals = pending)
    }
}
