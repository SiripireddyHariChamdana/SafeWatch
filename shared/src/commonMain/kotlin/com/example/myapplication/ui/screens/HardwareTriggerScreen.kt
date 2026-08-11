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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HardwareTriggerScreen(onBack: () -> Unit) {
    var powerTriggerEnabled by remember { mutableStateOf(true) }
    var shakeTriggerEnabled by remember { mutableStateOf(true) }
    var shakeSensitivity by remember { mutableStateOf(0.7f) }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Hardware Triggers", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Silent Emergency Triggers",
                color = NeonPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Configure how to trigger SOS without opening the app",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            TriggerToggleCard(
                title = "Power Button SOS",
                description = "Press power button 5 times rapidly to trigger SOS",
                icon = Icons.Default.PowerSettingsNew,
                enabled = powerTriggerEnabled,
                onToggle = { powerTriggerEnabled = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TriggerToggleCard(
                title = "Shake to Alert",
                description = "Rapidly shake your device to trigger SOS",
                icon = Icons.Default.Vibration,
                enabled = shakeTriggerEnabled,
                onToggle = { shakeTriggerEnabled = it }
            )

            if (shakeTriggerEnabled) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Shake Sensitivity", color = Color.White, fontSize = 14.sp)
                Slider(
                    value = shakeSensitivity,
                    onValueChange = { shakeSensitivity = it },
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
            
            // Info Note
            Surface(
                color = NeonBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "These triggers work in the background even if your screen is locked.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TriggerToggleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(NeonBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(description, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonBlue,
                    checkedTrackColor = NeonBlue.copy(alpha = 0.3f)
                )
            )
        }
    }
}
