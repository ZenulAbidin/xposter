package com.zenulabidin.xposter.scheduler.services

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zenulabidin.xposter.scheduler.models.RepliesResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

class GCSService(private val context: Context) {

    companion object {
        private const val TAG = "GCSService"
        private const val BUCKET_NAME = "your-bucket-name"
        private const val PROJECT_ID = "your-project-id"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var accessToken: String? = null
    private var tokenExpiry: Long = 0

    fun fetchReplies(currentETag: String?): RepliesResponse? {
        return try {
            val token = getAccessToken() ?: run {
                Log.e(TAG, "Failed to get access token")
                return null
            }

            // Check for processing lock to avoid race conditions
            if (!checkAndClearStaleProcessingLock(token)) {
                Log.d(TAG, "Another instance is processing, skipping fetch")
                return RepliesResponse(
                    replies = emptyList(),
                    etag = currentETag,
                    hasChanges = false
                )
            }

            // First check metadata to see if file changed
            val metadataUrl = "https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o/replies.json?alt=json"

            val metadataRequest = Request.Builder()
                .url(metadataUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()

            httpClient.newCall(metadataRequest).execute().use { metadataResponse ->
                if (!metadataResponse.isSuccessful) {
                    if (metadataResponse.code == 404) {
                        Log.d(TAG, "Replies file not found in GCS")
                        return RepliesResponse(
                            replies = emptyList(),
                            etag = null,
                            hasChanges = false
                        )
                    }
                    Log.e(TAG, "Metadata request failed: ${metadataResponse.code}")
                    return null
                }

                val metadataJson = metadataResponse.body?.string().orEmpty()
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                val metadata: Map<String, Any> = gson.fromJson(metadataJson, mapType)

                val newETag = metadata["etag"] as? String

                if (newETag != null && newETag == currentETag) {
                    Log.d(TAG, "ETag unchanged, no new data")
                    return RepliesResponse(
                        replies = emptyList(),
                        etag = newETag,
                        hasChanges = false
                    )
                }

                // Download the actual file
                val downloadUrl = "https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o/replies.json?alt=media"

                val downloadRequest = Request.Builder()
                    .url(downloadUrl)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                httpClient.newCall(downloadRequest).execute().use { downloadResponse ->
                    if (!downloadResponse.isSuccessful) {
                        Log.e(TAG, "Download request failed: ${downloadResponse.code}")
                        return null
                    }

                    val jsonContent = downloadResponse.body?.string().orEmpty()
                    val response = gson.fromJson(jsonContent, RepliesResponse::class.java)
                        ?: RepliesResponse(replies = emptyList())

                    response.etag = newETag
                    response.hasChanges = true

                    Log.d(TAG, "Successfully fetched ${response.replies?.size ?: 0} replies")
                    response
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching replies from GCS", e)
            null
        }
    }

    fun fetchTopics(): List<String> {
        return try {
            val token = getAccessToken() ?: return getDefaultTopics()

            val url = "https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o/topics.json?alt=media"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        Log.d(TAG, "Topics file not found, returning default topics")
                        return getDefaultTopics()
                    }
                    Log.e(TAG, "Topics request failed: ${response.code}")
                    return getDefaultTopics()
                }

                val jsonContent = response.body?.string().orEmpty()
                val mapType = object : TypeToken<Map<String, List<String>>>() {}.type
                val topicsData: Map<String, List<String>> = gson.fromJson(jsonContent, mapType)

                topicsData["topics"] ?: getDefaultTopics()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching topics from GCS", e)
            getDefaultTopics()
        }
    }

    fun uploadTopics(topics: List<String>): Boolean {
        return try {
            val token = getAccessToken() ?: return false

            val topicsData = mapOf(
                "topics" to topics,
                "last_updated" to Date().toString()
            )

            val jsonContent = gson.toJson(topicsData)
            val url = "https://storage.googleapis.com/upload/storage/v1/b/$BUCKET_NAME/o?uploadType=media&name=topics.json"

            val body = jsonContent.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully uploaded topics to GCS")
                    true
                } else {
                    Log.e(TAG, "Topics upload failed: ${response.code}")
                    false
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error uploading topics to GCS", e)
            false
        }
    }

    private fun getAccessToken(): String? {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken
        }

        return try {
            // Read service account key from assets
            context.assets.open("key.json")

            // For simplicity, using a basic implementation
            // In production, use Google Auth libraries
            // This is a simplified version - you should use proper OAuth2 flow

            // For now, return null to indicate authentication needed
            Log.w(TAG, "Access token generation not implemented - please implement OAuth2 flow")
            null

        } catch (e: IOException) {
            Log.e(TAG, "Error reading service account key", e)
            null
        }
    }

    private fun checkAndClearStaleProcessingLock(accessToken: String): Boolean {
        return try {
            val metadataUrl = "https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o/processing.lock?alt=json"

            val metadataRequest = Request.Builder()
                .url(metadataUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            httpClient.newCall(metadataRequest).execute().use { metadataResponse ->
                when (metadataResponse.code) {
                    404 -> true // No lock exists, proceed
                    !in 200..299 -> {
                        Log.w(TAG, "Lock check failed: ${metadataResponse.code}")
                        true // Assume no lock on error
                    }
                    else -> {
                        val metadataJson = metadataResponse.body?.string().orEmpty()
                        val mapType = object : TypeToken<Map<String, Any>>() {}.type
                        val metadata: Map<String, Any> = gson.fromJson(metadataJson, mapType)

                        val timeCreated = metadata["timeCreated"] as? String
                        if (timeCreated != null) {
                            val lockAge = System.currentTimeMillis() - java.time.Instant.parse(timeCreated).toEpochMilli()

                            // Clear stale locks older than 5 minutes
                            if (lockAge > 300000) {
                                deleteLock(accessToken)
                                Log.d(TAG, "Cleared stale processing lock")
                                true
                            } else {
                                false // Active lock exists
                            }
                        } else {
                            true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking processing lock", e)
            true // Proceed on error
        }
    }

    private fun deleteLock(accessToken: String) {
        try {
            val deleteUrl = "https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o/processing.lock"

            val deleteRequest = Request.Builder()
                .url(deleteUrl)
                .delete()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            httpClient.newCall(deleteRequest).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully deleted processing lock")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete processing lock", e)
        }
    }

    private fun getDefaultTopics(): List<String> = listOf(
        "AI",
        "Crypto",
        "Bitcoin",
        "Ethereum",
        "Motivation",
        "Machine Learning",
        "Blockchain",
        "Self-Improvement",
        "Tech Innovation",
        "Programming",
        "Science",
        "Gaming",
        "Environment",
        "Finance",
        "Health & Wellness"
    )
}