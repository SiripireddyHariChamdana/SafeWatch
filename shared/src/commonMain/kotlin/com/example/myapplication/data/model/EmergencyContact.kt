package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EmergencyContact(
    val id: String? = null,
    val user_id: String,
    val contact_name: String,
    val contact_phone: String,
    val is_trusted: Boolean = true,
    val created_at: String? = null
)
