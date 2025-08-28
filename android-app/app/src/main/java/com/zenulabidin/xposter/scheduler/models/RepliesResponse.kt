package com.zenulabidin.xposter.scheduler.models

data class RepliesResponse(
    var replies: List<PostReply>? = null,
    var lastUpdated: String? = null,
    var totalProcessed: Int = 0,
    var etag: String? = null,
    var hasChanges: Boolean = false
)