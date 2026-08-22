package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.history.LocationHistoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.HistoryPoint
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationHistoryScreen(
    onBack: () -> Unit,
    viewModel: LocationHistoryViewModel = viewModel { LocationHistoryViewModel() }
) {
    val history by viewModel.history
    val loading by viewModel.loading
    val selectedDate by viewModel.selectedDate
    val distance by viewModel.distanceTravelled

    LaunchedEffect(Unit) {
        viewModel.fetchHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location History", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkNavy)
            )
        },
        containerColor = DarkNavy
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Simple Date Selector
            DateSelectorRow(
                selectedDate = selectedDate,
                onDateChange = { viewModel.setSelectedDate(it) }
            )

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else if (history.isEmpty()) {
                EmptyHistoryState()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Minimal Map View
                    Box(modifier = Modifier.height(350.dp).fillMaxWidth().padding(16.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp),
                            color = CardNavy,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            com.example.myapplication.getPlatform().HistoryMapView(
                                modifier = Modifier.fillMaxSize(),
                                points = history
                            )
                        }
                    }

                    // Simple Stats Card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Travel Summary", color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            MinimalStatRow("Distance Travelled", if (distance < 1.0) "${(distance * 1000).toInt()} m" else "%.2f km".format(distance))
                            Spacer(modifier = Modifier.height(16.dp))
                            MinimalStatRow("Travel Time", "${formatTime(history.first().recorded_at)} - ${formatTime(history.last().recorded_at)}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalStatRow(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DateSelectorRow(selectedDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateChange(selectedDate.minus(1, DateTimeUnit.DAY)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
        }
        
        Text(
            text = "${selectedDate.dayOfMonth} ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.year}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black
        )

        IconButton(
            onClick = { onDateChange(selectedDate.plus(1, DateTimeUnit.DAY)) },
            enabled = selectedDate < today
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                null, 
                tint = if (selectedDate < today) Color.White else Color.Gray
            )
        }
    }
}

@Composable
fun EmptyHistoryState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📍", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Location History", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "No travel recorded for this date.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

private fun formatTime(iso: String): String {
    return try {
        // Handle various ISO formats and potential space separators from Supabase
        val sanitized = iso.replace(" ", "T")
        // If there's an offset like +05:30 but no T, it might already be handled by the replace.
        // If it's missing 'Z' or offset, it's not a valid Instant.
        // Supabase TIMESTAMPTZ always has offset.
        val instant = Instant.parse(sanitized)
        val lt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = if (lt.hour % 12 == 0) 12 else lt.hour % 12
        val ampm = if (lt.hour < 12) "AM" else "PM"
        "${hour}:${lt.minute.toString().padStart(2, '0')} $ampm"
    } catch (e: Exception) {
        // Fallback for non-standard strings
        if (iso.length >= 16) {
            val timePart = iso.split(" ").getOrNull(1)?.take(5)
            if (timePart != null) return timePart
            iso.substring(11, 16)
        } else {
            iso
        }
    }
}
