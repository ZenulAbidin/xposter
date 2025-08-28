package com.zenulabidin.xposter.scheduler.workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zenulabidin.xposter.scheduler.models.RepliesResponse
import com.zenulabidin.xposter.scheduler.services.GCSService

class PostSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "PostSyncWorker"
        private const val PREFS_NAME = "XPosterPrefs"
        private const val KEY_ETAG = "lastETag"
        private const val KEY_PROCESSED_IDS = "processedIds"
    }

    override fun doWork(): Result {
        Log.d(TAG, "Starting background sync work")

        return try {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val gcsService = GCSService(context)
            val gson = Gson()

            // Get current ETag
            val currentETag = prefs.getString(KEY_ETAG, null)

            // Fetch replies from GCS
            val response = gcsService.fetchReplies(currentETag)

            if (response != null && response.hasChanges) {
                // Load processed IDs
                val processedIdsJson = prefs.getString(KEY_PROCESSED_IDS, "[]").orEmpty()
                val setType = object : TypeToken<MutableSet<String>>() {}.type
                val processedIds: MutableSet<String>? = gson.fromJson(processedIdsJson, setType)
                val finalProcessedIds = processedIds ?: mutableSetOf()

                // Count new posts
                val newPostCount = response.replies?.count { post ->
                    !finalProcessedIds.contains(post.id)
                } ?: 0

                // Save new ETag
                prefs.edit().putString(KEY_ETAG, response.etag).apply()

                Log.d(TAG, "Background sync completed. Found $newPostCount new posts")
                Result.success()
            } else {
                Log.d(TAG, "Background sync completed. No new posts")
                Result.success()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Background sync failed", e)
            Result.retry()
        }
    }
}