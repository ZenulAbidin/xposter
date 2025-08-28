package com.zenulabidin.xposter.scheduler;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.zenulabidin.xposter.scheduler.databinding.ActivityPostingBinding;
import com.zenulabidin.xposter.scheduler.models.PostReply;

public class PostingActivity extends AppCompatActivity {
    private static final String TAG = "PostingActivity";

    private ActivityPostingBinding binding;
    private PostReply currentPost;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        gson = new Gson();
        
        setupToolbar();
        loadPostData();
        setupWebView();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post Reply");
        }
    }

    private void loadPostData() {
        Intent intent = getIntent();
        String postDataJson = intent.getStringExtra("post_data");
        
        if (postDataJson != null) {
            try {
                currentPost = gson.fromJson(postDataJson, PostReply.class);
                Log.d(TAG, "Loaded post data for: " + currentPost.getId());
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse post data", e);
                showToast("Failed to load post data");
                finish();
            }
        } else {
            Log.e(TAG, "No post data provided");
            showToast("No post data provided");
            finish();
        }
    }

    private void setupWebView() {
        if (currentPost == null) {
            return;
        }

        // Configure WebView
        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setDomStorageEnabled(true);
        
        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page loaded: " + url);
                
                // TODO: Inject reply text and simulate clicking reply button
                // This requires careful implementation to comply with X's terms
                showToast("Page loaded. Manual reply posting required.");
            }
        });
        
        // Load the post URL
        String postUrl = "https://x.com/" + currentPost.getHandle() + "/status/" + currentPost.getId();
        binding.webView.loadUrl(postUrl);
        
        Log.d(TAG, "Loading post URL: " + postUrl);
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
            binding.webView.destroy();
            binding = null;
        }
    }
}