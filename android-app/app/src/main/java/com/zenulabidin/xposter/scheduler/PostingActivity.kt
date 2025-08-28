package com.zenulabidin.xposter.scheduler

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.zenulabidin.xposter.scheduler.databinding.ActivityPostingBinding
import com.zenulabidin.xposter.scheduler.models.PostReply

class PostingActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PostingActivity"
    }

    private lateinit var binding: ActivityPostingBinding
    private var currentPost: PostReply? = null
    private lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gson = Gson()

        setupToolbar()
        loadPostData()
        setupWebView()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Post Reply"
        }
    }

    private fun loadPostData() {
        val postDataJson = intent.getStringExtra("post_data")

        if (postDataJson != null) {
            try {
                currentPost = gson.fromJson(postDataJson, PostReply::class.java)
                Log.d(TAG, "Loaded post data for: ${currentPost?.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse post data", e)
                showToast("Failed to load post data")
                finish()
            }
        } else {
            Log.e(TAG, "No post data provided")
            showToast("No post data provided")
            finish()
        }
    }

    private fun setupWebView() {
        currentPost ?: return

        // Configure WebView
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded: $url")

                // TODO: Inject reply text and simulate clicking reply button
                // This requires careful implementation to comply with X's terms
                showToast("Page loaded. Manual reply posting required.")
            }
        }

        // Load the post URL
        val postUrl = "https://x.com/${currentPost?.handle}/status/${currentPost?.id}"
        binding.webView.loadUrl(postUrl)

        Log.d(TAG, "Loading post URL: $postUrl")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
        // binding will be automatically cleaned up by lateinit
    }
}