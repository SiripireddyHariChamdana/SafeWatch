package com.example.myapplication.util

import android.util.Log
import com.example.myapplication.AndroidPlatform
import com.example.myapplication.androidContext
import com.example.myapplication.data.remote.SupabaseManager
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Background SMS Relay Synchronizer (Production Grade)
 * Listens for requests from the Web Dashboard and triggers physical SMS alerts.
 */
object SmsRelaySync {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isSyncing = false
    private const val TAG = "SmsRelaySync"
    private var activeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    fun startSync(userId: String) {
        Log.i(TAG, "🔍 startSync called with ID: $userId")
        if (isSyncing || userId.isEmpty()) {
            Log.d(TAG, "Sync already active or invalid userId ($userId). Skipping init.")
            return
        }
        isSyncing = true
        
        Log.i(TAG, "📡 CRITICAL: Initializing SMS Relay Listener for User: $userId")
        
        val client = SupabaseManager.client
        
        // Use a persistent channel name
        val channel = client.realtime.channel("safewatch_relay_v3_$userId")
        activeChannel = channel
        
        // Listen for ALL changes to 'sms_relay' table
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "sms_relay"
        }.onEach { action ->
            Log.d(TAG, "🔔 REALTIME EVENT: ${action.javaClass.simpleName}")
            
            try {
                if (action is PostgresAction.Insert) {
                    val data = action.record
                    Log.i(TAG, "📦 NEW RELAY RECORD RECEIVED: $data")
                    
                    val rawUserId = data["user_id"]?.toString()?.trim()?.replace("\"", "")
                    val phone = data["phone"]?.toString()?.trim()?.replace("\"", "")
                    val message = data["message"]?.toString()?.trim()?.replace("\"", "")
                    
                    Log.d(TAG, "🔍 ID CHECK: Incoming[$rawUserId] vs Target[$userId]")
                    
                    // Case-insensitive matching and trimming to prevent UUID mismatch
                    if (rawUserId?.equals(userId, ignoreCase = true) == true) {
                        if (!phone.isNullOrEmpty() && !message.isNullOrEmpty()) {
                            Log.i(TAG, "🚀 MATCH FOUND! Triggering SMS to $phone")
                            sendSmsInternal(phone, message)
                        } else {
                            Log.w(TAG, "⚠️ Received relay record but phone or message is missing")
                        }
                    } else {
                        Log.d(TAG, "ℹ️ Ignored relay: Record is for another user ($rawUserId)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ FATAL: Error parsing relay event: ${e.message}")
            }
        }.launchIn(scope)

        // Persistent Connection Management
        scope.launch {
            while (true) {
                try {
                    val status = client.realtime.status.value
                    Log.d(TAG, "📡 Current Connection Status: $status")

                    if (status != Realtime.Status.CONNECTED) {
                        Log.i(TAG, "🔄 Realtime disconnected. Reconnecting...")
                        client.realtime.connect()
                    }
                    
                    channel.subscribe()
                    Log.i(TAG, "✅ SMS RELAY ACTIVE: Waiting for web signals...")
                    
                    // Stay in loop but wait longer between checks
                    delay(30000) 
                } catch (e: Exception) {
                    Log.e(TAG, "❌ CONNECTION ERROR: ${e.message}. Retrying in 10s...")
                    delay(10000)
                }
            }
        }
    }

    private fun sendSmsInternal(phone: String, message: String) {
        Log.i(TAG, "📠 sendSmsInternal: Target=$phone")
        try {
            val context = androidContext
            if (context != null) {
                Log.d(TAG, "📠 Handing to platform...")
                val platform = AndroidPlatform(context)
                platform.sendNativeSms(phone, message)
                Log.i(TAG, "✅ SUCCESS: SMS Handed to Android OS for transmission")
            } else {
                Log.e(TAG, "❌ SYSTEM ERROR: androidContext is NULL. Cannot send SMS.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ OS ERROR: Failed to dispatch SMS: ${e.message}")
        }
    }
}
