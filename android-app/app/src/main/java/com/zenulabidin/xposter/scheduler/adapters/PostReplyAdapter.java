package com.zenulabidin.xposter.scheduler.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.zenulabidin.xposter.scheduler.R;
import com.zenulabidin.xposter.scheduler.models.PostReply;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostReplyAdapter extends RecyclerView.Adapter<PostReplyAdapter.PostViewHolder> {
    
    private List<PostReply> posts;
    private Context context;
    private PostActionListener listener;
    
    public interface PostActionListener {
        void onPostNow(PostReply post);
        void onDiscard(PostReply post);
    }
    
    public PostReplyAdapter(Context context, PostActionListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    public void updatePosts(List<PostReply> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_reply, parent, false);
        return new PostViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostReply post = posts.get(position);
        holder.bind(post);
    }
    
    @Override
    public int getItemCount() {
        return posts != null ? posts.size() : 0;
    }
    
    class PostViewHolder extends RecyclerView.ViewHolder {
        TextView authorName, authorHandle, postContent, postScore, replyText, postTime;
        Button postNowBtn, discardBtn;
        View mediaIndicator;
        
        PostViewHolder(View itemView) {
            super(itemView);
            authorName = itemView.findViewById(R.id.text_author_name);
            authorHandle = itemView.findViewById(R.id.text_author_handle);
            postContent = itemView.findViewById(R.id.text_post_content);
            postScore = itemView.findViewById(R.id.text_post_score);
            replyText = itemView.findViewById(R.id.text_reply_content);
            postTime = itemView.findViewById(R.id.text_post_time);
            postNowBtn = itemView.findViewById(R.id.btn_post_now);
            discardBtn = itemView.findViewById(R.id.btn_discard);
            mediaIndicator = itemView.findViewById(R.id.media_indicator);
        }
        
        void bind(PostReply post) {
            authorName.setText(post.getAuthor());
            authorHandle.setText("@" + post.getHandle());
            postContent.setText(truncateText(post.getContent(), 200));
            postScore.setText("Score: " + post.getScore());
            
            if (post.hasValidReply()) {
                replyText.setText(post.getReply());
                replyText.setVisibility(View.VISIBLE);
                postNowBtn.setEnabled(true);
            } else {
                replyText.setVisibility(View.GONE);
                postNowBtn.setEnabled(false);
            }
            
            // Format time
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
                Date date = inputFormat.parse(post.getCreatedAt());
                String timeAgo = getTimeAgo(date);
                postTime.setText(timeAgo);
            } catch (Exception e) {
                postTime.setText(post.getCreatedAt());
            }
            
            // Show media indicator
            mediaIndicator.setVisibility(post.hasMedia() ? View.VISIBLE : View.GONE);
            
            // Set click listeners
            postNowBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostNow(post);
                }
            });
            
            discardBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDiscard(post);
                }
            });
        }
        
        private String truncateText(String text, int maxLength) {
            if (text == null || text.length() <= maxLength) {
                return text;
            }
            return text.substring(0, maxLength) + "...";
        }
        
        private String getTimeAgo(Date date) {
            long now = System.currentTimeMillis();
            long diffInMs = now - date.getTime();
            long diffInMinutes = diffInMs / (60 * 1000);
            long diffInHours = diffInMinutes / 60;
            long diffInDays = diffInHours / 24;
            
            if (diffInMinutes < 60) {
                return diffInMinutes + "m ago";
            } else if (diffInHours < 24) {
                return diffInHours + "h ago";
            } else {
                return diffInDays + "d ago";
            }
        }
    }
}