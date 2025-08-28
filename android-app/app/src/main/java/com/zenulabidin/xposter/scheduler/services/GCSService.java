package com.zenulabidin.xposter.scheduler.services;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zenulabidin.xposter.scheduler.models.RepliesResponse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GCSService {
    private static final String TAG = "GCSService";
    private static final String BUCKET_NAME = "your-bucket-name";
    private static final String PROJECT_ID = "your-project-id";
    
    private final Context context;
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    private String accessToken = null;
    private long tokenExpiry = 0;

    public GCSService(Context context) {
        this.context = context;
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public RepliesResponse fetchReplies(String currentETag) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                Log.e(TAG, "Failed to get access token");
                return null;
            }

            // Check for processing lock to avoid race conditions
            if (!checkAndClearStaleProcessingLock(accessToken)) {
                Log.d(TAG, "Another instance is processing, skipping fetch");
                return new RepliesResponse(new ArrayList<>(), null, currentETag, false);
            }

            // First check metadata to see if file changed
            String metadataUrl = String.format(
                "https://storage.googleapis.com/storage/v1/b/%s/o/replies.json?alt=json",
                BUCKET_NAME
            );

            Request metadataRequest = new Request.Builder()
                    .url(metadataUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response metadataResponse = httpClient.newCall(metadataRequest).execute()) {
                if (!metadataResponse.isSuccessful()) {
                    if (metadataResponse.code() == 404) {
                        Log.d(TAG, "Replies file not found in GCS");
                        return new RepliesResponse(new ArrayList<>(), null, null, false);
                    }
                    Log.e(TAG, "Metadata request failed: " + metadataResponse.code());
                    return null;
                }

                String metadataJson = metadataResponse.body().string();
                Type mapType = new TypeToken<java.util.Map<String, Object>>(){}.getType();
                java.util.Map<String, Object> metadata = gson.fromJson(metadataJson, mapType);
                
                String newETag = (String) metadata.get("etag");
                
                if (newETag != null && newETag.equals(currentETag)) {
                    Log.d(TAG, "ETag unchanged, no new data");
                    return new RepliesResponse(new ArrayList<>(), null, newETag, false);
                }

                // Download the actual file
                String downloadUrl = String.format(
                    "https://storage.googleapis.com/storage/v1/b/%s/o/replies.json?alt=media",
                    BUCKET_NAME
                );

                Request downloadRequest = new Request.Builder()
                        .url(downloadUrl)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();

                try (Response downloadResponse = httpClient.newCall(downloadRequest).execute()) {
                    if (!downloadResponse.isSuccessful()) {
                        Log.e(TAG, "Download request failed: " + downloadResponse.code());
                        return null;
                    }

                    String jsonContent = downloadResponse.body().string();
                    RepliesResponse response = gson.fromJson(jsonContent, RepliesResponse.class);
                    
                    if (response == null) {
                        response = new RepliesResponse();
                        response.replies = new ArrayList<>();
                    }
                    
                    response.etag = newETag;
                    response.hasChanges = true;
                    
                    Log.d(TAG, "Successfully fetched " + (response.replies != null ? response.replies.size() : 0) + " replies");
                    return response;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching replies from GCS", e);
            return null;
        }
    }

    public List<String> fetchTopics() {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return getDefaultTopics();
            }

            String url = String.format(
                "https://storage.googleapis.com/storage/v1/b/%s/o/topics.json?alt=media",
                BUCKET_NAME
            );

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (response.code() == 404) {
                        Log.d(TAG, "Topics file not found, returning default topics");
                        return getDefaultTopics();
                    }
                    Log.e(TAG, "Topics request failed: " + response.code());
                    return getDefaultTopics();
                }

                String jsonContent = response.body().string();
                Type mapType = new TypeToken<java.util.Map<String, List<String>>>(){}.getType();
                java.util.Map<String, List<String>> topicsData = gson.fromJson(jsonContent, mapType);
                
                List<String> topics = topicsData.get("topics");
                return topics != null ? topics : getDefaultTopics();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching topics from GCS", e);
            return getDefaultTopics();
        }
    }

    public boolean uploadTopics(List<String> topics) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return false;
            }

            java.util.Map<String, Object> topicsData = new java.util.HashMap<>();
            topicsData.put("topics", topics);
            topicsData.put("last_updated", new java.util.Date().toString());

            String jsonContent = gson.toJson(topicsData);

            String url = String.format(
                "https://storage.googleapis.com/upload/storage/v1/b/%s/o?uploadType=media&name=topics.json",
                BUCKET_NAME
            );

            RequestBody body = RequestBody.create(
                jsonContent,
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully uploaded topics to GCS");
                    return true;
                } else {
                    Log.e(TAG, "Topics upload failed: " + response.code());
                    return false;
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading topics to GCS", e);
            return false;
        }
    }

    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken;
        }

        try {
            // Read service account key from assets
            InputStream keyStream = context.getAssets().open("key.json");
            
            // For simplicity, using a basic implementation
            // In production, use Google Auth libraries
            // This is a simplified version - you should use proper OAuth2 flow
            
            // For now, return null to indicate authentication needed
            Log.w(TAG, "Access token generation not implemented - please implement OAuth2 flow");
            return null;
            
        } catch (IOException e) {
            Log.e(TAG, "Error reading service account key", e);
            return null;
        }
    }

    private boolean checkAndClearStaleProcessingLock(String accessToken) {
        try {
            String metadataUrl = String.format(
                "https://storage.googleapis.com/storage/v1/b/%s/o/processing.lock?alt=json",
                BUCKET_NAME
            );

            Request metadataRequest = new Request.Builder()
                    .url(metadataUrl)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response metadataResponse = httpClient.newCall(metadataRequest).execute()) {
                if (metadataResponse.code() == 404) {
                    return true; // No lock exists, proceed
                }
                
                if (!metadataResponse.isSuccessful()) {
                    Log.w(TAG, "Lock check failed: " + metadataResponse.code());
                    return true; // Assume no lock on error
                }

                String metadataJson = metadataResponse.body().string();
                Type mapType = new TypeToken<java.util.Map<String, Object>>(){}.getType();
                java.util.Map<String, Object> metadata = gson.fromJson(metadataJson, mapType);
                
                String timeCreated = (String) metadata.get("timeCreated");
                if (timeCreated != null) {
                    long lockAge = System.currentTimeMillis() - java.time.Instant.parse(timeCreated).toEpochMilli();
                    
                    // Clear stale locks older than 5 minutes
                    if (lockAge > 300000) {
                        deleteLock(accessToken);
                        Log.d(TAG, "Cleared stale processing lock");
                        return true;
                    } else {
                        return false; // Active lock exists
                    }
                }
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking processing lock", e);
            return true; // Proceed on error
        }
    }

    private void deleteLock(String accessToken) {
        try {
            String deleteUrl = String.format(
                "https://storage.googleapis.com/storage/v1/b/%s/o/processing.lock",
                BUCKET_NAME
            );

            Request deleteRequest = new Request.Builder()
                    .url(deleteUrl)
                    .delete()
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = httpClient.newCall(deleteRequest).execute()) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully deleted processing lock");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to delete processing lock", e);
        }
    }

    private List<String> getDefaultTopics() {
        List<String> topics = new ArrayList<>();
        topics.add("AI");
        topics.add("Crypto");
        topics.add("Bitcoin");
        topics.add("Ethereum");
        topics.add("Motivation");
        topics.add("Machine Learning");
        topics.add("Blockchain");
        topics.add("Self-Improvement");
        topics.add("Tech Innovation");
        topics.add("Programming");
        topics.add("Science");
        topics.add("Gaming");
        topics.add("Environment");
        topics.add("Finance");
        topics.add("Health & Wellness");
        return topics;
    }
}