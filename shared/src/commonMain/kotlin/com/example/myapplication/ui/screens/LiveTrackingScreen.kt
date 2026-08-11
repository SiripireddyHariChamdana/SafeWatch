package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.getPlatform
import com.example.myapplication.util.LocationFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(onBack: () -> Unit) {
    val locationUpdate by LocationFlow.currentLocation.collectAsState(null)
    
    val lat = locationUpdate?.latitude ?: 0.0
    val lng = locationUpdate?.longitude ?: 0.0
    val acc = locationUpdate?.accuracy ?: 0f

    LaunchedEffect(Unit) {
        getPlatform().startLocationService()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Live GPS Tracking", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                getPlatform().MapView(
                    modifier = Modifier.fillMaxSize(),
                    latitude = lat,
                    longitude = lng,
                    accuracy = acc
                )
                
                // Status Overlay
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (lat != 0.0) "Signal: SECURE" else "Searching for GPS...",
                            color = if (lat != 0.0) NeonBlue else Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                        if (lat != 0.0) {
                            Text(
                                text = "${lat.toString().take(8)}, ${lng.toString().take(8)}",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
