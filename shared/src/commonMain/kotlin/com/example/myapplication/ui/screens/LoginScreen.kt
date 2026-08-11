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
fun LoginScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: AuthViewModel = viewModel { AuthViewModel() }
) {
    val loading by viewModel.loading
    val error by viewModel.error

    LoginScreenContent(
        onBack = onBack,
        onLogin = { emailOrPhone, password ->
            viewModel.login(emailOrPhone, password) {
                // SUCCESS: App.kt will navigate automatically
                println("🚀 UI: Login Success Callback")
            }
        },
        onSignUp = onSignUp,
        onForgotPassword = onForgotPassword,
        loading = loading,
        error = error
    )
}

@Composable
fun LoginScreenContent(
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    loading: Boolean = false,
    error: String? = null
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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

            // Logo Section
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SafeWatch Login",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Login with Email or Phone",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Fields
            ShadowGuardTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                placeholder = "Email or Phone Number",
                icon = Icons.Default.Person,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            ShadowGuardTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                icon = Icons.Default.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onVisibilityToggle = { passwordVisible = !passwordVisible }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Forgot Password?",
                color = NeonBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End).clickable { onForgotPassword() }
            )

            if (error != null) {
                Surface(
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // LOGIN BUTTON
            if (loading) {
                CircularProgressIndicator(color = NeonBlue)
            } else {
                GradientButton(
                    text = "Login to SafeWatch",
                    onClick = {
                        if (emailOrPhone.isNotBlank() && password.isNotBlank()) {
                            onLogin(emailOrPhone, password)
                        }
                    }
                )
            }


            Spacer(modifier = Modifier.height(32.dp))

            // BOTTOM TEXT
            Row(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to SafeWatch? ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign Up",
                    color = NeonBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSignUp() }
                )
            }
        }
    }
}
