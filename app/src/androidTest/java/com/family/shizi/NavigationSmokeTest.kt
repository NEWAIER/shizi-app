package com.family.shizi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.test.advanceEventTime
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeCanBeDisplayedWithoutDebugRoutes() {
        composeRule.onNodeWithTag("page_home").assertIsDisplayed()
    }

    @Test
    fun childCanReachLearningPageWithoutFirstParentSetup() {
        val app = composeRule.activity.application as ShiziApplication
        runBlocking {
            checkNotNull(app.repository).clearLearningDataAndResetSettingsSafely()
        }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("home_primary").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_primary").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag("page_learn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("page_learn").assertIsDisplayed()
    }
}
