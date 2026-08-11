package com.example.myapplication.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Evidence(
    val id: String? = null,
    val user_id: String,
    val file_name: String,
    val storage_path: String,
    val created_at: String? = null
)
