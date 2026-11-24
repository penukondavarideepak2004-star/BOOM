package com.boom.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boom.core.database.SessionStorage
import com.boom.core.model.AuthSession
import com.boom.core.model.MfaChallenge
import com.boom.core.network.api.BoomApiClient
import com.boom.core.network.model.LoginRequest
import com.boom.core.network.model.MfaVerifyRequest
import com.boom.core.network.model.RegisterRequest
import com.boom.core.network.model.VerifyContactRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    // Registration form
    val email: String = "",
    val phoneNumber: String = "",
    val displayName: String = "",
    val password: String = "",
    val isRegistering: Boolean = false,
    val registrationError: String? = null,

    // Verification form
    val emailOtp: String = "",
    val phoneOtp: String = "",
    val isEmailVerified: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val isVerifyingContact: Boolean = false,
    val verificationError: String? = null,
    val resendCooldownSeconds: Int = 0,

    // Login form
    val loginIdentifier: String = "",
    val loginPassword: String = "",
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,

    // MFA challenge
    val mfaChallenge: MfaChallenge? = null,
    val mfaCode: String = "",
    val isVerifyingMfa: Boolean = false,
    val mfaError: String? = null
)

sealed class AuthNavigationEvent {
    object NavigateToVerifyContact : AuthNavigationEvent()
    object NavigateToWelcome : AuthNavigationEvent()
    object NavigateToSignIn : AuthNavigationEvent()
    object NavigateToMfaChallenge : AuthNavigationEvent()
}

