package com.zenulabidin.xposter.scheduler.models;

import java.util.List;

public class PostReply {
    private String id;
    private String content;
    private String author;
    private String handle;
    private String created_at;
    private String url;
    private List<MediaItem> media;
    private int score;
    private List<String> categories;
    private String reply;
    private String image_description;
    private String processed_at;
    private String status;
    private int likes;
    private int retweets;
    private int replies;

    public static class MediaItem {
        private String type;
        private String url;
        private String alt_text;

        // Getters
        public String getType() { return type; }
        public String getUrl() { return url; }
        public String getAltText() { return alt_text; }

        // Setters
        public void setType(String type) { this.type = type; }
        public void setUrl(String url) { this.url = url; }
        public void setAltText(String alt_text) { this.alt_text = alt_text; }
    }

    // Default constructor
    public PostReply() {}

    // Getters
    public String getId() { return id; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public String getHandle() { return handle; }
    public String getCreatedAt() { return created_at; }
    public String getUrl() { return url; }
    public List<MediaItem> getMedia() { return media; }
    public int getScore() { return score; }
    public List<String> getCategories() { return categories; }
    public String getReply() { return reply; }
    public String getImageDescription() { return image_description; }
    public String getProcessedAt() { return processed_at; }
    public String getStatus() { return status; }
    public int getLikes() { return likes; }
    public int getRetweets() { return retweets; }
    public int getReplies() { return replies; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setContent(String content) { this.content = content; }
    public void setAuthor(String author) { this.author = author; }
    public void setHandle(String handle) { this.handle = handle; }
    public void setCreatedAt(String created_at) { this.created_at = created_at; }
    public void setUrl(String url) { this.url = url; }
    public void setMedia(List<MediaItem> media) { this.media = media; }
    public void setScore(int score) { this.score = score; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public void setReply(String reply) { this.reply = reply; }
    public void setImageDescription(String image_description) { this.image_description = image_description; }
    public void setProcessedAt(String processed_at) { this.processed_at = processed_at; }
    public void setStatus(String status) { this.status = status; }
    public void setLikes(int likes) { this.likes = likes; }
    public void setRetweets(int retweets) { this.retweets = retweets; }
    public void setReplies(int replies) { this.replies = replies; }

    // Helper methods
    public boolean hasMedia() {
        return media != null && !media.isEmpty();
    }

    public boolean hasValidReply() {
        return reply != null && !reply.trim().isEmpty();
    }

    public String getAuthorInitial() {
        return (author != null && !author.isEmpty()) ? author.substring(0, 1).toUpperCase() : "?";
    }

    public String getFormattedScore() {
        return String.valueOf(score);
    }

    public String getCategoriesString() {
        if (categories == null || categories.isEmpty()) {
            return "";
        }
        return String.join(", ", categories);
    }
}