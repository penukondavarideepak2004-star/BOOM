package com.boom.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.boom.app.BoomApplication
import com.boom.core.audio.AudioSyncEngine
import com.boom.core.connectivity.LocalLoopbackTransport
import com.boom.core.database.InMemorySessionStorage
import com.boom.core.network.api.BoomApiClient
import com.boom.core.security.BoomGuardian
import com.boom.feature.auth.AuthNavigationEvent
import com.boom.feature.auth.AuthViewModel
import com.boom.feature.auth.ui.MfaChallengeScreen
import com.boom.feature.auth.ui.SignInScreen
import com.boom.feature.auth.ui.SignUpScreen
import com.boom.feature.auth.ui.VerifyContactScreen
import com.boom.feature.dashboard.DashboardNavigationEvent
import com.boom.feature.dashboard.DashboardScreen
import com.boom.feature.dashboard.DashboardViewModel
import com.boom.feature.activities.ActivitiesViewModel
import com.boom.feature.activities.ui.ActivitiesHubScreen
import com.boom.feature.guardian.SecurityCentreViewModel
import com.boom.feature.guardian.ui.SecurityCentreScreen
import com.boom.feature.chat.ChatViewModel
import com.boom.feature.chat.ui.ChatScreen
import com.boom.feature.party.PartyNavigationEvent
import com.boom.feature.party.PartyViewModel
import com.boom.feature.party.ui.HostControlPanelScreen
import com.boom.feature.party.ui.HostPartyScreen
import com.boom.feature.party.ui.JoinPartyScreen
import com.boom.feature.party.ui.ParticipantDashboardScreen
import com.boom.feature.welcome.WelcomeScreen
import kotlinx.coroutines.flow.collectLatest

object BoomDestinations {
    const val AUTH_SIGNUP = "auth/signup"
    const val AUTH_VERIFY = "auth/verify"
    const val AUTH_SIGNIN = "auth/signin"
    const val AUTH_MFA = "auth/mfa"
    const val WELCOME = "welcome"
    const val DASHBOARD = "dashboard"
    const val HOST_PARTY = "party/host"
    const val JOIN_PARTY = "party/join"
    const val HOST_CONTROL = "party/host/control/{partyId}"
    const val PARTICIPANT_DASHBOARD = "party/participant/dashboard/{partyId}"
    const val UNIFIED_CHAT = "party/chat/{partyId}"
    const val ACTIVITIES = "party/activities/{partyId}"
    const val GUARDIAN_SECURITY = "security/centre/{partyId}"
}

