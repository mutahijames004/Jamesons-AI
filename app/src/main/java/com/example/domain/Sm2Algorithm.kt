package com.example.domain

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

enum class ReviewRating(val quality: Int, val label: String) {
    AGAIN(quality = 1, label = "Again"),
    HARD(quality = 2, label = "Hard"),
    GOOD(quality = 3, label = "Good"),
    EASY(quality = 5, label = "Easy")
}

data class Sm2Result(
    val repetitionCount: Int,
    val intervalDays: Int,
    val easeFactor: Float,
    val nextReviewEpochDay: Long
)

object Sm2Algorithm {
    private const val MIN_EASE_FACTOR = 1.3f

    /**
     * Calculates the next review parameters using SuperMemo SM-2 spaced repetition algorithm.
     *
     * @param currentRepetitions Previous consecutive correct recalls
     * @param currentIntervalDays Previous interval in days
     * @param currentEaseFactor Current difficulty factor (default 2.5)
     * @param rating User feedback rating (AGAIN, HARD, GOOD, EASY)
     * @param todayEpochDay Current day timestamp
     */
    fun calculateNextReview(
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Float,
        rating: ReviewRating,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): Sm2Result {
        val q = rating.quality

        // 1. Calculate new Ease Factor
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val delta = 0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)
        val newEaseFactor = max(MIN_EASE_FACTOR, currentEaseFactor + delta)

        // 2. Calculate new repetitions and interval in days
        val (newRepetitions, newIntervalDays) = when {
            q < 3 -> {
                // Failed recall: reset repetitions, review tomorrow
                Pair(0, 1)
            }
            rating == ReviewRating.HARD -> {
                // Hard recall: advance interval cautiously by 1.2x
                val nextInterval = max(1, (currentIntervalDays * 1.2f).roundToInt())
                Pair(currentRepetitions + 1, nextInterval)
            }
            currentRepetitions == 0 -> {
                Pair(1, 1)
            }
            currentRepetitions == 1 -> {
                Pair(2, 6)
            }
            else -> {
                val multiplier = if (rating == ReviewRating.EASY) newEaseFactor * 1.3f else newEaseFactor
                val nextInterval = max(1, (currentIntervalDays * multiplier).roundToInt())
                Pair(currentRepetitions + 1, nextInterval)
            }
        }

        return Sm2Result(
            repetitionCount = newRepetitions,
            intervalDays = newIntervalDays,
            easeFactor = newEaseFactor,
            nextReviewEpochDay = todayEpochDay + newIntervalDays
        )
    }
}
