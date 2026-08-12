package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*
import com.example.myapplication.ui.auth.AuthViewModel
import com.example.myapplication.getPlatform
import com.example.myapplication.data.remote.SupabaseManager
import kotlinx.coroutines.launch
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel
) {
    val userProfile by viewModel.userProfile
    val loading by viewModel.loading
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableStateOf(false) }
    var triggerImagePicker by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            fullName = it.full_name ?: ""
            phone = it.phone ?: ""
            dob = it.dob ?: ""
            address = it.home_address ?: ""
            avatarUrl = it.avatar_url
        }
    }

    if (uploadProgress) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Uploading Photo") },
            text = { 
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonBlue)
                }
            },
            confirmButton = { }
        )
    }

    if (triggerImagePicker) {
        getPlatform().ImagePicker { bytes: ByteArray ->
            triggerImagePicker = false
            scope.launch {
                uploadProgress = true
                userProfile?.let { profile ->
                    val url = SupabaseManager.uploadAvatar(profile.id, bytes)
                    if (url != null) {
                        avatarUrl = url
                        viewModel.updateProfile(profile.copy(avatar_url = url)) {
                            uploadProgress = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Profile photo updated")
                            }
                        }
                    } else {
                        uploadProgress = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Failed to upload photo")
                        }
                    }
                } ?: run {
                    uploadProgress = false
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkNavy
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SecurityGridBackground()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Avatar Section
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { triggerImagePicker = true }
                            .then(if (avatarUrl == null) Modifier else Modifier.background(Color.Transparent)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrEmpty()) {
                            KamelImage(
                                resource = asyncPainterResource(data = avatarUrl!!),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = NeonBlue, modifier = Modifier.size(64.dp))
                        }
                    }
                    
                    FloatingActionButton(
                        onClick = { triggerImagePicker = true },
                        modifier = Modifier.size(40.dp),
                        containerColor = NeonBlue,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                ShadowGuardTextField(fullName, { fullName = it }, "Full Name", Icons.Default.Badge)
                Spacer(modifier = Modifier.height(16.dp))
                ShadowGuardTextField(phone, { phone = it }, "Phone Number", Icons.Default.Phone)
                Spacer(modifier = Modifier.height(16.dp))
                ShadowGuardTextField(dob, { dob = it }, "Date of Birth", Icons.Default.CalendarToday)
                Spacer(modifier = Modifier.height(16.dp))
                ShadowGuardTextField(address, { address = it }, "Home Address", Icons.Default.Home)

                Spacer(modifier = Modifier.height(48.dp))

                if (loading) {
                    CircularProgressIndicator(color = NeonBlue)
                } else {
                    GradientButton(
                        text = "Update Profile",
                        onClick = {
                            userProfile?.let {
                                val updatedProfile = it.copy(
                                    full_name = fullName,
                                    phone = phone,
                                    dob = dob,
                                    home_address = address,
                                    avatar_url = avatarUrl
                                )
                                viewModel.updateProfile(updatedProfile) {
                                    // Profile updated
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(onClick = { viewModel.logout(onLogout) }) {
                    Text("Logout Account", color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
