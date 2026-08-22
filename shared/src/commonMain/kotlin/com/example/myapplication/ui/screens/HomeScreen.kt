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
    onNavigateToHistory: () -> Unit,
    onNavigateToVoiceNotes: () -> Unit,
    onNavigateToLiveTracking: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToContacts: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val userProfile by viewModel.userProfile
    val locationUpdate by LocationFlow.currentLocation.collectAsState(null)

    // Handle Permissions Once on Home Screen Entry
    com.example.myapplication.getPlatform().PermissionManager {
        com.example.myapplication.getPlatform().startLocationService()
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SafeWatch",
                        color = NeonBlue,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "GPS: ${locationUpdate?.latitude?.let { "%.4f".format(it) } ?: "Searching..."}, ${locationUpdate?.longitude?.let { "%.4f".format(it) } ?: ""}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Surface(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(50.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Settings, null, tint = NeonBlue, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // TOP ACTIONS ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MainActionCard(
                    title = "Live GPS",
                    icon = Icons.Default.MyLocation,
                    color = NeonBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToLiveTracking
                )
                MainActionCard(
                    title = "SOS Alert",
                    icon = Icons.Default.Warning,
                    color = Color.Red,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSOS
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Core Features",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // FEATURE GRID
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FeatureGridCard("Emergency Contacts", Icons.Default.People, NeonPurple, Modifier.weight(1f)) { onNavigateToContacts() }
                FeatureGridCard("Location History", Icons.Default.History, Color.White.copy(alpha = 0.6f), Modifier.weight(1f)) { onNavigateToHistory() }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            FeatureGridCard("Voice Record", Icons.Default.Mic, NeonBlue, Modifier.fillMaxWidth()) { onNavigateToVoiceNotes() }

            Spacer(modifier = Modifier.height(120.dp))
        }

        // BOTTOM NAV PILL
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .width(300.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Home, 
                    null, 
                    tint = NeonBlue, 
                    modifier = Modifier.size(32.dp).clickable { }
                )
                Icon(
                    Icons.Default.Person, 
                    null, 
                    tint = Color.White.copy(alpha = 0.3f), 
                    modifier = Modifier.size(32.dp).clickable { onNavigateToProfile() }
                )
            }
        }
    }
}

@Composable
fun MainActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FeatureGridCard(title: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(130.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
