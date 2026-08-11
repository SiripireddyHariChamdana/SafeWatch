package com.example.myapplication.ui.history

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.SupabaseManager
import com.example.myapplication.data.model.HistoryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class LocationHistoryViewModel : ViewModel() {
    private val client = SupabaseManager.client

    private val _history = mutableStateOf<List<HistoryPoint>>(emptyList())
    val history: State<List<HistoryPoint>> = _history

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    fun fetchHistory() {
        if (client == null) return
        val userId = client.auth.currentUserOrNull()?.id ?: return

        viewModelScope.launch {
            _loading.value = true
            try {
                val result = client.postgrest["location_history"]
                    .select(columns = Columns.ALL) {
                        filter { eq("user_id", userId) }
                        order("captured_at", Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<HistoryPoint>()
                
                _history.value = result
            } catch (e: Exception) {
                println("❌ History Fetch Error: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }
}
