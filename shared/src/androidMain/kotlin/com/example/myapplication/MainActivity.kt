package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {
    
    private var showFakeCallState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidContext = this
        
        handleIntent(intent)

        setContent {
            // Pass the state to the App composable or handle it here
            AppWithFakeCall(showFakeCallState)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra("SHOW_FAKE_CALL", false) == true) {
            showFakeCallState.value = true
        }
    }
}

@Composable
fun AppWithFakeCall(showFakeCall: MutableState<Boolean>) {
    if (showFakeCall.value) {
        com.example.myapplication.ui.screens.FakeCallIncomingScreen(onDismiss = {
            showFakeCall.value = false
        })
    } else {
        App()
    }
}
