package com.zenulabidin.xposter.scheduler;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zenulabidin.xposter.scheduler.databinding.ActivityTopicSelectionBinding;

public class TopicSelectionActivity extends AppCompatActivity {
    private static final String TAG = "TopicSelectionActivity";
    private static final String PREFS_NAME = "XPosterPrefs";
    private static final String KEY_TOPICS = "selectedTopics";

    private ActivityTopicSelectionBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTopicSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        setupToolbar();
        setupUI();
        loadSavedTopics();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Select Topics");
        }
    }

    private void setupUI() {
        binding.btnSave.setOnClickListener(v -> saveTopics());
        
        // TODO: Add checkboxes for predefined topics and custom topic input
        // For now, just show a placeholder message
        showToast("Topic selection UI to be implemented");
    }

    private void loadSavedTopics() {
        String topicsJson = prefs.getString(KEY_TOPICS, null);
        // TODO: Load and display saved topics
    }

    private void saveTopics() {
        // TODO: Collect selected topics and save
        // For now, save empty array to allow app to continue
        prefs.edit().putString(KEY_TOPICS, "[]").apply();
        showToast("Topics saved");
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding = null;
        }
    }
}