package com.zenulabidin.xposter.scheduler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zenulabidin.xposter.scheduler.databinding.ActivityMainBinding;
import com.zenulabidin.xposter.scheduler.models.PostReply;
import com.zenulabidin.xposter.scheduler.models.RepliesResponse;
import com.zenulabidin.xposter.scheduler.services.GCSService;
import com.zenulabidin.xposter.scheduler.adapters.PostReplyAdapter;
import com.zenulabidin.xposter.scheduler.workers.PostSyncWorker;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements PostReplyAdapter.PostActionListener {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "XPosterPrefs";
    private static final String KEY_ETAG = "lastETag";
    private static final String KEY_PROCESSED_IDS = "processedIds";
    private static final String KEY_TOPICS = "selectedTopics";
    
    private ActivityMainBinding binding;
    private PostReplyAdapter adapter;
    private GCSService gcsService;
    private Handler mainHandler;
    private SharedPreferences prefs;
    private Gson gson;
    
    private List<PostReply> posts = new ArrayList<>();
    private Set<String> processedIds = new HashSet<>();
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeServices();
        setupUI();
        loadLocalData();
        checkTopicsAndStart();
    }

    private void initializeServices() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        gson = new Gson();
        gcsService = new GCSService(this);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Load processed IDs
        String processedIdsJson = prefs.getString(KEY_PROCESSED_IDS, "[]");
        Type setType = new TypeToken<HashSet<String>>(){}.getType();
        processedIds = gson.fromJson(processedIdsJson, setType);
        if (processedIds == null) {
            processedIds = new HashSet<>();
        }
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        
        // Setup RecyclerView
        adapter = new PostReplyAdapter(this, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener(this::refreshPosts);
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
        
        // Setup FAB
        binding.fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, TopicSelectionActivity.class);
            startActivity(intent);
        });
        
        // Setup refresh button
        binding.btnRefresh.setOnClickListener(v -> refreshPosts());
        
        // Setup schedule button
        binding.btnSchedule.setOnClickListener(v -> scheduleAllPosts());
        
        updateUI();
    }

    private void loadLocalData() {
        updateStats();
    }

    private void checkTopicsAndStart() {
        String topicsJson = prefs.getString(KEY_TOPICS, null);
        if (topicsJson == null || topicsJson.equals("[]")) {
            // No topics selected, open topic selection
            Intent intent = new Intent(this, TopicSelectionActivity.class);
            startActivity(intent);
        } else {
            startBackgroundSync();
            refreshPosts();
        }
    }

    private void startBackgroundSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                PostSyncWorker.class, 
                15, 
                TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .build();

        WorkManager.getInstance(this).enqueue(syncWorkRequest);
        Log.d(TAG, "Background sync work scheduled");
    }

    private void refreshPosts() {
        if (isLoading) return;
        
        isLoading = true;
        binding.swipeRefresh.setRefreshing(true);
        binding.btnRefresh.setEnabled(false);
        
        new Thread(() -> {
            try {
                String currentETag = prefs.getString(KEY_ETAG, null);
                RepliesResponse response = gcsService.fetchReplies(currentETag);
                
                mainHandler.post(() -> {
                    if (response != null) {
                        if (response.hasChanges) {
                            // Filter out processed posts
                            List<PostReply> newPosts = new ArrayList<>();
                            for (PostReply post : response.replies) {
                                if (!processedIds.contains(post.getId())) {
                                    newPosts.add(post);
                                }
                            }
                            
                            posts.clear();
                            posts.addAll(newPosts);
                            adapter.updatePosts(posts);
                            
                            // Save new ETag
                            prefs.edit().putString(KEY_ETAG, response.etag).apply();
                            
                            showToast("Loaded " + newPosts.size() + " new posts");
                        } else {
                            showToast("No new posts available");
                        }
                    } else {
                        showToast("Failed to fetch posts");
                    }
                    
                    updateUI();
                    isLoading = false;
                    binding.swipeRefresh.setRefreshing(false);
                    binding.btnRefresh.setEnabled(true);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing posts", e);
                mainHandler.post(() -> {
                    showToast("Error fetching posts: " + e.getMessage());
                    isLoading = false;
                    binding.swipeRefresh.setRefreshing(false);
                    binding.btnRefresh.setEnabled(true);
                });
            }
        }).start();
    }

    private void scheduleAllPosts() {
        List<PostReply> validPosts = new ArrayList<>();
        for (PostReply post : posts) {
            if (post.getReply() != null && !post.getReply().trim().isEmpty()) {
                validPosts.add(post);
            }
        }
        
        if (validPosts.isEmpty()) {
            showToast("No posts with replies available to schedule");
            return;
        }
        
        binding.btnSchedule.setEnabled(false);
        binding.btnSchedule.setText("Scheduling...");
        
        // Process posts with delays in background
        new Thread(() -> {
            try {
                for (int i = 0; i < validPosts.size(); i++) {
                    PostReply post = validPosts.get(i);
                    
                    // Random delay between 15-45 seconds
                    if (i > 0) {
                        int delay = (int) (Math.random() * (45 - 15) + 15);
                        Thread.sleep(delay * 1000);
                    }
                    
                    final int currentIndex = i + 1;
                    final int total = validPosts.size();
                    
                    mainHandler.post(() -> {
                        binding.btnSchedule.setText("Processing " + currentIndex + "/" + total);
                    });
                    
                    // Start posting activity for this post
                    Intent intent = new Intent(this, PostingActivity.class);
                    intent.putExtra("post_data", gson.toJson(post));
                    startActivity(intent);
                    
                    // Mark as processed
                    markAsProcessed(post.getId());
                }
                
                mainHandler.post(() -> {
                    showToast("Scheduled " + validPosts.size() + " posts");
                    // Remove scheduled posts from list
                    posts.removeAll(validPosts);
                    adapter.updatePosts(posts);
                    updateUI();
                    binding.btnSchedule.setEnabled(true);
                    binding.btnSchedule.setText("Schedule All");
                });
                
            } catch (InterruptedException e) {
                Log.e(TAG, "Scheduling interrupted", e);
                mainHandler.post(() -> {
                    showToast("Scheduling was interrupted");
                    binding.btnSchedule.setEnabled(true);
                    binding.btnSchedule.setText("Schedule All");
                });
            }
        }).start();
    }

    @Override
    public void onPostNow(PostReply post) {
        Intent intent = new Intent(this, PostingActivity.class);
        intent.putExtra("post_data", gson.toJson(post));
        startActivity(intent);
        markAsProcessed(post.getId());
    }

    @Override
    public void onDiscard(PostReply post) {
        posts.remove(post);
        adapter.updatePosts(posts);
        markAsProcessed(post.getId());
        updateUI();
        showToast("Post discarded");
    }

    private void markAsProcessed(String postId) {
        processedIds.add(postId);
        String processedIdsJson = gson.toJson(processedIds);
        prefs.edit().putString(KEY_PROCESSED_IDS, processedIdsJson).apply();
    }

    private void updateUI() {
        updateStats();
        
        // Show/hide empty state
        if (posts.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
        
        // Update schedule button
        long validPosts = posts.stream().filter(p -> p.getReply() != null && !p.getReply().trim().isEmpty()).count();
        binding.btnSchedule.setEnabled(validPosts > 0);
    }

    private void updateStats() {
        int totalPosts = posts.size();
        long readyPosts = posts.stream().filter(p -> p.getReply() != null && !p.getReply().trim().isEmpty()).count();
        double avgScore = posts.isEmpty() ? 0 : posts.stream().mapToInt(PostReply::getScore).average().orElse(0);
        
        binding.textTotalPosts.setText(String.valueOf(totalPosts));
        binding.textReadyPosts.setText(String.valueOf(readyPosts));
        binding.textAvgScore.setText(String.format("%.0f", avgScore));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if topics were updated
        loadLocalData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }
}