package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShakeDetectionScreen(onBack: () -> Unit) {
    var sensitivity by remember { mutableFloatStateOf(0.7f) }
    var enabled by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Shake Detection", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(100.dp).background(NeonBlue.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Vibration, null, tint = NeonBlue, modifier = Modifier.size(56.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Shake to SOS", color = Color.White, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("Trigger emergency protocol via motion", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Feature", color = Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Active in background", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonBlue,
                            checkedTrackColor = NeonBlue.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Sensitivity", color = Color.White, fontSize = 14.sp)
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    colors = SliderDefaults.colors(
                        thumbColor = NeonBlue,
                        activeTrackColor = NeonBlue,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Low", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                    Text("High", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                color = NeonBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
            ) {
                Text(
                    "Note: Avoid setting sensitivity too high to prevent accidental triggers during normal use.",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
