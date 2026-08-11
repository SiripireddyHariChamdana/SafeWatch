package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserLocation(
    val user_id: String,
    val latitude: Double,
    val longitude: Double,
    val updated_at: String? = null
)
