package com.example.myapplication.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.sos.SOSViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.getPlatform

@Composable
fun SOSScreen(onCancel: () -> Unit, viewModel: SOSViewModel = viewModel { SOSViewModel() }) {
    val loading by viewModel.loading
    val error by viewModel.error
    val sosSent by viewModel.sosSent

    val infiniteTransition = rememberInfiniteTransition(label = "SOS")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse),
        label = "Pulse"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF200000))) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("EMERGENCY SOS", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Text("Broadcasting your live position...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Automatic Voice Recording Indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Red.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val recordingAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(tween(500), repeatMode = RepeatMode.Reverse),
                        label = "RecAlpha"
                    )
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).alpha(recordingAlpha).background(Color.Red))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AUTOMATIC EVIDENCE RECORDING ACTIVE", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (error != null) {
                Text(text = error!!, color = Color.Yellow, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
            if (sosSent) {
                Text(text = "Emergency Alert Sent!", color = Color.Green, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Pulsing SOS Button
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(240.dp).scale(scale).background(Color.Red.copy(alpha = 0.15f), CircleShape))
                Surface(
                    onClick = {
                        // Triggers permissions (on Android) and starts the SOS protocol
                        viewModel.triggerManualSOS()
                    },
                    modifier = Modifier.size(160.dp),
                    shape = CircleShape,
                    color = Color.Red,
                    shadowElevation = 20.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (loading) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text("SOS", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text("Deactivate SOS", color = Color.White)
            }
        }
    }
}
