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
import kotlinx.datetime.*
import kotlin.math.*

class LocationHistoryViewModel : ViewModel() {
    private val client = SupabaseManager.client

    private val _history = mutableStateOf<List<HistoryPoint>>(emptyList())
    val history: State<List<HistoryPoint>> = _history

    private val _loading = mutableStateOf(false)
    val loading: State<Boolean> = _loading

    private val _selectedDate = mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()))
    val selectedDate: State<LocalDate> = _selectedDate

    private val _distanceTravelled = mutableStateOf(0.0)
    val distanceTravelled: State<Double> = _distanceTravelled

    private val _duration = mutableStateOf("")
    val duration: State<String> = _duration

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        fetchHistory()
    }

    fun fetchHistory() {
        if (client == null) return
        val userId = client.auth.currentUserOrNull()?.id ?: return

        viewModelScope.launch {
            _loading.value = true
            try {
                val startOfDay = _selectedDate.value.atStartOfDayIn(TimeZone.currentSystemDefault()).toString()
                val endOfDay = _selectedDate.value.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault()).toString()

                val result = client.postgrest["location_history"]
                    .select(columns = Columns.ALL) {
                        filter { 
                            eq("user_id", userId)
                            gte("recorded_at", startOfDay)
                            lte("recorded_at", endOfDay)
                        }
                        order("recorded_at", Order.ASCENDING)
                    }
                    .decodeList<HistoryPoint>()
                
                _history.value = result
                calculateStats(result)
            } catch (e: Exception) {
                println("❌ History Fetch Error: ${e.message}")
                _history.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteHistory() {
        if (client == null) return
        val userId = client.auth.currentUserOrNull()?.id ?: return

        viewModelScope.launch {
            _loading.value = true
            try {
                val startOfDay = _selectedDate.value.atStartOfDayIn(TimeZone.currentSystemDefault()).toString()
                val endOfDay = _selectedDate.value.atTime(23, 59, 59).toInstant(TimeZone.currentSystemDefault()).toString()

                client.postgrest["location_history"].delete {
                    filter {
                        eq("user_id", userId)
                        gte("recorded_at", startOfDay)
                        lte("recorded_at", endOfDay)
                    }
                }
                _history.value = emptyList()
                _distanceTravelled.value = 0.0
                _duration.value = "Not available"
            } catch (e: Exception) {
                println("❌ History Delete Error: ${e.message}")
            } finally {
                _loading.value = false
            }
        }
    }

    private fun calculateStats(points: List<HistoryPoint>) {
        if (points.size < 2) {
            _distanceTravelled.value = 0.0
            _duration.value = "Not available"
            return
        }

        var totalDist = 0.0
        for (i in 0 until points.size - 1) {
            totalDist += haversine(
                points[i].latitude, points[i].longitude,
                points[i+1].latitude, points[i+1].longitude
            )
        }
        _distanceTravelled.value = totalDist

        val first = Instant.parse(points.first().recorded_at)
        val last = Instant.parse(points.last().recorded_at)
        val diff = last - first
        
        val hours = diff.inWholeHours
        val minutes = diff.inWholeMinutes % 60
        _duration.value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in kilometers
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val a = sin(dLat / 2).pow(2.0) + cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLon / 2).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return r * c
    }
}
