package com.zenulabidin.xposter.scheduler.models;

import java.util.List;

public class RepliesResponse {
    public List<PostReply> replies;
    public String last_updated;
    public int total_processed;
    public String etag;
    public boolean hasChanges;

    public RepliesResponse() {}

    public RepliesResponse(List<PostReply> replies, String last_updated, String etag, boolean hasChanges) {
        this.replies = replies;
        this.last_updated = last_updated;
        this.etag = etag;
        this.hasChanges = hasChanges;
    }

    // Getters
    public List<PostReply> getReplies() { return replies; }
    public String getLastUpdated() { return last_updated; }
    public int getTotalProcessed() { return total_processed; }
    public String getEtag() { return etag; }
    public boolean isHasChanges() { return hasChanges; }

    // Setters
    public void setReplies(List<PostReply> replies) { this.replies = replies; }
    public void setLastUpdated(String last_updated) { this.last_updated = last_updated; }
    public void setTotalProcessed(int total_processed) { this.total_processed = total_processed; }
    public void setEtag(String etag) { this.etag = etag; }
    public void setHasChanges(boolean hasChanges) { this.hasChanges = hasChanges; }
}