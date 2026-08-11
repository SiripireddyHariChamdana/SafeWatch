package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.getPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakeCallScreen(onBack: () -> Unit) {
    var scheduledTime by remember { mutableStateOf(10) } // Default 10 seconds
    var statusMessage by remember { mutableStateOf("Ready to schedule") }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Fake Call Setup", color = Color.White) },
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
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Call, null, tint = SuccessGreen, modifier = Modifier.size(56.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Simulate Incoming Call", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Helps you exit uncomfortable situations", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text("SCHEDULE TIMER", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallPresetItem("10s", 10, scheduledTime == 10) { scheduledTime = it }
                CallPresetItem("30s", 30, scheduledTime == 30) { scheduledTime = it }
                CallPresetItem("1m", 60, scheduledTime == 60) { scheduledTime = it }
                CallPresetItem("5m", 300, scheduledTime == 300) { scheduledTime = it }
            }

            Spacer(modifier = Modifier.height(48.dp))

            GradientButton(
                text = "Schedule Fake Call",
                onClick = {
                    getPlatform().scheduleFakeCall(scheduledTime.toLong())
                    statusMessage = "Call scheduled in $scheduledTime seconds"
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = statusMessage,
                color = if (statusMessage.contains("scheduled")) SuccessGreen else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CallPresetItem(label: String, value: Int, isSelected: Boolean, onSelect: (Int) -> Unit) {
    Surface(
        onClick = { onSelect(value) },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SuccessGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) SuccessGreen else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = label,
            color = if (isSelected) SuccessGreen else Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontWeight = FontWeight.Bold
        )
    }
}
