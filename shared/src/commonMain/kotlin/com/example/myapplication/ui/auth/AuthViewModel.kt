package com.example.myapplication.ui.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.getPlatform
import com.example.myapplication.data.model.UserProfile
import com.example.myapplication.data.remote.SupabaseManager
import com.example.myapplication.ui.theme.UserTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel : ViewModel() {
    private val client = SupabaseManager.client

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _isLoggedIn = mutableStateOf(false)
    val isLoggedIn: State<Boolean> = _isLoggedIn

    private val _isInitialSessionLoaded = mutableStateOf(false)
    val isInitialSessionLoaded: State<Boolean> = _isInitialSessionLoaded

    private val _userProfile = mutableStateOf<UserProfile?>(null)
    val userProfile: State<UserProfile?> = _userProfile
    
    private val _selectedTheme = mutableStateOf(UserTheme.FUTURISTIC_DARK)
    val selectedTheme: State<UserTheme> = _selectedTheme

    private val _verificationOTP = mutableStateOf<String?>(null)
    val verificationOTP: State<String?> = _verificationOTP

    private var currentSyncUserId: String? = null

    init {
        observeAuthStatus()
    }

    private fun observeAuthStatus() {
        // Start a fallback timer to ensure the app doesn't hang on splash
        viewModelScope.launch {
            kotlinx.coroutines.delay(8000)
            if (!_isInitialSessionLoaded.value) {
                println("⚠️ Auth: Session Load Timeout - Forcing UI Ready")
                _isInitialSessionLoaded.value = true
            }
        }

        client.auth.sessionStatus.onEach { status ->
            println("🔑 Auth: Status Update: $status")
            when (status) {
                is SessionStatus.Authenticated -> {
                    val userId = status.session.user?.id ?: ""
                    if (userId.isNotEmpty()) {
                        println("🔑 Auth: Confirmed Logged In ($userId)")
                        
                        getPlatform().persistUserId(userId)
                        currentSyncUserId = userId
                        fetchUserProfile(userId)
                        fetchUserSettings(userId)
                        startRealtimeSync(userId)
                        _isLoggedIn.value = true
                        _isInitialSessionLoaded.value = true
                    }
                }
                is SessionStatus.NotAuthenticated -> {
                    println("🔑 Auth: Confirmed Logged Out")
                    currentSyncUserId = null
                    _isLoggedIn.value = false
                    _userProfile.value = null
                    _isInitialSessionLoaded.value = true
                }
                else -> {}
            }
        }.launchIn(viewModelScope)
    }

    private fun checkSession() {
        // Obsolete: Replaced by observeAuthStatus()
    }

    private fun startRealtimeSync(userId: String) {
        if (userId.isEmpty()) return
        
        // Listen for changes in 'users' table
        val profileChannel = client.realtime.channel("profile_sync_$userId")
        profileChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "users"
        }.onEach {
            println("🔄 Realtime: User profile change detected")
            fetchUserProfile(userId)
        }.launchIn(viewModelScope)

        // Listen for changes in 'user_settings' table
        val settingsChannel = client.realtime.channel("settings_sync_$userId")
        settingsChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "user_settings"
        }.onEach {
            println("🔄 Realtime: Settings change detected")
            fetchUserSettings(userId)
        }.launchIn(viewModelScope)

        // Listen for SMS Relay requests (Web Gateway)
        val relayChannel = client.realtime.channel("sms_relay_sync_$userId")
        relayChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "sms_relay"
        }.onEach { action ->
            if (action is PostgresAction.Insert) {
                val reqUserId = action.record["user_id"]?.toString()?.replace("\"", "")
                if (reqUserId == userId) {
                    val phone = action.record["phone"]?.toString()?.replace("\"", "")
                    val message = action.record["message"]?.toString()?.replace("\"", "")
                    if (phone != null && message != null) {
                        println("📡 Realtime: SMS Relay Triggered for $phone")
                        getPlatform().sendNativeSms(phone, message)
                    }
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                client.realtime.connect()
                profileChannel.subscribe()
                settingsChannel.subscribe()
                relayChannel.subscribe()
            } catch (e: Exception) {
                println("❌ Realtime Connection Error: ${e.message}")
            }
        }
    }

    private fun fetchUserProfile(userId: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    client.postgrest["users"]
                        .select(columns = Columns.ALL) {
                            filter { eq("id", userId) }
                        }
                        .decodeSingleOrNull<UserProfile>()
                }
                _userProfile.value = profile
            } catch (e: Exception) {
                _error.value = "Profile error: ${e.message}"
            }
        }
    }

    private fun fetchUserSettings(userId: String) {
        viewModelScope.launch {
            try {
                val settings = withContext(Dispatchers.IO) {
                    client.postgrest["user_settings"]
                        .select(columns = Columns.ALL) {
                            filter { eq("user_id", userId) }
                        }
                        .decodeSingleOrNull<Map<String, String>>()
                }
                settings?.let {
                    val themeStr = it["theme"] ?: "FUTURISTIC_DARK"
                    _selectedTheme.value = UserTheme.valueOf(themeStr)
                }
            } catch (e: Exception) {}
        }
    }

    fun updateTheme(theme: UserTheme) {
        _selectedTheme.value = theme
        val userId = client.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                client.postgrest["user_settings"].upsert(
                    mapOf(
                        "user_id" to userId,
                        "theme" to theme.name
                    )
                )
            } catch (e: Exception) {}
        }
    }

    fun updateProfile(profile: UserProfile, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                println("🚀 Auth: Saving profile for ${profile.id}")
                val data = mapOf(
                    "id" to profile.id,
                    "full_name" to profile.full_name,
                    "email" to profile.email,
                    "phone" to profile.phone,
                    "dob" to profile.dob,
                    "home_address" to profile.home_address,
                    "blood_group" to profile.blood_group,
                    "avatar_url" to profile.avatar_url
                )
                
                withContext(Dispatchers.IO) {
                    client.postgrest["users"].upsert(data)
                }
                println("✅ Auth: Profile saved successfully")
                _userProfile.value = profile
                println("✅ Auth: Local Profile Updated")
                onSuccess()
            } catch (e: Exception) {
                println("❌ Auth: Save Error: ${e.message}")
                _error.value = "Update failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun login(emailOrPhone: String, password: String, onSuccess: () -> Unit) {
        val cleanEmailOrPhone = emailOrPhone.trim()
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val isEmail = cleanEmailOrPhone.contains("@")
                withContext(Dispatchers.IO) {
                    if (isEmail) {
                        client.auth.signInWith(Email) {
                            this.email = cleanEmailOrPhone
                            this.password = password
                        }
                    } else {
                        val cleanPhone = if (cleanEmailOrPhone.startsWith("+")) cleanEmailOrPhone else "+$cleanEmailOrPhone"
                        client.auth.signInWith(Phone) {
                            this.phone = cleanPhone
                            this.password = password
                        }
                    }
                }
                println("🔑 Login Triggered Successfully")
                onSuccess()
            } catch (e: Exception) {
                println("❌ Login Error: ${e.message}")
                _error.value = e.message ?: "Login failed"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Direct Signup logic as requested.
     * Automatically logs in and sends a "Welcome" success email.
     */
    fun signUp(
        email: String, 
        password: String, 
        fullName: String,
        phone: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                // 1. Create the Auth account
                withContext(Dispatchers.IO) {
                    println("🚀 Auth: Attempting signUp for ${email.trim()}")
                    try {
                        client.auth.signUpWith(Email) {
                            this.email = email.trim()
                            this.password = password
                        }
                        println("✅ Auth: signUpWith successful")
                    } catch (e: Exception) {
                        println("ℹ️ Auth: signUpWith failed or user exists: ${e.message}")
                        // Continue to signIn in case user was already created but session is missing
                    }

                    // Tiny delay to avoid race conditions
                    kotlinx.coroutines.delay(500)

                    println("🚀 Auth: Attempting signIn for ${email.trim()}")
                    try {
                        client.auth.signInWith(Email) {
                            this.email = email.trim()
                            this.password = password
                        }
                        println("✅ Auth: signInWith successful")
                    } catch (e: Exception) {
                        println("❌ Auth: signInWith FAILED: ${e.message}")
                        throw e
                    }
                }
                
                val user = client.auth.currentUserOrNull()
                val userId = user?.id

                if (userId != null) {
                    val profile = UserProfile(
                        id = userId,
                        full_name = fullName,
                        email = email,
                        phone = phone
                    )
                    
                    val profileData = mapOf(
                        "id" to userId,
                        "full_name" to fullName,
                        "email" to email,
                        "phone" to phone
                    )
                    
                    _userProfile.value = profile
                    // Don't set _isLoggedIn here. The observer handles it.
                    onSuccess()

                    // Background tasks - move AFTER onSuccess to speed up UI
                    viewModelScope.launch(Dispatchers.IO) {
                        println("🚀 Auth: Running background creation tasks...")
                        try {
                            client.postgrest["users"].upsert(profileData)
                            println("✅ Auth: Users table synced")
                        } catch (e: Exception) { println("❌ Users table sync fail: ${e.message}") }

                        try {
                            client.postgrest["user_settings"].upsert(
                                mapOf("user_id" to userId, "theme" to UserTheme.FUTURISTIC_DARK.name)
                            )
                            println("✅ Auth: User settings synced")
                        } catch (e: Exception) { println("❌ User settings sync fail: ${e.message}") }
                        
                        // Email is slow, keep it in background
                        getPlatform().sendEmail(
                            email.trim(),
                            "Welcome to SafeWatch",
                            "Registration Successful: Hello $fullName, you have been successfully registered to SafeWatch."
                        )
                    }
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                println("❌ Signup error: $msg")
                if (msg.contains("Email not confirmed", ignoreCase = true)) {
                    _error.value = "Signup error: Please disable 'Confirm Email' in Supabase Auth settings."
                } else if (msg.contains("Invalid login credentials", ignoreCase = true)) {
                    _error.value = "Signup error: This account already exists with a different password."
                } else {
                    _error.value = "Signup error: $msg"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Recovery flow: Generates and sends a real verification code (OTP) via Email (Java Backend Logic on Android).
     */
    fun sendRecoveryOTP(email: String, onOTPSent: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val otp = (100000..999999).random().toString()
                _verificationOTP.value = otp
                
                // Calling Platform abstraction which calls Java on Android
                getPlatform().sendEmail(
                    email,
                    "SafeWatch Recovery Code",
                    "Your verification code is: $otp"
                )
                
                onOTPSent()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun verifyRecoveryOTP(otp: String, onSuccess: () -> Unit) {
        if (otp == _verificationOTP.value) {
            onSuccess()
        } else {
            _error.value = "Invalid verification code"
        }
    }

    fun updatePassword(newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                withContext(Dispatchers.IO) { client.auth.updateUser { password = newPassword } }
                _verificationOTP.value = null
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                client.auth.signOut()
            } catch (e: Exception) {}
            _isLoggedIn.value = false
            _userProfile.value = null
            onSuccess()
        }
    }
}
