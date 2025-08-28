package com.zenulabidin.xposter.scheduler

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zenulabidin.xposter.scheduler.databinding.ActivityMainBinding
import com.zenulabidin.xposter.scheduler.models.PostReply
import com.zenulabidin.xposter.scheduler.models.RepliesResponse
import com.zenulabidin.xposter.scheduler.services.GCSService
import com.zenulabidin.xposter.scheduler.adapters.PostReplyAdapter
import com.zenulabidin.xposter.scheduler.workers.PostSyncWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), PostReplyAdapter.PostActionListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "XPosterPrefs"
        private const val KEY_ETAG = "lastETag"
        private const val KEY_PROCESSED_IDS = "processedIds"
        private const val KEY_TOPICS = "selectedTopics"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PostReplyAdapter
    private lateinit var gcsService: GCSService
    private lateinit var mainHandler: Handler
    private lateinit var prefs: SharedPreferences
    private lateinit var gson: Gson

    private val posts = mutableListOf<PostReply>()
    private val processedIds = mutableSetOf<String>()
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeServices()
        setupUI()
        loadLocalData()
        checkTopicsAndStart()
    }

    private fun initializeServices() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        gson = Gson()
        gcsService = GCSService(this)
        mainHandler = Handler(Looper.getMainLooper())

        // Load processed IDs
        val processedIdsJson = prefs.getString(KEY_PROCESSED_IDS, "[]").orEmpty()
        val setType = object : TypeToken<MutableSet<String>>() {}.type
        val loadedIds: MutableSet<String>? = gson.fromJson(processedIdsJson, setType)
        processedIds.clear()
        loadedIds?.let { processedIds.addAll(it) }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)

        // Setup RecyclerView
        adapter = PostReplyAdapter(this, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.apply {
            setOnRefreshListener { refreshPosts() }
            setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )
        }

        // Setup FAB
        binding.fab.setOnClickListener {
            startActivity(Intent(this, TopicSelectionActivity::class.java))
        }

        // Setup refresh button
        binding.btnRefresh.setOnClickListener { refreshPosts() }

        // Setup schedule button
        binding.btnSchedule.setOnClickListener { scheduleAllPosts() }

        updateUI()
    }

    private fun loadLocalData() {
        updateStats()
    }

    private fun checkTopicsAndStart() {
        val topicsJson = prefs.getString(KEY_TOPICS, null)
        if (topicsJson.isNullOrEmpty() || topicsJson == "[]") {
            // No topics selected, open topic selection
            startActivity(Intent(this, TopicSelectionActivity::class.java))
        } else {
            startBackgroundSync()
            refreshPosts()
        }
    }

    private fun startBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = PeriodicWorkRequest.Builder(
            PostSyncWorker::class.java,
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(syncWorkRequest)
        Log.d(TAG, "Background sync work scheduled")
    }

    private fun refreshPosts() {
        if (isLoading) return

        isLoading = true
        binding.swipeRefresh.isRefreshing = true
        binding.btnRefresh.isEnabled = false

        Thread {
            try {
                val currentETag = prefs.getString(KEY_ETAG, null)
                val response = gcsService.fetchReplies(currentETag)

                mainHandler.post {
                    response?.let { repliesResponse ->
                        if (repliesResponse.hasChanges) {
                            // Filter out processed posts
                            val newPosts = repliesResponse.replies?.filter { post ->
                                !processedIds.contains(post.id)
                            } ?: emptyList()

                            posts.clear()
                            posts.addAll(newPosts)
                            adapter.updatePosts(posts)

                            // Save new ETag
                            prefs.edit().putString(KEY_ETAG, repliesResponse.etag).apply()

                            showToast("Loaded ${newPosts.size} new posts")
                        } else {
                            showToast("No new posts available")
                        }
                    } ?: run {
                        showToast("Failed to fetch posts")
                    }

                    updateUI()
                    isLoading = false
                    binding.swipeRefresh.isRefreshing = false
                    binding.btnRefresh.isEnabled = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing posts", e)
                mainHandler.post {
                    showToast("Error fetching posts: ${e.message}")
                    isLoading = false
                    binding.swipeRefresh.isRefreshing = false
                    binding.btnRefresh.isEnabled = true
                }
            }
        }.start()
    }

    private fun scheduleAllPosts() {
        val validPosts = posts.filter { post ->
            !post.reply.isNullOrBlank()
        }

        if (validPosts.isEmpty()) {
            showToast("No posts with replies available to schedule")
            return
        }

        binding.btnSchedule.apply {
            isEnabled = false
            text = "Scheduling..."
        }

        // Process posts with delays in background
        Thread {
            try {
                validPosts.forEachIndexed { index, post ->
                    // Random delay between 15-45 seconds
                    if (index > 0) {
                        val delay = (Math.random() * (45 - 15) + 15).toInt()
                        Thread.sleep(delay * 1000L)
                    }

                    val currentIndex = index + 1
                    val total = validPosts.size

                    mainHandler.post {
                        binding.btnSchedule.text = "Processing $currentIndex/$total"
                    }

                    // Start posting activity for this post
                    val intent = Intent(this, PostingActivity::class.java).apply {
                        putExtra("post_data", gson.toJson(post))
                    }
                    startActivity(intent)

                    // Mark as processed
                    markAsProcessed(post.id)
                }

                mainHandler.post {
                    showToast("Scheduled ${validPosts.size} posts")
                    // Remove scheduled posts from list
                    posts.removeAll(validPosts)
                    adapter.updatePosts(posts)
                    updateUI()
                    binding.btnSchedule.apply {
                        isEnabled = true
                        text = "Schedule All"
                    }
                }

            } catch (e: InterruptedException) {
                Log.e(TAG, "Scheduling interrupted", e)
                mainHandler.post {
                    showToast("Scheduling was interrupted")
                    binding.btnSchedule.apply {
                        isEnabled = true
                        text = "Schedule All"
                    }
                }
            }
        }.start()
    }

    override fun onPostNow(post: PostReply) {
        val intent = Intent(this, PostingActivity::class.java).apply {
            putExtra("post_data", gson.toJson(post))
        }
        startActivity(intent)
        markAsProcessed(post.id)
    }

    override fun onDiscard(post: PostReply) {
        posts.remove(post)
        adapter.updatePosts(posts)
        markAsProcessed(post.id)
        updateUI()
        showToast("Post discarded")
    }

    private fun markAsProcessed(postId: String?) {
        postId?.let {
            processedIds.add(it)
            val processedIdsJson = gson.toJson(processedIds)
            prefs.edit().putString(KEY_PROCESSED_IDS, processedIdsJson).apply()
        }
    }

    private fun updateUI() {
        updateStats()

        // Show/hide empty state
        if (posts.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }

        // Update schedule button
        val validPostsCount = posts.count { post ->
            !post.reply.isNullOrBlank()
        }
        binding.btnSchedule.isEnabled = validPostsCount > 0
    }

    private fun updateStats() {
        val totalPosts = posts.size
        val readyPosts = posts.count { post ->
            !post.reply.isNullOrBlank()
        }
        val avgScore = if (posts.isNotEmpty()) {
            posts.map { it.score }.average()
        } else {
            0.0
        }

        binding.textTotalPosts.text = totalPosts.toString()
        binding.textReadyPosts.text = readyPosts.toString()
        binding.textAvgScore.text = String.format("%.0f", avgScore)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Check if topics were updated
        loadLocalData()
    }

    override fun onDestroy() {
        super.onDestroy()
        // binding will be automatically cleaned up by lateinit
    }
}