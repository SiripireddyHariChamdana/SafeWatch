package com.example.myapplication.ui.timer

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

class SafetyTimerViewModel : ViewModel() {
    private val client = SupabaseManager.client

    private val _timeLeft = mutableStateOf(0L)
    val timeLeft: State<Long> = _timeLeft

    private val _isRunning = mutableStateOf(false)
    val isRunning: State<Boolean> = _isRunning

    private val _status = mutableStateOf("Ready")
    val status: State<String> = _status

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private var timerJob: Job? = null

    fun startTimer(durationSeconds: Long) {
        if (durationSeconds <= 0) {
            _status.value = "Error: Invalid duration"
            return
        }

        timerJob?.cancel()
        _timeLeft.value = durationSeconds
        _isRunning.value = true
        _status.value = "Active"

        timerJob = viewModelScope.launch {
            try {
                while (_timeLeft.value > 0) {
                    delay(1000)
                    _timeLeft.value -= 1
                }
                onTimerFinished()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _status.value = "Error: ${e.message}"
                    _isRunning.value = false
                }
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _isRunning.value = false
        _status.value = "Stopped (Safe)"
    }

    private suspend fun onTimerFinished() {
        _isRunning.value = false
        _status.value = "SOS TRIGGERED"
        triggerSOS()
    }

    private suspend fun triggerSOS() {
        if (client == null) return
        
        val userId = client.auth.currentUserOrNull()?.id ?: return
        
        try {
            client.postgrest["sos_alerts"].insert(
                mapOf(
                    "user_id" to userId,
                    "trigger_type" to "TIMER_EXPIRED",
                    "status" to "ACTIVE",
                    "created_at" to Clock.System.now().toString()
                )
            )
        } catch (e: Exception) {
            _status.value = "SOS Failed: ${e.message}"
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
