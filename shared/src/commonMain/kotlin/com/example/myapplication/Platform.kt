package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface Platform {
    val name: String
    fun startLocationService()
    fun stopLocationService()
    
    fun observeLocation(onUpdate: (Double, Double, Float) -> Unit)
    
    @Composable
    fun MapView(modifier: Modifier, latitude: Double, longitude: Double, accuracy: Float)

    @Composable
    fun HistoryMapView(modifier: Modifier, points: List<com.example.myapplication.data.model.HistoryPoint>)

    fun triggerEmergencyProtocol()

    fun sendNativeSms(phoneNumber: String, message: String)
    fun sendEmail(to: String, subject: String, body: String)

    @Composable
    fun PermissionManager(onAllGranted: () -> Unit)

    fun playAudio(url: String)
    fun toggleAudio(url: String): Boolean
    fun stopAudio()

    fun startManualRecording(): String?
    fun stopManualRecording(): String?
    fun readFileBytes(path: String): ByteArray?

    @Composable
    fun ImagePicker(onImagePicked: (ByteArray) -> Unit)

    fun persistUserId(userId: String)
    fun persistSetting(key: String, value: String)
    fun getPersistedSetting(key: String, defaultValue: String): String
    fun refreshSafetyServiceSettings()
}

expect fun getPlatform(): Platform
