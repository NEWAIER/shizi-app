package com.family.shizi.engine

import com.family.shizi.domain.engine.HonorLevels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HonorLevelsTest {
    @Test fun levelOneAtZeroStars() {
        val p = HonorLevels.progressFor(0)
        assertEquals(1, p.level)
        assertEquals(0, p.currentLevelStart)
        assertEquals(50, p.nextLevelThreshold)
        assertEquals(0f, p.progress, 0.0001f)
    }

    @Test fun stillLevelOneBelowFifty() {
        val p = HonorLevels.progressFor(49)
        assertEquals(1, p.level)
        assertEquals(0, p.currentLevelStart)
        assertEquals(50, p.nextLevelThreshold)
        assertTrue(p.progress > 0.9f)
        assertEquals(1, HonorLevels.starsToNext(49))
    }

    @Test fun levelTwoAtFifty() {
        val p = HonorLevels.progressFor(50)
        assertEquals(2, p.level)
        assertEquals(50, p.currentLevelStart)
        assertEquals(120, p.nextLevelThreshold)
        assertEquals(0f, p.progress, 0.0001f)
    }

    @Test fun levelTwoAt119() {
        val p = HonorLevels.progressFor(119)
        assertEquals(2, p.level)
        assertEquals(50, p.currentLevelStart)
        assertEquals(120, p.nextLevelThreshold)
        // (119-50)/(120-50) = 69/70
        assertEquals(69f / 70f, p.progress, 0.0001f)
    }

    @Test fun levelThreeAt120() {
        val p = HonorLevels.progressFor(120)
        assertEquals(3, p.level)
        assertEquals(120, p.currentLevelStart)
        assertEquals(250, p.nextLevelThreshold)
        assertEquals(0f, p.progress, 0.0001f)
    }

    @Test fun levelThreeAt249() {
        val p = HonorLevels.progressFor(249)
        assertEquals(3, p.level)
        assertEquals(120, p.currentLevelStart)
        assertEquals(250, p.nextLevelThreshold)
        // (249-120)/(250-120) = 129/130
        assertEquals(129f / 130f, p.progress, 0.0001f)
    }

    @Test fun levelFourAt250() {
        val p = HonorLevels.progressFor(250)
        assertEquals(4, p.level)
        assertEquals(250, p.currentLevelStart)
        assertEquals(450, p.nextLevelThreshold)
        assertEquals(0f, p.progress, 0.0001f)
    }

    @Test fun levelFiveAt699() {
        val p = HonorLevels.progressFor(699)
        assertEquals(5, p.level)
        assertEquals(450, p.currentLevelStart)
        assertEquals(700, p.nextLevelThreshold)
        // (699-450)/(700-450) = 249/250
        assertEquals(249f / 250f, p.progress, 0.0001f)
        assertEquals(1, HonorLevels.starsToNext(699))
    }

    @Test fun maxLevelAt700() {
        val p = HonorLevels.progressFor(700)
        assertEquals(6, p.level)
        assertEquals(700, p.currentLevelStart)
        assertEquals(700, p.nextLevelThreshold)
        assertEquals(1f, p.progress, 0.0001f)
        assertEquals(0, HonorLevels.starsToNext(700))
    }

    @Test fun beyondMaxLevelStaysAtMaxWithFullProgress() {
        val p = HonorLevels.progressFor(1200)
        assertEquals(6, p.level)
        assertEquals(700, p.currentLevelStart)
        assertEquals(700, p.nextLevelThreshold)
        assertEquals(1f, p.progress, 0.0001f)
        assertEquals(0, HonorLevels.starsToNext(1200))
    }
}
