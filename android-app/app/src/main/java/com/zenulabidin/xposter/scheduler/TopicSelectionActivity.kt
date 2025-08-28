package com.zenulabidin.xposter.scheduler

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zenulabidin.xposter.scheduler.databinding.ActivityTopicSelectionBinding

class TopicSelectionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TopicSelectionActivity"
        private const val PREFS_NAME = "XPosterPrefs"
        private const val KEY_TOPICS = "selectedTopics"
    }

    private lateinit var binding: ActivityTopicSelectionBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupToolbar()
        setupUI()
        loadSavedTopics()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Select Topics"
        }
    }

    private fun setupUI() {
        binding.btnSave.setOnClickListener { saveTopics() }

        // TODO: Add checkboxes for predefined topics and custom topic input
        // For now, just show a placeholder message
        showToast("Topic selection UI to be implemented")
    }

    private fun loadSavedTopics() {
        val topicsJson = prefs.getString(KEY_TOPICS, null)
        // TODO: Load and display saved topics
    }

    private fun saveTopics() {
        // TODO: Collect selected topics and save
        // For now, save empty array to allow app to continue
        prefs.edit().putString(KEY_TOPICS, "[]").apply()
        showToast("Topics saved")
        finish()
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
        // binding will be automatically cleaned up by lateinit
    }
}