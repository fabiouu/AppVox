package dev.fabiou.appvox.review.itunesrss.domain

import dev.fabiou.appvox.review.appstore.classification.AppStoreCommentType
import dev.fabiou.appvox.review.appstore.classification.AppStoreCommentType.EXTENSIVE
import dev.fabiou.appvox.review.appstore.classification.AppStoreCommentType.IRRELEVANT
import dev.fabiou.appvox.review.appstore.classification.AppStoreUserType
import dev.fabiou.appvox.review.appstore.classification.AppStoreUserType.DETRACTOR
import dev.fabiou.appvox.review.appstore.classification.AppStoreUserType.PROMOTER
import dev.fabiou.appvox.review.itunesrss.constant.AppStoreRegion
import java.time.ZonedDateTime

data class ItunesRssReview(
    /**
     * Review Id
     */
    val id: String,

    /**
     * AppStore region
     */
    val region: AppStoreRegion,

    /**
     * Url to the user's comment
     */
    val url: String? = null,

    /**
     * List of comments written by the user and developer. If the user edited his comment, comments size will be > 1
     */
    val comments: List<Comment>,
) {
    companion object {
        private const val LONG_REVIEW_THRESHOLD = 150

        private const val SHORT_REVIEW_THRESHOLD = 10

        private const val MIN_NEGATIVE_REVIEW_STAR = 1

        private const val MAX_NEGATIVE_REVIEW_STAR = 3 + 1

        private const val MIN_POSITIVE_REVIEW_STAR = 4

        private const val MAX_POSITIVE_REVIEW_STAR = 5 + 1

        private const val POPULAR_USER_THRESOLD = 100
    }

    /**
     * Most recent comment. Contains the conversation between user and developer
     */
    val latestComment: Comment
        get() = comments.first()

    /**
     * Most recent comment written by the user
     */
    val latestUserComment: UserComment
        get() = comments.first().userComment

    data class Comment(
        /**
         * Comment written by the user
         */
        val userComment: UserComment,
    )

    data class UserComment(
        /**
         * iTunes Author or User Name of the user who wrote the review
         */
        val userName: String,

        /**
         * Review rating from 1 (poor) to 5 (very good)
         */
        val rating: Int,

        /**
         * Title of the review written by the user (optional, can be null)
         */
        val title: String? = null,

        /**
         * iOS App Version
         */
        val appVersion: String? = null,

        /**
         * Comment written by the user
         */
        val text: String,

        /**
         * Time the user commented on iTunes
         */
        val time: ZonedDateTime? = null,

        /**
         * Number of times users found this comment useful (thumbs-up / upvote / like)
         */
        val likeCount: Int = 0
    ) {
        val userTypes: Set<AppStoreUserType>
            get() {
                val userPersonas = HashSet<AppStoreUserType>()
                when (rating) {
                    in MIN_NEGATIVE_REVIEW_STAR..MAX_NEGATIVE_REVIEW_STAR -> userPersonas.add(DETRACTOR)
                    in MIN_POSITIVE_REVIEW_STAR..MAX_POSITIVE_REVIEW_STAR -> userPersonas.add(PROMOTER)
                }
                return userPersonas
            }

        val commentTypes: Set<AppStoreCommentType>
            get() {
                val reviewTypes = HashSet<AppStoreCommentType>()
                val cleanCommentText = text.filter { !it.isWhitespace() }
                when {
                    cleanCommentText.length > LONG_REVIEW_THRESHOLD -> reviewTypes.add(EXTENSIVE)
                    cleanCommentText.length < SHORT_REVIEW_THRESHOLD -> reviewTypes.add(IRRELEVANT)
                    likeCount >= POPULAR_USER_THRESOLD -> reviewTypes.add(AppStoreCommentType.POPULAR)
                }
                return reviewTypes
            }
    }
}
