package com.example.myapplication.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocationUpdate(val latitude: Double, val longitude: Double, val accuracy: Float)

object LocationFlow {
    private val _currentLocation = MutableStateFlow<LocationUpdate?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    fun updateLocation(lat: Double, lng: Double, acc: Float) {
        _currentLocation.value = LocationUpdate(lat, lng, acc)
    }
}

fun formatIsoNow(): String = kotlinx.datetime.Clock.System.now().toString()
