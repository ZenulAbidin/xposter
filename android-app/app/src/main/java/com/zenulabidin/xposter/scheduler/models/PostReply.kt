package com.zenulabidin.xposter.scheduler.models

data class PostReply(
    var id: String? = null,
    var content: String? = null,
    var author: String? = null,
    var handle: String? = null,
    var createdAt: String? = null,
    var url: String? = null,
    var media: List<MediaItem>? = null,
    var score: Int = 0,
    var categories: List<String>? = null,
    var reply: String? = null,
    var imageDescription: String? = null,
    var processedAt: String? = null,
    var status: String? = null,
    var likes: Int = 0,
    var retweets: Int = 0,
    var replies: Int = 0
) {

    data class MediaItem(
        var type: String? = null,
        var url: String? = null,
        var altText: String? = null
    )

    val hasMedia: Boolean
        get() = media?.isNotEmpty() == true

    val hasValidReply: Boolean
        get() = !reply.isNullOrBlank()

    val authorInitial: String
        get() = author?.firstOrNull()?.uppercase() ?: "?"

    val formattedScore: String
        get() = score.toString()

    val categoriesString: String
        get() = categories?.joinToString(", ") ?: ""
}