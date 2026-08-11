package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SOSAlert(
    val id: String? = null,
    val user_id: String,
    val latitude: Double,
    val longitude: Double,
    val status: String = "active",
    val created_at: String? = null
)
