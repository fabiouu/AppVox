package dev.fabiou.appvox.review

internal data class ReviewRequest<RequestParameters>(
    val parameters: RequestParameters,
    val nextToken: String? = null
)
