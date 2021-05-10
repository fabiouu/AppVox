package dev.fabiou.appvox

import dev.fabiou.appvox.configuration.Constant.MIN_REQUEST_DELAY
import dev.fabiou.appvox.configuration.RequestConfiguration
import dev.fabiou.appvox.exception.AppVoxError
import dev.fabiou.appvox.exception.AppVoxException
import dev.fabiou.appvox.exception.AppVoxNetworkException
import dev.fabiou.appvox.review.ReviewIterator
import dev.fabiou.appvox.review.ReviewRequest
import dev.fabiou.appvox.review.googleplay.GooglePlayReviewConverter
import dev.fabiou.appvox.review.googleplay.GooglePlayReviewService
import dev.fabiou.appvox.review.googleplay.constant.GooglePlayLanguage
import dev.fabiou.appvox.review.googleplay.constant.GooglePlaySortType
import dev.fabiou.appvox.review.googleplay.domain.GooglePlayReview
import dev.fabiou.appvox.review.googleplay.domain.GooglePlayReviewRequestParameters
import dev.fabiou.appvox.util.HttpUtil
import dev.fabiou.appvox.util.retryRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlin.contracts.ExperimentalContracts

/**
 * This class consists of the main methods for interacting with Google Play
 */
class GooglePlay(
    val config: RequestConfiguration = RequestConfiguration(delay = MIN_REQUEST_DELAY)
) {

    companion object {
        private const val DEFAULT_BATCH_SIZE = 40
        private const val MIN_RETRY_DELAY = 3000L
        private const val MAX_RETRY_ATTEMPTS = 5L
        private const val DELAY_FACTOR = 2
    }

    private val googlePlayReviewService = GooglePlayReviewService(config)

    private val googlePlayReviewConverter = GooglePlayReviewConverter()

    init {
        config.proxyAuthentication?.let {
            HttpUtil.setAuthenticator(it.userName, it.password)
        }
    }

    /**
     * Returns a Kotlin Flow of reviews
     */
    @ExperimentalContracts
    fun reviews(
        appId: String,
        language: GooglePlayLanguage = GooglePlayLanguage.ENGLISH_US,
        sortType: GooglePlaySortType = GooglePlaySortType.RECENT,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): Flow<GooglePlayReview> {
        var currentDelay = MIN_RETRY_DELAY
        return flow {

            if (config.delay < MIN_REQUEST_DELAY) {
                throw AppVoxException(AppVoxError.REQ_DELAY_TOO_SHORT)
            }

            val iterator = ReviewIterator(
                converter = googlePlayReviewConverter,
                service = googlePlayReviewService,
                request = ReviewRequest(
                    GooglePlayReviewRequestParameters(
                        appId = appId,
                        language = language,
                        sortType = sortType,
                        batchSize = batchSize
                    )
                )
            )

            iterator.forEach { reviews ->
                reviews.forEach { review ->
                    emit(review)
                }
                delay(timeMillis = config.delay.toLong())
            }
        }.retry(retries = MAX_RETRY_ATTEMPTS) { cause ->
            if (cause is Exception) { //AppVoxNetworkException
                delay(timeMillis = currentDelay)
                currentDelay *= DELAY_FACTOR
                true
            } else {
                false
            }
        }
    }
}
