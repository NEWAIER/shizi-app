package com.family.shizi.engine

import com.family.shizi.domain.engine.BadgeCatalog
import com.family.shizi.domain.engine.BadgeCounts
import com.family.shizi.domain.engine.BadgeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeCatalogTest {
    @Test fun learnedCountBadgesUnlockByLearnedCharacters() {
        val zero = BadgeCounts(learnedCount = 0, learningDays = 0)
        val one = BadgeCounts(learnedCount = 1, learningDays = 0)
        val four = BadgeCounts(learnedCount = 4, learningDays = 0)
        val five = BadgeCounts(learnedCount = 5, learningDays = 0)
        val fifty = BadgeCounts(learnedCount = 50, learningDays = 0)

        val learned = BadgeCatalog.byType(BadgeType.LEARNED_COUNT)
        assertEquals(7, learned.size)
        assertFalse(BadgeCatalog.isUnlocked(learned.first(), zero))
        assertTrue(BadgeCatalog.isUnlocked(learned.first(), one)) // 1 字
        assertTrue(BadgeCatalog.isUnlocked(learned.first(), five))
        assertTrue(BadgeCatalog.isUnlocked(learned.last(), fifty)) // 50 字
        assertFalse(BadgeCatalog.isUnlocked(learned[1], four)) // 5 字徽章需要 5 字
        assertTrue(BadgeCatalog.isUnlocked(learned[1], five))
    }

    @Test fun learningDaysBadgesUnlockByDays() {
        val zero = BadgeCounts(learnedCount = 0, learningDays = 0)
        val days14 = BadgeCounts(learnedCount = 0, learningDays = 14)

        val days = BadgeCatalog.byType(BadgeType.LEARNING_DAYS)
        assertEquals(4, days.size)
        assertFalse(BadgeCatalog.isUnlocked(days.first(), zero))
        assertTrue(BadgeCatalog.isUnlocked(days.first(), days14))
        assertTrue(BadgeCatalog.isUnlocked(days.last(), days14)) // 14 天
        assertFalse(BadgeCatalog.isUnlocked(days[1], BadgeCounts(learningDays = 2))) // 3 天徽章
    }

    @Test fun reviewBadgesNeverUnlockWithoutReliableCount() {
        val review = BadgeCatalog.byType(BadgeType.REVIEW_COUNT)
        assertTrue(review.isNotEmpty())
        review.forEach { badge ->
            assertFalse(badge.hasReliableCount)
            // 即使计数给出，无可靠计数源也不解锁
            assertFalse(BadgeCatalog.isUnlocked(badge, BadgeCounts(reviewCount = 100)))
        }
        assertTrue(BadgeCatalog.awaitingIds().containsAll(review.map { it.id }))
    }

    @Test fun challengeBadgesNeverUnlockWithoutReliableCount() {
        val challenge = BadgeCatalog.byType(BadgeType.CHALLENGE_COUNT)
        challenge.forEach { badge ->
            assertFalse(BadgeCatalog.isUnlocked(badge, BadgeCounts(challengeCount = 100)))
        }
    }

    @Test fun collectionBadgesNeverUnlockWithoutReliableCount() {
        val collection = BadgeCatalog.byType(BadgeType.COLLECTION_COUNT)
        collection.forEach { badge ->
            assertFalse(BadgeCatalog.isUnlocked(badge, BadgeCounts(collectionCount = 100)))
        }
    }

    @Test fun unlockedIdsOnlyContainsReallyUnlockedBadges() {
        val counts = BadgeCounts(learnedCount = 10, learningDays = 3)
        val unlocked = BadgeCatalog.unlockedIds(counts)
        // 字数 1/5/10 + 天数 1/3 解锁；其余等待点亮
        assertTrue(unlocked.contains("learned_1"))
        assertTrue(unlocked.contains("learned_5"))
        assertTrue(unlocked.contains("learned_10"))
        assertFalse(unlocked.contains("learned_20"))
        assertTrue(unlocked.contains("days_1"))
        assertTrue(unlocked.contains("days_3"))
        assertFalse(unlocked.contains("days_7"))
        assertFalse(unlocked.contains("review_1"))
        assertFalse(unlocked.contains("challenge_1"))
        assertFalse(unlocked.contains("collect_10"))
        // 等待点亮的徽章绝不会出现在已解锁列表
        BadgeCatalog.awaitingIds().forEach { assertFalse("$it must not unlock", unlocked.contains(it)) }
    }
}
