package com.boom.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.core.database.SessionStorage
import com.boom.core.model.AuthSession
import com.boom.core.security.BoomGuardian
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userId: String = "",
    val displayName: String = "User",
    val email: String = "",
    val securityScore: Int = 100,
    val securityStatusLabel: String = "Protected",
    val activePartyId: String? = null,
    val isLoggingOut: Boolean = false
)

sealed class DashboardNavigationEvent {
    object NavigateToHostParty : DashboardNavigationEvent()
    object NavigateToJoinParty : DashboardNavigationEvent()
    data class NavigateToActiveParty(val partyId: String) : DashboardNavigationEvent()
    object NavigateToSecurityCentre : DashboardNavigationEvent()
    object NavigateToSignIn : DashboardNavigationEvent()
}

class DashboardViewModel(
    private val sessionStorage: SessionStorage,
    private val guardian: BoomGuardian
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<DashboardNavigationEvent>()
    val navigationEvents: SharedFlow<DashboardNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        loadSessionData()
    }

    fun loadSessionData() {
        val session = sessionStorage.getSession()
        if (session != null) {
            val score = guardian.computePartySecurityScore("global")
            _uiState.value = _uiState.value.copy(
                userId = session.userId,
                displayName = "Alex Sound", // Cached user profile
                email = "user@boom.party",
                securityScore = score,
                securityStatusLabel = if (score >= 90) "Protected" else "Action Recommended"
            )
        }
    }

    fun onHostPartyClicked() {
        viewModelScope.launch {
            _navigationEvents.emit(DashboardNavigationEvent.NavigateToHostParty)
        }
    }

    fun onJoinPartyClicked() {
        viewModelScope.launch {
            _navigationEvents.emit(DashboardNavigationEvent.NavigateToJoinParty)
        }
    }

    fun onSecurityCentreClicked() {
        viewModelScope.launch {
            _navigationEvents.emit(DashboardNavigationEvent.NavigateToSecurityCentre)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            sessionStorage.clearSession()
            _navigationEvents.emit(DashboardNavigationEvent.NavigateToSignIn)
        }
    }
}
