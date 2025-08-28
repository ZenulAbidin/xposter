package com.zenulabidin.xposter.scheduler.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zenulabidin.xposter.scheduler.R
import com.zenulabidin.xposter.scheduler.models.PostReply
import java.text.SimpleDateFormat
import java.util.*

class PostReplyAdapter(
    private val context: Context,
    private val listener: PostActionListener
) : RecyclerView.Adapter<PostReplyAdapter.PostViewHolder>() {

    interface PostActionListener {
        fun onPostNow(post: PostReply)
        fun onDiscard(post: PostReply)
    }

    private var posts: List<PostReply> = emptyList()

    fun updatePosts(posts: List<PostReply>) {
        this.posts = posts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post_reply, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorName: TextView = itemView.findViewById(R.id.text_author_name)
        private val authorHandle: TextView = itemView.findViewById(R.id.text_author_handle)
        private val postContent: TextView = itemView.findViewById(R.id.text_post_content)
        private val postScore: TextView = itemView.findViewById(R.id.text_post_score)
        private val replyText: TextView = itemView.findViewById(R.id.text_reply_content)
        private val postTime: TextView = itemView.findViewById(R.id.text_post_time)
        private val postNowBtn: Button = itemView.findViewById(R.id.btn_post_now)
        private val discardBtn: Button = itemView.findViewById(R.id.btn_discard)
        private val mediaIndicator: View = itemView.findViewById(R.id.media_indicator)

        fun bind(post: PostReply) {
            authorName.text = post.author
            authorHandle.text = "@${post.handle}"
            postContent.text = post.content?.truncateText(200)
            postScore.text = "Score: ${post.score}"

            if (post.hasValidReply) {
                replyText.text = post.reply
                replyText.visibility = View.VISIBLE
                postNowBtn.isEnabled = true
            } else {
                replyText.visibility = View.GONE
                postNowBtn.isEnabled = false
            }

            // Format time
            postTime.text = try {
                val inputFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH)
                val date = post.createdAt?.let { inputFormat.parse(it) }
                date?.getTimeAgo() ?: post.createdAt
            } catch (e: Exception) {
                post.createdAt
            }

            // Show media indicator
            mediaIndicator.visibility = if (post.hasMedia) View.VISIBLE else View.GONE

            // Set click listeners
            postNowBtn.setOnClickListener {
                listener.onPostNow(post)
            }

            discardBtn.setOnClickListener {
                listener.onDiscard(post)
            }
        }

        private fun String?.truncateText(maxLength: Int): String? {
            return this?.let {
                if (length <= maxLength) this else substring(0, maxLength) + "..."
            }
        }

        private fun Date.getTimeAgo(): String {
            val now = System.currentTimeMillis()
            val diffInMs = now - time
            val diffInMinutes = diffInMs / (60 * 1000)
            val diffInHours = diffInMinutes / 60
            val diffInDays = diffInHours / 24

            return when {
                diffInMinutes < 60 -> "${diffInMinutes}m ago"
                diffInHours < 24 -> "${diffInHours}h ago"
                else -> "${diffInDays}d ago"
            }
        }
    }
}