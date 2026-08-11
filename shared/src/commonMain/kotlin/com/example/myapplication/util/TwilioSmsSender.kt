package com.example.myapplication.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object TwilioSmsSender {
    private val client = HttpClient()
    private const val SEND_ENDPOINT = "${Constants.BACKEND_URL}/api/sms/send"

    suspend fun sendSms(to: String, message: String): Boolean {
        return try {
            val response: HttpResponse = client.post(SEND_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("to", to)
                    put("message", message)
                }.toString())
            }

            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }
}
