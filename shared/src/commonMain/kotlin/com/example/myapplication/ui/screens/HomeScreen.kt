package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.auth.AuthViewModel
import com.example.myapplication.util.LocationFlow
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.getPlatform

@Composable
fun HomeScreen(
    onNavigateToSOS: () -> Unit,
    onNavigateToTimer: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToVoiceNotes: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToLiveTracking: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val userProfile by viewModel.userProfile
    val locationUpdate by LocationFlow.currentLocation.collectAsState(null)

    // Handle Permissions Once on Home Screen Entry
    getPlatform().PermissionManager {
        println("📡 HomeScreen: Permissions granted, starting service...")
        getPlatform().startLocationService()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // User Info Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Hello, ${userProfile?.full_name?.split(" ")?.firstOrNull() ?: "User"}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Protection Active",
                        color = NeonBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, null, tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BIG SOS BUTTON
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = 0.1f))
                    .clickable { onNavigateToSOS() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(160.dp),
                    shape = CircleShape,
                    color = Color.Red,
                    shadowElevation = 20.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.WifiTethering, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("SOS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // FEATURE GRID
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeActionCard("Live GPS", Icons.Default.LocationOn, NeonBlue, Modifier.weight(1f)) { onNavigateToLiveTracking() }
                HomeActionCard("Safety Timer", Icons.Default.Timer, NeonPurple, Modifier.weight(1f)) { onNavigateToTimer() }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HomeActionCard("History", Icons.Default.History, Color.Gray, Modifier.weight(1f)) { onNavigateToHistory() }
                HomeActionCard("Voice Notes", Icons.Default.Mic, SuccessGreen, Modifier.weight(1f)) { onNavigateToVoiceNotes() }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HomeActionCard("Fake Call", Icons.Default.Call, Color.White, Modifier.fillMaxWidth()) { onNavigateToFakeCall() }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun HomeActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
