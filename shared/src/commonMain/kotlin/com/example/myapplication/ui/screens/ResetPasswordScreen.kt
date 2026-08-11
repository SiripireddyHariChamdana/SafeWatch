package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
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
import com.example.myapplication.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.auth.AuthViewModel

@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val loading by viewModel.loading
    val error by viewModel.error

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
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Set New Password", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Secure your account with a new master key", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(48.dp))

            ShadowGuardTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = "New Password",
                icon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onVisibilityToggle = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ShadowGuardTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = "Confirm New Password",
                icon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onVisibilityToggle = { passwordVisible = !passwordVisible }
            )

            if (error != null) {
                Text(
                    text = error!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (loading) {
                CircularProgressIndicator(color = NeonBlue)
            } else {
                GradientButton(
                    text = "Update Master Password",
                    onClick = {
                        if (newPassword == confirmPassword && newPassword.isNotEmpty()) {
                            // Link to ViewModel reset logic
                            onResetSuccess()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Cancel Reset",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBack() }.padding(vertical = 24.dp)
            )
        }
    }
}
