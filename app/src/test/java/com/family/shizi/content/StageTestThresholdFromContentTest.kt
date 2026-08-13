package com.family.shizi.content

import com.family.shizi.data.content.CourseConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StageTestThresholdFromContentTest {
    @Test fun fiveCharacterCourseDoesNotAllowFourCharacters() {
        val threshold = CourseConfig(stageTestThreshold = 5).stageTestThreshold
        assertFalse(4 >= threshold)
        assertTrue(5 >= threshold)
    }
}
