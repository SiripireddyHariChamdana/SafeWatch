package com.example.myapplication.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object OTPVerification : Screen("otp_verification")
    object ResetPassword : Screen("reset_password")
    object Home : Screen("home")
    object SOS : Screen("sos")
    object SafetyTimer : Screen("safety_timer")
    object EmergencyContacts : Screen("emergency_contacts")
    object LocationHistory : Screen("location_history")
    object Profile : Screen("profile")
    object VoiceNotes : Screen("voice_notes")
    object LiveTracking : Screen("live_tracking")
    object Settings : Screen("settings")
    object ThemeCustomization : Screen("theme_customization")
    object PrivacySecurity : Screen("privacy_security")
    
    // Advanced Engineering Features
    object BatteryProtocol : Screen("battery_protocol")
    object SafeZoneManager : Screen("safe_zone_manager")
    object GuardianTerminal : Screen("guardian_terminal")
    object HardwareTrigger : Screen("hardware_trigger")
    object SafetyHeatmap : Screen("safety_heatmap")
}
