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
 * Listens for requests from the Web SPA and sends native SMS alerts.
 */
object SmsRelaySync {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isSyncing = false
    private const val TAG = "SmsRelaySync"

    fun startSync(userId: String) {
        if (isSyncing || userId.isEmpty()) {
            Log.d(TAG, "Sync already active or invalid userId ($userId)")
            return
        }
        isSyncing = true
        
        Log.i(TAG, "📡 Initializing SMS Relay for User: $userId")
        
        val client = SupabaseManager.client
        // Use a unique channel name per user to avoid collisions
        val channel = client.realtime.channel("relay_v2_$userId")
        
        // Listen for NEW relay requests
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "sms_relay"
            // Note: Filter depends on Supabase Realtime configuration (must be enabled on table)
        }.onEach { action ->
            Log.d(TAG, "🔔 Realtime Event Received: ${action.javaClass.simpleName}")
            
            if (action is PostgresAction.Insert) {
                val data = action.record
                val reqUserId = data["user_id"]?.toString()?.trim()?.replace("\"", "")
                
                if (reqUserId == userId) {
                    val phone = data["phone"]?.toString()?.trim()?.replace("\"", "")
                    val message = data["message"]?.toString()?.trim()?.replace("\"", "")
                    
                    if (!phone.isNullOrEmpty() && !message.isNullOrEmpty()) {
                        Log.i(TAG, "📤 RELAY TRIGGERED: Sending SMS to $phone")
                        sendSmsInternal(phone, message)
                    }
                }
            }
        }.launchIn(scope)

        scope.launch {
            while (true) {
                try {
                    if (client.realtime.status.value != Realtime.Status.CONNECTED) {
                        Log.d(TAG, "Realtime status: ${client.realtime.status.value}. Connecting...")
                        client.realtime.connect()
                    }
                    channel.subscribe()
                    Log.d(TAG, "✅ SMS Relay Channel Subscribed")
                    break // Exit loop on success
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Connection Error: ${e.message}. Retrying in 5s...")
                    delay(5000)
                }
            }
        }
    }

    private fun sendSmsInternal(phone: String, message: String) {
        try {
            // Re-check context availability
            val context = androidContext
            if (context != null) {
                val platform = AndroidPlatform(context)
                platform.sendNativeSms(phone, message)
                Log.i(TAG, "✅ SMS Handed to OS")
            } else {
                Log.e(TAG, "❌ CRITICAL: androidContext is NULL in background")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ SMS Send Error: ${e.message}")
        }
    }
}