class AuthViewModel(
    private val apiClient: BoomApiClient,
    private val sessionStorage: SessionStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvents: SharedFlow<AuthNavigationEvent> = _navigationEvents.asSharedFlow()

    private var cooldownJob: Job? = null

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value.trim(), registrationError = null)
    }

    fun updatePhone(value: String) {
        _uiState.value = _uiState.value.copy(phoneNumber = value.trim(), registrationError = null)
    }

    fun updateDisplayName(value: String) {
        _uiState.value = _uiState.value.copy(displayName = value, registrationError = null)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, registrationError = null)
    }

    fun updateLoginIdentifier(value: String) {
        _uiState.value = _uiState.value.copy(loginIdentifier = value.trim(), loginError = null)
    }

    fun updateLoginPassword(value: String) {
        _uiState.value = _uiState.value.copy(loginPassword = value, loginError = null)
    }

    fun updateEmailOtp(value: String) {
        _uiState.value = _uiState.value.copy(emailOtp = value.trim().take(6), verificationError = null)
    }

    fun updatePhoneOtp(value: String) {
        _uiState.value = _uiState.value.copy(phoneOtp = value.trim().take(6), verificationError = null)
    }

    fun updateMfaCode(value: String) {
        _uiState.value = _uiState.value.copy(mfaCode = value.trim().take(6), mfaError = null)
    }

    // AUTH-01: Registration with strong credentials
    fun register() {
        val state = _uiState.value

        // Client-side pre-validation
        if (!state.email.contains("@") || !state.email.contains(".")) {
            _uiState.value = state.copy(registrationError = "Please enter a valid email address")
            return
        }
        if (state.phoneNumber.length < 10) {
            _uiState.value = state.copy(registrationError = "Please enter a valid phone number with country code")
            return
        }
        if (state.displayName.isBlank()) {
            _uiState.value = state.copy(registrationError = "Display name cannot be empty")
            return
        }
        if (state.password.length < 8) {
            _uiState.value = state.copy(registrationError = "Password must be at least 8 characters long")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRegistering = true, registrationError = null)
            val response = apiClient.register(
                RegisterRequest(
                    email = state.email,
                    phoneNumber = state.phoneNumber,
                    displayName = state.displayName,
                    passwordHash = state.password
                )
            )

            if (response.success && response.data != null) {
                _uiState.value = _uiState.value.copy(isRegistering = false)
                startResendCooldown(60)
                _navigationEvents.emit(AuthNavigationEvent.NavigateToVerifyContact)
            } else {
                _uiState.value = _uiState.value.copy(
                    isRegistering = false,
                    registrationError = response.error?.message ?: "Registration failed. Please try again."
                )
            }
        }
    }

    // AUTH-02: Verify email ownership
    fun verifyEmail() {
        val state = _uiState.value
        if (state.emailOtp.length != 6) {
            _uiState.value = state.copy(verificationError = "Enter 6-digit email verification code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingContact = true, verificationError = null)
            val response = apiClient.verifyContact(
                VerifyContactRequest(
                    contactType = "EMAIL",
                    identifier = state.email,
                    code = state.emailOtp
                )
            )

            if (response.success) {
                val updatedEmailVerified = true
                _uiState.value = _uiState.value.copy(
                    isVerifyingContact = false,
                    isEmailVerified = updatedEmailVerified
                )
                checkBothVerified()
            } else {
                _uiState.value = _uiState.value.copy(
                    isVerifyingContact = false,
                    verificationError = response.error?.message ?: "Invalid email code"
                )
            }
        }
    }

    // AUTH-03: Verify phone ownership using time-limited OTP
    fun verifyPhone() {
        val state = _uiState.value
        if (state.phoneOtp.length != 6) {
            _uiState.value = state.copy(verificationError = "Enter 6-digit SMS OTP code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingContact = true, verificationError = null)
            val response = apiClient.verifyContact(
                VerifyContactRequest(
                    contactType = "PHONE",
                    identifier = state.phoneNumber,
                    code = state.phoneOtp
                )
            )

            if (response.success) {
                val updatedPhoneVerified = true
                _uiState.value = _uiState.value.copy(
                    isVerifyingContact = false,
                    isPhoneVerified = updatedPhoneVerified
                )
                checkBothVerified()
            } else {
                _uiState.value = _uiState.value.copy(
                    isVerifyingContact = false,
                    verificationError = response.error?.message ?: "Invalid phone OTP"
                )
            }
        }
    }

    private suspend fun checkBothVerified() {
        if (_uiState.value.isEmailVerified && _uiState.value.isPhoneVerified) {
            _navigationEvents.emit(AuthNavigationEvent.NavigateToSignIn)
        }
    }

    // AUTH-05 & AUTH-08: Sign-in with generic error messages
    fun login(deviceReference: String = "Android Device") {
        val state = _uiState.value
        if (state.loginIdentifier.isBlank() || state.loginPassword.isBlank()) {
            _uiState.value = state.copy(loginError = "Please enter both identifier and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingIn = true, loginError = null)
            val response = apiClient.login(
                LoginRequest(
                    identifier = state.loginIdentifier,
                    password = state.loginPassword,
                    deviceReference = deviceReference
                )
            )

            if (response.success && response.data != null) {
                val session = response.data
                sessionStorage.saveSession(session)
                _uiState.value = _uiState.value.copy(isLoggingIn = false)

                if (session.requiresMfa) {
                    _navigationEvents.emit(AuthNavigationEvent.NavigateToMfaChallenge)
                } else {
                    _navigationEvents.emit(AuthNavigationEvent.NavigateToWelcome)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoggingIn = false,
                    loginError = response.error?.message ?: "Invalid credentials"
                )
            }
        }
    }

    // AUTH-06: MFA verification
    fun verifyMfa() {
        val state = _uiState.value
        val challenge = state.mfaChallenge ?: return

        if (state.mfaCode.length != 6) {
            _uiState.value = state.copy(mfaError = "Enter 6-digit authenticator code")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingMfa = true, mfaError = null)
            val response = apiClient.verifyMfa(
                MfaVerifyRequest(
                    challengeId = challenge.challengeId,
                    factorType = "TOTP",
                    code = state.mfaCode
                )
            )

            if (response.success && response.data != null) {
                sessionStorage.saveSession(response.data)
                _uiState.value = _uiState.value.copy(isVerifyingMfa = false)
                _navigationEvents.emit(AuthNavigationEvent.NavigateToWelcome)
            } else {
                _uiState.value = _uiState.value.copy(
                    isVerifyingMfa = false,
                    mfaError = response.error?.message ?: "Invalid MFA code"
                )
            }
        }
    }

    private fun startResendCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _uiState.value = _uiState.value.copy(resendCooldownSeconds = i)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(resendCooldownSeconds = 0)
        }
    }
}
