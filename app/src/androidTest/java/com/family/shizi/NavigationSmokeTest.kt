package com.family.shizi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.test.advanceEventTime
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.ui.learned.LearnedCharacterCard
import com.family.shizi.ui.practice.TextOptionGrid

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

    @Test
    fun shapeRecognitionTextOptionsAreVisibleAndCorrectOptionIsClickable() {
        val content = ContentLoader.load(composeRule.activity)
        val options = content.optionCatalog.filter { it.id in listOf("text_char_ren", "text_char_kou", "text_char_shan") }
        var selected: String? = null
        composeRule.setContent {
            TextOptionGrid(options = options, teachingCorrectId = null, disabled = false) { selected = it }
        }
        composeRule.onNodeWithTag("practice_text_option_text_char_ren").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("practice_text_option_text_char_kou").assertIsDisplayed()
        composeRule.onNodeWithTag("practice_text_option_text_char_shan").assertIsDisplayed()
        composeRule.runOnIdle { check(selected == "text_char_ren") }
    }

    @Test
    fun learnedCardShowsChildCharacterInsteadOfInternalId() {
        val content = ContentLoader.load(composeRule.activity)
        val character = checkNotNull(content.characters.firstOrNull { it.id == "char_ren" })
        val progress = com.family.shizi.data.db.CharacterProgressEntity(
            characterId = "char_ren",
            initialLessonCompleted = true,
            updatedAt = java.time.Instant.EPOCH,
        )
        composeRule.setContent {
            LearnedCharacterCard(progress, character, onPlay = {})
        }
        composeRule.onNodeWithTag("learned_character_title").assertIsDisplayed()
        composeRule.onNodeWithText("人").assertIsDisplayed()
        composeRule.runOnIdle {
            check(composeRule.onAllNodesWithText("char_ren").fetchSemanticsNodes().isEmpty())
        }
    }
}
