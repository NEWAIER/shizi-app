package com.family.shizi.engine

import com.family.shizi.data.db.ReviewStage
import com.family.shizi.domain.engine.ReviewScheduler
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewSchedulerTest {
    private val scheduler = ReviewScheduler(listOf(1, 3, 7, 14, 30, 60))

    @Test
    fun schedulesStrictReviewMilestones() {
        val d0 = LocalDate.parse("2026-07-26")
        assertEquals(ReviewStage.D1 to d0.plusDays(1), scheduler.next(ReviewStage.NONE, d0, d0))
        assertEquals(ReviewStage.D3 to d0.plusDays(3), scheduler.next(ReviewStage.D1, d0, d0.plusDays(1)))
        assertEquals(ReviewStage.D7 to d0.plusDays(7), scheduler.next(ReviewStage.D3, d0, d0.plusDays(3)))
        assertEquals(ReviewStage.D14 to d0.plusDays(14), scheduler.next(ReviewStage.D7, d0, d0.plusDays(7)))
        assertEquals(ReviewStage.D30 to d0.plusDays(30), scheduler.next(ReviewStage.D14, d0, d0.plusDays(14)))
        assertEquals(ReviewStage.D60 to d0.plusDays(60), scheduler.next(ReviewStage.D30, d0, d0.plusDays(30)))
        assertNull(scheduler.next(ReviewStage.D60, d0, d0.plusDays(60)))
    }

    @Test
    fun lateCompletionUsesActualNextDayWhenLaterThanAnchor() {
        val d0 = LocalDate.parse("2026-07-26")
        assertEquals(ReviewStage.D3 to d0.plusDays(6), scheduler.next(ReviewStage.D1, d0, d0.plusDays(5)))
    }
}
