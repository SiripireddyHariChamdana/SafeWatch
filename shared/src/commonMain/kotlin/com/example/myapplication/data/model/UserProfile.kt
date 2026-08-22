package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val full_name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val blood_group: String? = null,
    val home_address: String? = null,
    val dob: String? = null,
    val avatar_url: String? = null
)

@Serializable
data class HistoryPoint(
    val id: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val battery_level: Int? = null,
    val recorded_at: String
)
