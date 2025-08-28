package com.zenulabidin.xposter.scheduler.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zenulabidin.xposter.scheduler.models.RepliesResponse;
import com.zenulabidin.xposter.scheduler.services.GCSService;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class PostSyncWorker extends Worker {
    private static final String TAG = "PostSyncWorker";
    private static final String PREFS_NAME = "XPosterPrefs";
    private static final String KEY_ETAG = "lastETag";
    private static final String KEY_PROCESSED_IDS = "processedIds";

    public PostSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting background sync work");
        
        try {
            Context context = getApplicationContext();
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            GCSService gcsService = new GCSService(context);
            Gson gson = new Gson();
            
            // Get current ETag
            String currentETag = prefs.getString(KEY_ETAG, null);
            
            // Fetch replies from GCS
            RepliesResponse response = gcsService.fetchReplies(currentETag);
            
            if (response != null && response.hasChanges) {
                // Load processed IDs
                String processedIdsJson = prefs.getString(KEY_PROCESSED_IDS, "[]");
                Type setType = new TypeToken<HashSet<String>>(){}.getType();
                Set<String> processedIds = gson.fromJson(processedIdsJson, setType);
                if (processedIds == null) {
                    processedIds = new HashSet<>();
                }
                
                // Count new posts
                int newPostCount = 0;
                for (com.zenulabidin.xposter.scheduler.models.PostReply post : response.replies) {
                    if (!processedIds.contains(post.getId())) {
                        newPostCount++;
                    }
                }
                
                // Save new ETag
                prefs.edit().putString(KEY_ETAG, response.etag).apply();
                
                Log.d(TAG, "Background sync completed. Found " + newPostCount + " new posts");
                return Result.success();
            } else {
                Log.d(TAG, "Background sync completed. No new posts");
                return Result.success();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Background sync failed", e);
            return Result.retry();
        }
    }
}