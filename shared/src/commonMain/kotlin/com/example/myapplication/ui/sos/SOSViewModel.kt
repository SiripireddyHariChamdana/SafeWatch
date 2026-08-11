package com.example.myapplication.ui.sos

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.SupabaseManager
import com.example.myapplication.getPlatform
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class SOSViewModel : ViewModel() {
    private val client = SupabaseManager.client

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _sosSent = mutableStateOf(false)
    val sosSent: State<Boolean> = _sosSent

    fun triggerManualSOS() {
        // Trigger local hardware/recording protocol
        getPlatform().triggerEmergencyProtocol()

        if (client == null) return
        val userId = client.auth.currentUserOrNull()?.id ?: return

        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                client.postgrest["sos_alerts"].insert(
                    mapOf(
                        "user_id" to userId,
                        "trigger_type" to "MANUAL",
                        "status" to "ACTIVE",
                        "created_at" to Clock.System.now().toString()
                    )
                )
                _sosSent.value = true
                startSOSMonitoring(userId)
            } catch (e: Exception) {
                _error.value = "Failed to broadcast SOS: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun startSOSMonitoring(userId: String) {
        val channel = client.realtime.channel("sos_monitor_$userId")
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "sos_alerts"
        }.onEach { action ->
            if (action is PostgresAction.Update) {
                val status = action.record["status"]?.toString()
                if (status == "RESOLVED" || status == "CANCELLED") {
                    println("🚨 SOS: Resolved remotely!")
                    _sosSent.value = false
                    // Here we might need a way to navigate back, but for now we update state
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            client.realtime.connect()
            channel.subscribe()
        }
    }
}