@Composable
fun BoomNavHost(
    navController: NavHostController = rememberNavController(),
    apiClient: BoomApiClient? = null,
    startDestination: String? = null
) {
    val sessionStorage = try {
        BoomApplication.instance.sessionStorage
    } catch (e: Exception) {
        remember { InMemorySessionStorage() }
    }

    val guardian = try {
        BoomApplication.instance.guardian
    } catch (e: Exception) {
        remember { BoomGuardian() }
    }

    val transport = remember { LocalLoopbackTransport() }
    val audioSyncEngine = remember { AudioSyncEngine() }

    // Determine initial entry point: only authenticated sessions reach Welcome/Dashboard
    val computedStartDestination = startDestination ?: if (sessionStorage.getSession() != null) {
        BoomDestinations.WELCOME
    } else {
        BoomDestinations.AUTH_SIGNIN
    }

    val authViewModel = remember {
        val client = apiClient ?: object : BoomApiClient by (throw UnsupportedOperationException()) {}
        AuthViewModel(client, sessionStorage)
    }

    val dashboardViewModel = remember {
        DashboardViewModel(sessionStorage, guardian)
    }

    val partyViewModel = remember {
        val client = apiClient ?: object : BoomApiClient by (throw UnsupportedOperationException()) {}
        PartyViewModel(client, sessionStorage, transport, audioSyncEngine, guardian)
    }

    LaunchedEffect(authViewModel) {
        authViewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is AuthNavigationEvent.NavigateToVerifyContact -> {
                    navController.navigate(BoomDestinations.AUTH_VERIFY)
                }
                is AuthNavigationEvent.NavigateToSignIn -> {
                    navController.navigate(BoomDestinations.AUTH_SIGNIN) {
                        popUpTo(BoomDestinations.AUTH_SIGNUP) { inclusive = true }
                    }
                }
                is AuthNavigationEvent.NavigateToMfaChallenge -> {
                    navController.navigate(BoomDestinations.AUTH_MFA)
                }
                is AuthNavigationEvent.NavigateToWelcome -> {
                    navController.navigate(BoomDestinations.WELCOME) {
                        popUpTo(BoomDestinations.AUTH_SIGNIN) { inclusive = true }
                    }
                }
            }
        }
    }

    LaunchedEffect(dashboardViewModel) {
        dashboardViewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is DashboardNavigationEvent.NavigateToHostParty -> {
                    navController.navigate(BoomDestinations.HOST_PARTY)
                }
                is DashboardNavigationEvent.NavigateToJoinParty -> {
                    navController.navigate(BoomDestinations.JOIN_PARTY)
                }
                is DashboardNavigationEvent.NavigateToActiveParty -> {
                    navController.navigate("party/host/control/${event.partyId}")
                }
                is DashboardNavigationEvent.NavigateToSecurityCentre -> {
                    navController.navigate("security/centre/global")
                }
                is DashboardNavigationEvent.NavigateToSignIn -> {
                    navController.navigate(BoomDestinations.AUTH_SIGNIN) {
                        popUpTo(BoomDestinations.DASHBOARD) { inclusive = true }
                    }
                }
            }
        }
    }

    LaunchedEffect(partyViewModel) {
        partyViewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is PartyNavigationEvent.NavigateToHostControlPanel -> {
                    navController.navigate("party/host/control/${event.partyId}")
                }
                is PartyNavigationEvent.NavigateToParticipantDashboard -> {
                    navController.navigate("party/participant/dashboard/${event.partyId}")
                }
                is PartyNavigationEvent.NavigateToDashboard -> {
                    navController.navigate(BoomDestinations.DASHBOARD) {
                        popUpTo(BoomDestinations.HOST_PARTY) { inclusive = true }
                    }
                }
                is PartyNavigationEvent.OpenChat -> {
                    navController.navigate("party/chat/${event.partyId}")
                }
                is PartyNavigationEvent.OpenSecurityCentre -> {
                    navController.navigate("security/centre/${event.partyId}")
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = computedStartDestination
    ) {
        composable(BoomDestinations.AUTH_SIGNUP) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToSignIn = { navController.navigate(BoomDestinations.AUTH_SIGNIN) }
            )
        }

        composable(BoomDestinations.AUTH_VERIFY) {
            VerifyContactScreen(
                viewModel = authViewModel,
                onAllVerified = { navController.navigate(BoomDestinations.AUTH_SIGNIN) }
            )
        }

        composable(BoomDestinations.AUTH_SIGNIN) {
            SignInScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(BoomDestinations.AUTH_SIGNUP) }
            )
        }

        composable(BoomDestinations.AUTH_MFA) {
            MfaChallengeScreen(
                viewModel = authViewModel,
                onMfaSuccess = { navController.navigate(BoomDestinations.WELCOME) }
            )
        }

        // Animated Welcome Screen (UI-01, UI-02)
        composable(BoomDestinations.WELCOME) {
            WelcomeScreen(
                onNavigateToDashboard = {
                    navController.navigate(BoomDestinations.DASHBOARD) {
                        popUpTo(BoomDestinations.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        // Main Dashboard Screen (DASH-01)
        composable(BoomDestinations.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onNavigateToHost = { navController.navigate(BoomDestinations.HOST_PARTY) },
                onNavigateToJoin = { navController.navigate(BoomDestinations.JOIN_PARTY) },
                onNavigateToSecurity = { navController.navigate("security/centre/global") },
                onLogout = { dashboardViewModel.logout() }
            )
        }

        // Host Party Creation Flow (HOST-01, HOST-02)
        composable(BoomDestinations.HOST_PARTY) {
            HostPartyScreen(
                viewModel = partyViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Join Party Flow (JOIN-01, JOIN-02)
        composable(BoomDestinations.JOIN_PARTY) {
            JoinPartyScreen(
                viewModel = partyViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Host Control Panel (HOST-03, HOST-04, HOST-05, HOST-06)
        composable(BoomDestinations.HOST_CONTROL) { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
            HostControlPanelScreen(
                viewModel = partyViewModel,
                onOpenChat = { id -> navController.navigate("party/chat/$id") },
                onOpenActivities = { id -> navController.navigate("party/activities/$id") },
                onOpenSecurity = { id -> navController.navigate("security/centre/$id") },
                onEndParty = {
                    navController.navigate(BoomDestinations.DASHBOARD) {
                        popUpTo(BoomDestinations.DASHBOARD) { inclusive = false }
                    }
                }
            )
        }

        // Participant Dashboard (JOIN-03, JOIN-04, FR-06)
        composable(BoomDestinations.PARTICIPANT_DASHBOARD) { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
            ParticipantDashboardScreen(
                viewModel = partyViewModel,
                onOpenChat = { id -> navController.navigate("party/chat/$id") },
                onOpenActivities = { id -> navController.navigate("party/activities/$id") },
                onLeaveParty = {
                    navController.navigate(BoomDestinations.DASHBOARD) {
                        popUpTo(BoomDestinations.DASHBOARD) { inclusive = false }
                    }
                }
            )
        }

        // Unified Chat (CHAT-01 to CHAT-09)
        composable(BoomDestinations.UNIFIED_CHAT) { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
            val client = apiClient ?: object : BoomApiClient by (throw UnsupportedOperationException()) {}
            val partyState = partyViewModel.uiState.value
            val isHost = partyState.isHost
            val hostUserId = partyState.activeParty?.hostUserId ?: ""

            val chatViewModel = remember(partyId) {
                ChatViewModel(
                    apiClient = client,
                    sessionStorage = sessionStorage,
                    guardian = guardian,
                    partyId = partyId,
                    isHost = isHost,
                    hostUserId = hostUserId
                )
            }

            ChatScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Party Activities (ACT-01, ACT-02, FR-04, FR-10)
        composable(BoomDestinations.ACTIVITIES) { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
            val client = apiClient ?: object : BoomApiClient by (throw UnsupportedOperationException()) {}
            val partyState = partyViewModel.uiState.value
            val isHost = partyState.isHost

            val activitiesViewModel = remember(partyId) {
                ActivitiesViewModel(
                    apiClient = client,
                    sessionStorage = sessionStorage,
                    nearbyTransport = transport,
                    audioSyncEngine = audioSyncEngine,
                    partyId = partyId,
                    isHost = isHost
                )
            }

            ActivitiesHubScreen(
                viewModel = activitiesViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // BOOM Guardian Security Centre (SEC-01 to SEC-04)
        composable(BoomDestinations.GUARDIAN_SECURITY) { backStackEntry ->
            val partyId = backStackEntry.arguments?.getString("partyId") ?: "global"
            val securityViewModel = remember(partyId) {
                SecurityCentreViewModel(
                    guardian = guardian,
                    partyId = partyId
                )
            }

            SecurityCentreScreen(
                viewModel = securityViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}



