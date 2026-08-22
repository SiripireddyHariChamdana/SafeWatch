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

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToHardware: () -> Unit,
    onNavigateToLastBreath: () -> Unit,
    onNavigateToEvidenceVault: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            SettingsActionCard("User Profile", "Manage your personal information", Icons.Default.Person) { onNavigateToProfile() }
            SettingsActionCard("Emergency Circle", "Manage trusted contacts", Icons.Default.Groups) { onNavigateToContacts() }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Safety Protocol", color = NeonPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            
            SettingsActionCard("Hardware Triggers", "Silent SOS via physical buttons", Icons.Default.PowerSettingsNew) { onNavigateToHardware() }
            SettingsActionCard("Last Breath Protocol", "Emergency broadcast on low battery", Icons.Default.BatteryChargingFull) { onNavigateToLastBreath() }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Data & Vault", color = NeonPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            
            SettingsActionCard("Evidence Vault", "Securely stored voice and logs", Icons.Default.Storage) { onNavigateToEvidenceVault() }
            SettingsActionCard("Privacy & Security", "Data encryption and app locking", Icons.Default.Security) { onNavigateToPrivacy() }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("System", color = NeonPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            
            SettingsActionCard("Themes & Appearance", "UI Customization", Icons.Default.Palette) { onNavigateToTheme() }
            
            Spacer(modifier = Modifier.height(40.dp))

            // App Version Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SafeWatch v1.5.0", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun SettingsActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.2f))
        }
    }
}
