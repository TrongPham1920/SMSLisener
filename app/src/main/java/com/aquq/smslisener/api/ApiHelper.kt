package com.aquq.smslisener.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object ApiHelper {
    private const val TAG = "ApiHelper"

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun sendSmsData(
        domain: String,
        bodyFormat: String,
        sender: String,
        message: String,
        receiver: String
    ) {
        Thread {
            try {
                val body = bodyFormat
                    .replace("{sender}", sender)
                    .replace("{message}", message)
                    .replace("{receiver}", receiver)

                Log.d(TAG, "Sending request to: $domain")
                Log.d(TAG, "Body: $body")

                val request = Request.Builder()
                    .url(domain)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()

                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response body: ${response.body?.string()}")

                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error sending SMS data to API", e)
            }
        }.start()
    }
}

