package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.timer.SafetyTimerViewModel

@Composable
fun SafetyTimerScreen(
    onBack: () -> Unit,
    viewModel: SafetyTimerViewModel = viewModel { SafetyTimerViewModel() },
) {
    val timeLeft by viewModel.timeLeft
    val isRunning by viewModel.isRunning
    val status by viewModel.status
    val loading by viewModel.loading

    var selectedDuration by remember { mutableLongStateOf(600L) } // Default 10 mins
    var manualMinutes by remember { mutableStateOf("10") }
    var manualSeconds by remember { mutableStateOf("00") }

    // Update selectedDuration when manual inputs change
    LaunchedEffect(manualMinutes, manualSeconds) {
        val mins = manualMinutes.toLongOrNull() ?: 0L
        val secs = manualSeconds.toLongOrNull() ?: 0L
        selectedDuration = (mins * 60) + secs
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkNavy)) {
        SecurityGridBackground()
        
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(56.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Safety Timer", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Auto SOS if timer reaches zero", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Timer Circle
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = if (isRunning && selectedDuration > 0) (timeLeft.toFloat() / selectedDuration.toFloat()) else 1f,
                    modifier = Modifier.size(240.dp),
                    color = if (isRunning && timeLeft < 60) Color.Red else NeonPurple,
                    strokeWidth = 8.dp,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val displayTimeSeconds = if (isRunning) timeLeft else selectedDuration
                    val minutes = displayTimeSeconds / 60
                    val seconds = displayTimeSeconds % 60
                    Text(
                        text = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
                        color = Color.White,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(status.uppercase(), color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (loading) {
                CircularProgressIndicator(color = NeonBlue)
            } else if (!isRunning) {
                // TIMER SETUP
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TimerPresetItem("1m", 60L, selectedDuration == 60L) { 
                        selectedDuration = it 
                        manualMinutes = "1"
                        manualSeconds = "00"
                    }
                    TimerPresetItem("5m", 300L, selectedDuration == 300L) { 
                        selectedDuration = it 
                        manualMinutes = "5"
                        manualSeconds = "00"
                    }
                    TimerPresetItem("10m", 600L, selectedDuration == 600L) {
                        selectedDuration = it 
                        manualMinutes = "10"
                        manualSeconds = "00"
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("CUSTOM DURATION", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ManualTimeInput(
                        value = manualMinutes,
                        onValueChange = { if(it.length <= 3 && it.all { c -> c.isDigit() }) manualMinutes = it },
                        label = "MIN"
                    )
                    Text(":", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    ManualTimeInput(
                        value = manualSeconds,
                        onValueChange = { if(it.length <= 2 && it.all { c -> c.isDigit() }) manualSeconds = it },
                        label = "SEC"
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
                GradientButton(
                    text = "Activate Timer",
                    onClick = {
                        viewModel.startTimer(selectedDuration)
                    }
                )
            } else {
                // ACTIVE STATE
                Button(
                    onClick = { viewModel.stopTimer() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text("I AM SAFE (STOP)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ManualTimeInput(value: String, onValueChange: (String) -> Unit, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(80.dp),
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center, 
                color = Color.White, 
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedBorderColor = NeonBlue,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
            ),
            singleLine = true
        )
        Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
    }
}

@Composable
fun TimerPresetItem(label: String, seconds: Long, isSelected: Boolean, onSelect: (Long) -> Unit) {
    Surface(
        onClick = { onSelect(seconds) },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NeonBlue.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) NeonBlue else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = label,
            color = if (isSelected) NeonBlue else Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            fontWeight = FontWeight.Bold
        )
    }
}
