package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.SuccessGreen
import com.example.myapplication.getPlatform

@Composable
fun FakeCallIncomingScreen(onDismiss: () -> Unit) {
    var isCallActive by remember { mutableStateOf(false) }
    var callSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isCallActive) {
        if (isCallActive) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                callSeconds++
            }
        }
    }

    // When the screen is dismissed, stop the ringtone and vibration
    val handleDismiss = {
        getPlatform().stopFakeCallMedia()
        onDismiss()
    }

    val handleAnswer = {
        getPlatform().stopFakeCallMedia()
        isCallActive = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isCallActive) {
                // Caller Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Incoming Call",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
                
                Text(
                    text = "Home / Family",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(100.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Decline Button
                    LargeCallButton(
                        icon = Icons.Default.CallEnd,
                        color = Color.Red,
                        label = "Decline",
                        onClick = handleDismiss
                    )
                    
                    // Accept Button
                    LargeCallButton(
                        icon = Icons.Default.Call,
                        color = SuccessGreen,
                        label = "Accept",
                        onClick = handleAnswer
                    )
                }
            } else {
                // ACTIVE CALL UI
                Text(
                    text = "Active Call",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
                
                Text(
                    text = "Home / Family",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                val minutes = callSeconds / 60
                val remainingSeconds = callSeconds % 60
                Text(
                    text = "${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}",
                    color = SuccessGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(80.dp))

                // Simulated Call Controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControlItem(Icons.Default.MicOff, "Mute")
                    CallControlItem(Icons.Default.Dialpad, "Keypad")
                    CallControlItem(Icons.Default.VolumeUp, "Speaker")
                }

                Spacer(modifier = Modifier.height(80.dp))

                // End Call Button
                LargeCallButton(
                    icon = Icons.Default.CallEnd,
                    color = Color.Red,
                    label = "End Call",
                    onClick = handleDismiss
                )
            }
        }
    }
}

@Composable
fun CallControlItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.7f)) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun LargeCallButton(
    icon: ImageVector,
    color: Color,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
