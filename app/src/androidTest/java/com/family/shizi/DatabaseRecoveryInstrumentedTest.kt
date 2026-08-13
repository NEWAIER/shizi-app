package com.family.shizi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.family.shizi.data.db.ShiziDatabase
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseRecoveryInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun failedOpenDoesNotDeleteExistingDatabaseFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "p0_open_failure_preserves.db"
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, ShiziDatabase::class.java, name).build()
        db.close()
        val file = context.getDatabasePath(name)
        assertTrue(file.exists())
        ShiziDatabase.clearInstance()
        val result = ShiziDatabase.getInstance(context, databaseName = name) { error("simulated open failure") }
        assertTrue(result == null)
        assertTrue("failed open must preserve original database", file.exists())
        context.deleteDatabase(name)
    }

    @Test
    fun recoveryScreenCanBeDisplayedWithoutClearAction() {
        composeRule.setContent { DatabaseRecoveryScreen() }
        composeRule.onNodeWithTag("db_recovery_title").assertIsDisplayed()
        composeRule.onNodeWithTag("db_recovery_clear").assertIsDisplayed()
    }
}
