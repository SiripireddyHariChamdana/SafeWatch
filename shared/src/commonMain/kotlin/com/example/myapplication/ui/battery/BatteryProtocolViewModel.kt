package com.example.myapplication.ui.battery

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.SupabaseManager
import com.example.myapplication.getPlatform
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

class BatteryProtocolViewModel : ViewModel() {
    private val client = SupabaseManager.client

    private val _isEnabled = mutableStateOf(true)
    val isEnabled: State<Boolean> = _isEnabled

    private val _threshold = mutableStateOf(5f)
    val threshold: State<Float> = _threshold

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    fun fetchSettings() {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = client.postgrest["user_settings"]
                    .select(columns = Columns.ALL) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeSingleOrNull<Map<String, String>>()
                
                result?.let {
                    _isEnabled.value = it["last_breath_enabled"]?.toBoolean() ?: true
                    _threshold.value = it["last_breath_threshold"]?.toFloatOrNull() ?: 5f
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateSettings(enabled: Boolean, threshold: Float) {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        _isEnabled.value = enabled
        _threshold.value = threshold

        viewModelScope.launch {
            try {
                client.postgrest["user_settings"].upsert(
                    mapOf(
                        "user_id" to userId,
                        "last_breath_enabled" to enabled,
                        "last_breath_threshold" to threshold.toInt()
                    )
                )
                // Notify the platform service to refresh settings
                getPlatform().refreshSafetyServiceSettings()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
