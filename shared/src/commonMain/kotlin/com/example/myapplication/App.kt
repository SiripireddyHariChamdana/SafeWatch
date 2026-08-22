package com.example.myapplication

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.screens.*
import com.example.myapplication.ui.theme.SafeWatchTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.auth.AuthViewModel

@Composable
fun App() {
    val authViewModel: AuthViewModel = viewModel { AuthViewModel() }
    val currentTheme by authViewModel.selectedTheme
    val isLoggedIn by authViewModel.isLoggedIn
    val isInitialLoaded by authViewModel.isInitialSessionLoaded

    SafeWatchTheme(userTheme = currentTheme) {
        val navController = rememberNavController()

        // Reactive Navigation: The "Brain" of the App
        LaunchedEffect(isLoggedIn, isInitialLoaded) {
            // Safety timeout: If session doesn't load in 5 seconds, assume not logged in
            kotlinx.coroutines.delay(5000)
            if (!isInitialLoaded) {
                println("⚠️ Nav: Initial session load timeout. Falling back.")
                // We don't force it here yet, let AuthViewModel do its job, but this is for debugging
            }
        }

        LaunchedEffect(isLoggedIn, isInitialLoaded) {
            if (!isInitialLoaded) return@LaunchedEffect

            val currentRoute = navController.currentBackStackEntry?.destination?.route
            println("🚀 Nav: isLoggedIn=$isLoggedIn, currentRoute=$currentRoute")
            
            if (isLoggedIn) {
                if (currentRoute == Screen.Login.route || 
                    currentRoute == Screen.Signup.route || 
                    currentRoute == Screen.Splash.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            } else {
                // Only navigate to login if we are not on a public screen
                if (currentRoute != null && 
                    currentRoute != Screen.Splash.route && 
                    currentRoute != Screen.Login.route &&
                    currentRoute != Screen.Signup.route &&
                    currentRoute != Screen.ForgotPassword.route &&
                    currentRoute != Screen.OTPVerification.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
            composable(Screen.Splash.route) {
                SplashScreen {
                    // After splash animation, if still on Splash, force a decision
                    if (navController.currentBackStackEntry?.destination?.route == Screen.Splash.route) {
                        if (isLoggedIn) {
                            navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                        } else {
                            navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                        }
                    }
                }
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen {
                    navController.navigate(Screen.Login.route)
                }
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onBack = { },
                    onLogin = { /* Handled by Auth State Change */ },
                    onSignUp = { navController.navigate(Screen.Signup.route) },
                    onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    viewModel = authViewModel
                )
            }
            composable(Screen.Signup.route) {
                SignupScreen(
                    onBack = { navController.popBackStack() },
                    onSignupSuccess = { /* Handled by Auth State Change */ },
                    viewModel = authViewModel
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onOTPSent = { navController.navigate(Screen.OTPVerification.route) },
                    viewModel = authViewModel
                )
            }
            composable(Screen.OTPVerification.route) {
                OTPVerificationScreen(
                    onSuccess = { navController.navigate(Screen.ResetPassword.route) },
                    onChangeContact = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }
            composable(Screen.ResetPassword.route) {
                ResetPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onResetSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    viewModel = authViewModel
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSOS = { navController.navigate(Screen.SOS.route) },
                    onNavigateToHistory = { navController.navigate(Screen.LocationHistory.route) },
                    onNavigateToVoiceNotes = { navController.navigate(Screen.VoiceNotes.route) },
                    onNavigateToLiveTracking = { navController.navigate(Screen.LiveTracking.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToContacts = { navController.navigate(Screen.EmergencyContacts.route) },
                    viewModel = authViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToContacts = { navController.navigate(Screen.EmergencyContacts.route) },
                    onNavigateToHardware = { navController.navigate(Screen.HardwareTrigger.route) },
                    onNavigateToLastBreath = { navController.navigate(Screen.BatteryProtocol.route) },
                    onNavigateToEvidenceVault = { navController.navigate(Screen.VoiceNotes.route) },
                    onNavigateToTheme = { navController.navigate(Screen.ThemeCustomization.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.PrivacySecurity.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable(Screen.SOS.route) { SOSScreen(onCancel = { navController.popBackStack() }) }
            composable(Screen.SafetyTimer.route) { SafetyTimerScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.LocationHistory.route) { LocationHistoryScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.VoiceNotes.route) { EmergencyVoiceNotesScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.LiveTracking.route) { LiveTrackingScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.EmergencyContacts.route) { EmergencyContactsScreen(onBack = { navController.popBackStack() }) }
            
            composable(Screen.Profile.route) { 
                ProfileScreen(
                    onLogout = { /* Handled by Auth State Change */ },
                    onBack = { navController.popBackStack() },
                    viewModel = authViewModel
                )
            }
            composable(Screen.HardwareTrigger.route) { HardwareTriggerScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.BatteryProtocol.route) { BatteryProtocolScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.ThemeCustomization.route) { 
                ThemeCustomizationScreen(onBack = { navController.popBackStack() }, viewModel = authViewModel) 
            }
            composable(Screen.PrivacySecurity.route) { PrivacySecurityScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
