package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionRequest(
    val id: String? = null,
    val requester_id: String,
    val receiver_email: String,
    val status: String = "pending",
    val created_at: String? = null
)
