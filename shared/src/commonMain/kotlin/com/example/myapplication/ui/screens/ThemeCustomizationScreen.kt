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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel
) {
    val selectedTheme by viewModel.selectedTheme

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Themes & Appearance", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text("Select Visual Protocol", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            ThemeOptionCard(
                "Futuristic Dark", 
                "Neon accents with deep navy background", 
                NeonBlue, 
                selectedTheme == UserTheme.FUTURISTIC_DARK
            ) { viewModel.updateTheme(UserTheme.FUTURISTIC_DARK) }
            
            ThemeOptionCard(
                "Stealth Black", 
                "True black background for AMOLED devices", 
                Color.White, 
                selectedTheme == UserTheme.STEALTH_BLACK
            ) { viewModel.updateTheme(UserTheme.STEALTH_BLACK) }
            
            ThemeOptionCard(
                "Midnight Purple", 
                "Vibrant purple gradients and shadows", 
                NeonPurple, 
                selectedTheme == UserTheme.MIDNIGHT_PURPLE
            ) { viewModel.updateTheme(UserTheme.MIDNIGHT_PURPLE) }
            
            ThemeOptionCard(
                "Emergency Red", 
                "High-contrast theme for visibility", 
                RedSOS, 
                selectedTheme == UserTheme.EMERGENCY_RED
            ) { viewModel.updateTheme(UserTheme.EMERGENCY_RED) }

            Spacer(modifier = Modifier.height(48.dp))

            // Preview Section
            Surface(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PREVIEW", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Icon(
                            imageVector = Icons.Default.Shield, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary, 
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("SafeWatch Active", color = Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ThemeOptionCard(
    title: String,
    description: String,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(12.dp).background(accentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(description, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
        }
    }
}
