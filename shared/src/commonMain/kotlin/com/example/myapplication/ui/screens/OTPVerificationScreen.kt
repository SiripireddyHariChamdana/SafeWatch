package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.auth.AuthViewModel

@Composable
fun OTPVerificationScreen(
    onSuccess: () -> Unit,
    onChangeContact: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val loading by viewModel.loading
    val error by viewModel.error

    OTPVerificationContent(
        onVerify = { otp ->
            viewModel.verifyRecoveryOTP(otp, onSuccess)
        },
        onChangeContact = onChangeContact,
        loading = loading,
        error = error
    )
}

@Composable
fun OTPVerificationContent(
    onVerify: (String) -> Unit, 
    onChangeContact: () -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    var timeLeft by remember { mutableStateOf(30) }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy)
    ) {
        SecurityGridBackground()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VerifiedUser, null, tint = NeonBlue, modifier = Modifier.size(56.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Verification Code", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("A 6-digit code has been sent to your email", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                otpValues.forEachIndexed { index, value ->
                    Box(modifier = Modifier.weight(1f)) {
                        OTPBoxItem(
                            value = value,
                            onValueChange = { 
                                if (it.length <= 1) {
                                    otpValues[index] = it
                                }
                            },
                            isSelected = value.isNotEmpty()
                        )
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (loading) {
                CircularProgressIndicator(color = NeonBlue)
            } else {
                GradientButton(
                    text = "Verify Code",
                    onClick = { 
                        val otp = otpValues.joinToString("")
                        if (otp.length == 6) onVerify(otp)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Resend code in ", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                Text(
                    text = "00:${timeLeft.toString().padStart(2, '0')}",
                    color = NeonPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (timeLeft == 0) {
                TextButton(onClick = { timeLeft = 30 }) {
                    Text("Resend OTP Now", color = NeonBlue, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Change Email",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
                modifier = Modifier.clickable { onChangeContact() }.padding(vertical = 24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPBoxItem(value: String, onValueChange: (String) -> Unit, isSelected: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        textStyle = TextStyle(
            color = Color.White, 
            fontSize = 20.sp, 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonBlue,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedContainerColor = Color.White.copy(alpha = 0.05f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            cursorColor = NeonBlue
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}
