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
fun ProfileSetupScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text("Profile Setup", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(32.dp))

            SafeWatchTextField(name, { name = it }, "Full Name", Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
            SafeWatchTextField(phone, { phone = it }, "Phone Number", Icons.Default.Phone)
            Spacer(modifier = Modifier.height(16.dp))
            SafeWatchTextField(bloodGroup, { bloodGroup = it }, "Blood Group", Icons.Default.Bloodtype)
            Spacer(modifier = Modifier.height(16.dp))
            SafeWatchTextField(address, { address = it }, "Emergency Address", Icons.Default.Home)

            Spacer(modifier = Modifier.height(48.dp))

            GradientButton(text = "Save Profile", onClick = onBack)
        }
    }
}
