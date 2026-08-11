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
fun PrivacySecurityScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Privacy & Security", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(32.dp))

            SecurityOptionCard("Data Encryption", "End-to-end AES-256 active", Icons.Default.Lock, true)
            SecurityOptionCard("Biometric Lock", "Require fingerprint for app entry", Icons.Default.Fingerprint, false)
            SecurityOptionCard("Privacy Mode", "Hide coordinates from public radar", Icons.Default.VisibilityOff, true)
            SecurityOptionCard("Auto-Erase", "Wipe logs after 30 days", Icons.Default.DeleteSweep, false)

            Spacer(modifier = Modifier.height(48.dp))

            // Policy Link
            TextButton(
                onClick = {},
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("View Full Privacy Protocol", color = NeonBlue, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SecurityOptionCard(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, initialValue: Boolean) {
    var enabled by remember { mutableStateOf(initialValue) }
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
                colors = SwitchDefaults.colors(checkedThumbColor = NeonBlue)
            )
        }
    }
}
