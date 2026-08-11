package com.example.myapplication.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

import com.example.myapplication.data.SupabaseBackendManager

/**
 * Shared Supabase Client (Kotlin)
 * Used by UI and Auth components.
 * Realtime and Postgrest are shared between Kotlin and Java.
 */
object SupabaseManager {
    const val SUPABASE_URL = "https://mdszyqabsljbrjfcimss.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1kc3p5cWFic2xqYnJqZmNpbXNzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYwMDMwMzMsImV4cCI6MjEwMTU3OTAzM30.g_xrYF9H5xo3wYXBFKPwdnNdn4lNPCNkjeWbwZuKNBM"

    @OptIn(SupabaseInternal::class)
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
        
        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = 60000L
                connectTimeoutMillis = 60000L
                socketTimeoutMillis = 60000L
            }
            install(WebSockets)
        }
    }

    // Bridge to Java Backend Logic
    fun uploadEvidence(userId: String, fileName: String, bytes: ByteArray) {
        SupabaseBackendManager.uploadEvidence(userId, fileName, bytes)
    }

    fun getPlayableUrl(storagePath: String): String {
        return SupabaseBackendManager.getPlayableUrl(storagePath)
    }

    suspend fun uploadAvatar(userId: String, bytes: ByteArray): String? {
        val fileName = "avatar_$userId.jpg"
        return try {
            client.storage.from("avatars").upload(
                path = fileName,
                data = bytes,
            ) {
                upsert = true
            }
            val url = client.storage.from("avatars").publicUrl(fileName)
            url
        } catch (e: Exception) {
            println("❌ Avatar upload error: ${e.message}")
            null
        }
    }
}
