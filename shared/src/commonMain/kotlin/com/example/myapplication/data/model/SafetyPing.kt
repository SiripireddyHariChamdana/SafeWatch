package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SafetyPing(
    val id: String? = null,
    val user_id: String,
    val type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String,
    val color_hex: String,
    val created_at: String? = null
)
