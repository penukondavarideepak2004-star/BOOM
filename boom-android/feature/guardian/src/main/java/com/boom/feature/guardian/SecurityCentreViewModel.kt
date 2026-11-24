package com.boom.feature.guardian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.core.model.SecurityEvent
import com.boom.core.model.SecuritySeverity
import com.boom.core.security.BoomGuardian
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DefenseLayer(
    val title: String,
    val description: String,
    val standard: String,
    val isActive: Boolean = true
)

sealed class SecurityNavigationEvent {
    object NavigateBack : SecurityNavigationEvent()
}

data class SecurityUiState(
    val partyId: String? = null,
    val overallScore: Int = 100,
    val protectionStatus: String = "ALL DEFENSES ACTIVE",
    val recentEvents: List<SecurityEvent> = emptyList(),
    val activeDefenses: List<DefenseLayer> = listOf(
        DefenseLayer(
            title = "Hardware Keystore Encryption",
            description = "Session auth tokens & cryptographic seeds encrypted with hardware-backed Android Keystore.",
            standard = "AES-GCM-256 / Keystore SPI",
            isActive = true
        ),
        DefenseLayer(
            title = "Transport Layer Security (TLS 1.3 & Wi-Fi Direct)",
            description = "Mutual party authentication with ephemeral keys. Direct local mesh traffic encrypted.",
            standard = "TLS 1.3 / WPA2-PSK Peer Mesh",
            isActive = true
        ),
        DefenseLayer(
            title = "Object-Level Chat Privacy",
            description = "Host-Only messages cryptographically isolated. Zero participant-to-participant surveillance.",
            standard = "Server & Client Access Policy",
            isActive = true
        ),
        DefenseLayer(
            title = "Abuse Throttling & Replay Guard",
            description = "Sliding-window rate limiter blocks brute-force attempts. Clock timestamps protect replay.",
            standard = "5-Attempt Threshold / NTP Token",
            isActive = true
        )
    ),
    val privacyAssurances: List<String> = listOf(
        "Zero Device File Inspection: BOOM never reads documents, photos, or storage outside its sandbox.",
        "Zero Background Surveillance: No microphone recording, no voice calling, no location harvesting.",
        "Zero Credential Leakage: Passwords, OTPs, and authentication headers are masked before audit logging."
    ),
    val filterSeverity: SecuritySeverity? = null
) {
    val filteredEvents: List<SecurityEvent>
        get() = if (filterSeverity == null) {
            recentEvents
        } else {
            recentEvents.filter { it.severity == filterSeverity }
        }
}

class SecurityCentreViewModel(
    private val guardian: BoomGuardian,
    partyId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState(partyId = partyId))
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SecurityNavigationEvent>()
    val navigationEvents: SharedFlow<SecurityNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        refreshSecurityState()
    }

    fun setPartyContext(partyId: String?) {
        _uiState.update { it.copy(partyId = partyId) }
        refreshSecurityState()
    }

    fun filterBySeverity(severity: SecuritySeverity?) {
        _uiState.update { it.copy(filterSeverity = severity) }
    }

    fun refreshSecurityState() {
        val partyId = _uiState.value.partyId
        val rawEvents = guardian.getRecentEvents(partyId).reversed() // latest first
        val score = if (partyId != null && partyId != "global") {
            guardian.computePartySecurityScore(partyId)
        } else {
            val critical = rawEvents.count { it.severity == SecuritySeverity.CRITICAL }
            val high = rawEvents.count { it.severity == SecuritySeverity.HIGH }
            val medium = rawEvents.count { it.severity == SecuritySeverity.MEDIUM }
            (100 - (critical * 30 + high * 15 + medium * 5)).coerceIn(0, 100)
        }

        val status = when {
            score >= 90 -> "ALL DEFENSES ACTIVE"
            score >= 70 -> "ELEVATED VIGILANCE"
            score >= 50 -> "THREATS MITIGATED"
            else -> "ACTION REQUIRED"
        }

        _uiState.update {
            it.copy(
                overallScore = score,
                protectionStatus = status,
                recentEvents = rawEvents
            )
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            _navigationEvents.emit(SecurityNavigationEvent.NavigateBack)
        }
    }
}
