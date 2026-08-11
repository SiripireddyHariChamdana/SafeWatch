package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.history.LocationHistoryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.getPlatform
import com.example.myapplication.data.model.HistoryPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationHistoryScreen(
    onBack: () -> Unit,
    viewModel: LocationHistoryViewModel = viewModel { LocationHistoryViewModel() }
) {
    val history by viewModel.history
    val loading by viewModel.loading
    
    var selectedPoint by remember { mutableStateOf<HistoryPoint?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchHistory()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Location History", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (selectedPoint != null) {
                // Mini Map View for selected point
                Box(modifier = Modifier.height(250.dp).fillMaxWidth()) {
                    getPlatform().MapView(
                        modifier = Modifier.fillMaxSize(),
                        latitude = selectedPoint!!.latitude,
                        longitude = selectedPoint!!.longitude,
                        accuracy = 0f
                    )
                    IconButton(
                        onClick = { selectedPoint = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }

            if (loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No location logs found.", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { point ->
                        HistoryCard(
                            point = point,
                            isSelected = selectedPoint == point,
                            onClick = { selectedPoint = point }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    point: HistoryPoint,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) NeonBlue.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) NeonBlue else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSelected) NeonBlue else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Logged Position", color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(text = "Time: ${point.captured_at.take(16).replace("T", " ")}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Text(text = "${point.latitude}, ${point.longitude}", color = NeonBlue.copy(alpha = 0.5f), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.2f))
        }
    }
}
