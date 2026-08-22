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
import com.example.myapplication.ui.battery.BatteryProtocolViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryProtocolScreen(
    onBack: () -> Unit,
    viewModel: BatteryProtocolViewModel = viewModel { BatteryProtocolViewModel() }
) {
    val isEnabled by viewModel.isEnabled
    val threshold by viewModel.threshold
    val loading by viewModel.loading

    LaunchedEffect(Unit) {
        viewModel.fetchSettings()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Last Breath Protocol", color = Color.White) },
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
                    modifier = Modifier.size(100.dp).background(WarningOrange.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BatteryChargingFull, null, tint = WarningOrange, modifier = Modifier.size(56.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Last Breath Sync", color = Color.White, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("Automatic SOS broadcast when battery is critical", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            } else {
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
                            Text("Enable Protocol", color = Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Triggers a final GPS push before shutdown", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.updateSettings(it, threshold) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WarningOrange,
                                checkedTrackColor = WarningOrange.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                if (isEnabled) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Trigger Threshold: ${threshold.toInt()}%", color = Color.White, fontSize = 14.sp)
                    Slider(
                        value = threshold,
                        onValueChange = { viewModel.updateSettings(isEnabled, it) },
                        valueRange = 1f..20f,
                        colors = SliderDefaults.colors(
                            thumbColor = WarningOrange,
                            activeTrackColor = WarningOrange,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1%", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                        Text("20%", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                color = WarningOrange.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How it works:",
                        color = WarningOrange,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "When your battery reaches the threshold, SafeWatch will perform a high-accuracy GPS sync and notify your emergency contacts that your phone is about to die.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
