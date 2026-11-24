package com.boom.core.connectivity

import com.boom.core.model.AudioSyncPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ConnectionEvent {
    data class DeviceDiscovered(val endpointId: String, val endpointName: String, val partyShortCode: String) : ConnectionEvent()
    data class PairingRequestReceived(val endpointId: String, val authenticationToken: String) : ConnectionEvent()
    data class Connected(val endpointId: String) : ConnectionEvent()
    data class Disconnected(val endpointId: String, val reason: String) : ConnectionEvent()
    data class PacketReceived(val endpointId: String, val packet: AudioSyncPacket) : ConnectionEvent()
}

enum class TransportState {
    IDLE,
    ADVERTISING,
    DISCOVERING,
    CONNECTED,
    ERROR
}

interface NearbyTransport {
    val transportState: StateFlow<TransportState>
    val events: Flow<ConnectionEvent>

    suspend fun startAdvertising(partyId: String, hostDisplayName: String, shortCode: String)
    suspend fun stopAdvertising()

    suspend fun startDiscovery()
    suspend fun stopDiscovery()

    suspend fun requestPairing(endpointId: String, clientDisplayName: String)
    suspend fun acceptPairing(endpointId: String)
    suspend fun rejectPairing(endpointId: String)

    suspend fun sendPacket(endpointId: String, packet: AudioSyncPacket)
    suspend fun broadcastPacket(packet: AudioSyncPacket)

    suspend fun disconnect(endpointId: String)
}

/**
 * High-fidelity testable simulator for local peer networking without requiring physical radio hardware.
 */
class LocalLoopbackTransport : NearbyTransport {
    private val _transportState = MutableStateFlow(TransportState.IDLE)
    override val transportState: StateFlow<TransportState> = _transportState.asStateFlow()

    private val _events = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)
    override val events: Flow<ConnectionEvent> = _events.asSharedFlow()

    private val connectedEndpoints = mutableSetOf<String>()

    override suspend fun startAdvertising(partyId: String, hostDisplayName: String, shortCode: String) {
        _transportState.value = TransportState.ADVERTISING
    }

    override suspend fun stopAdvertising() {
        if (_transportState.value == TransportState.ADVERTISING) {
            _transportState.value = TransportState.IDLE
        }
    }

    override suspend fun startDiscovery() {
        _transportState.value = TransportState.DISCOVERING
    }

    override suspend fun stopDiscovery() {
        if (_transportState.value == TransportState.DISCOVERING) {
            _transportState.value = TransportState.IDLE
        }
    }

    override suspend fun requestPairing(endpointId: String, clientDisplayName: String) {
        _events.emit(ConnectionEvent.PairingRequestReceived(endpointId, "SAS-789-BOOM"))
    }

    override suspend fun acceptPairing(endpointId: String) {
        connectedEndpoints.add(endpointId)
        _transportState.value = TransportState.CONNECTED
        _events.emit(ConnectionEvent.Connected(endpointId))
    }

    override suspend fun rejectPairing(endpointId: String) {
        _events.emit(ConnectionEvent.Disconnected(endpointId, "REJECTED_BY_HOST"))
    }

    override suspend fun sendPacket(endpointId: String, packet: AudioSyncPacket) {
        _events.emit(ConnectionEvent.PacketReceived(endpointId, packet))
    }

    override suspend fun broadcastPacket(packet: AudioSyncPacket) {
        connectedEndpoints.forEach { id ->
            _events.emit(ConnectionEvent.PacketReceived(id, packet))
        }
    }

    override suspend fun disconnect(endpointId: String) {
        connectedEndpoints.remove(endpointId)
        _events.emit(ConnectionEvent.Disconnected(endpointId, "LOCAL_DISCONNECT"))
        if (connectedEndpoints.isEmpty()) {
            _transportState.value = TransportState.IDLE
        }
    }

    // Test simulation helpers
    suspend fun simulateDiscoveredParty(endpointId: String, hostName: String, shortCode: String) {
        _events.emit(ConnectionEvent.DeviceDiscovered(endpointId, hostName, shortCode))
    }
}
