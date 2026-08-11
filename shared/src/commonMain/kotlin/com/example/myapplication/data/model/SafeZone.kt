package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SafeZone(
    val id: String? = null,
    val user_id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val is_active: Boolean = true,
    val created_at: String? = null
)
