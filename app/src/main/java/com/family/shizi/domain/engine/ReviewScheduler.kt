package com.family.shizi.domain.engine

import com.family.shizi.data.db.ReviewStage
import java.time.LocalDate

class ReviewScheduler(private val offsetsDays: List<Int>) {
    fun next(stage: ReviewStage, firstLearnDate: LocalDate, completedDate: LocalDate): Pair<ReviewStage, LocalDate>? {
        val nextStage = when (stage) {
            ReviewStage.NONE -> ReviewStage.D1
            ReviewStage.D1 -> ReviewStage.D3
            ReviewStage.D3 -> ReviewStage.D7
            ReviewStage.D7 -> ReviewStage.D14
            ReviewStage.D14 -> ReviewStage.D30
            ReviewStage.D30 -> ReviewStage.D60
            ReviewStage.D60 -> null
        } ?: return null
        val anchorOffset = when (nextStage) {
            ReviewStage.D1 -> offsetsDays.getOrElse(0) { 1 }
            ReviewStage.D3 -> offsetsDays.getOrElse(1) { 3 }
            ReviewStage.D7 -> offsetsDays.getOrElse(2) { 7 }
            ReviewStage.D14 -> offsetsDays.getOrElse(3) { 14 }
            ReviewStage.D30 -> offsetsDays.getOrElse(4) { 30 }
            ReviewStage.D60 -> offsetsDays.getOrElse(5) { 60 }
            ReviewStage.NONE -> 0
        }
        val anchoredDate = firstLearnDate.plusDays(anchorOffset.toLong())
        val noEarlierThanTomorrow = completedDate.plusDays(1)
        return nextStage to maxOf(anchoredDate, noEarlierThanTomorrow)
    }

    fun nextAfterLate(stage: ReviewStage, firstLearnDate: LocalDate, completedDate: LocalDate): Pair<ReviewStage, LocalDate>? =
        next(stage, firstLearnDate, completedDate)
}
