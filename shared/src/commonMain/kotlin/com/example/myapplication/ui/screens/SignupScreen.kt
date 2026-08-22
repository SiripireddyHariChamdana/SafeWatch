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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.auth.AuthViewModel

@Composable
fun SignupScreen(
    onBack: () -> Unit,
    onSignupSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val loading by viewModel.loading
    val error by viewModel.error

    SignupScreenContent(
        onBack = onBack,
        onSignup = { fullName, email, phone, password ->
            // Direct signup as requested (no OTP for registration)
            // Order in AuthViewModel: email, password, fullName, phone, onSuccess
            viewModel.signUp(email, password, fullName, phone, onSignupSuccess)
        },
        loading = loading,
        error = error
    )
}

@Composable
fun SignupScreenContent(
    onBack: () -> Unit,
    onSignup: (String, String, String, String) -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, null, tint = NeonBlue, modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Create Account", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Join SafeWatch for maximum protection", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))

            Spacer(modifier = Modifier.height(32.dp))

            SafeWatchTextField(fullName, { fullName = it }, "Full Name", Icons.Default.Person)
            Spacer(modifier = Modifier.height(16.dp))
            SafeWatchTextField(email, { email = it }, "Email Address", Icons.Default.Email, keyboardType = KeyboardType.Email)
            Spacer(modifier = Modifier.height(16.dp))
            SafeWatchTextField(phone, { phone = it }, "Phone Number", Icons.Default.Phone, keyboardType = KeyboardType.Phone)
            Spacer(modifier = Modifier.height(16.dp))
            SafeWatchTextField(password, { password = it }, "Password", Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityToggle = { passwordVisible = !passwordVisible })
            Spacer(modifier = Modifier.height(16.dp))
            SafeWatchTextField(confirmPassword, { confirmPassword = it }, "Confirm Password", Icons.Default.Lock, isPassword = true, passwordVisible = passwordVisible, onVisibilityToggle = { passwordVisible = !passwordVisible })

            if (error != null) {
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // REGISTER BUTTON
            if (loading) {
                CircularProgressIndicator(color = NeonBlue)
            } else {
                GradientButton(
                    text = "Register for SafeWatch",
                    onClick = {
                        if (password == confirmPassword && email.isNotBlank() && fullName.isNotBlank() && phone.isNotBlank()) {
                            onSignup(fullName, email, phone, password)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already a member? ", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                Text("Login", color = NeonBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() } )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
