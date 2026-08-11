package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.example.myapplication.ui.theme.*
import com.example.myapplication.getPlatform
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.auth.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit, 
    onOTPSent: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val loading by viewModel.loading
    val error by viewModel.error

    // Ensure we have SMS permissions before trying to send native SMS
    getPlatform().PermissionManager { }

    ForgotPasswordContent(
        onBack = onBack,
        onSendOTP = { email ->
            viewModel.sendRecoveryOTP(email, onOTPSent)
        },
        loading = loading,
        error = error
    )
}

@Composable
fun ForgotPasswordContent(
    onBack: () -> Unit, 
    onSendOTP: (String) -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    var email by remember { mutableStateOf("") }

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

            // Icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Reset Password", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Enter your email to receive an OTP", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(48.dp))

            ShadowGuardTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email Address",
                icon = Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            if (error != null) {
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (loading) {
                CircularProgressIndicator(color = NeonBlue)
            } else {
                GradientButton(text = "Send Recovery OTP", onClick = { onSendOTP(email) })
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Back to Login",
                color = NeonBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 24.dp)
            )
        }
    }
}
