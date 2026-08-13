package com.family.shizi.engine

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.family.shizi.data.content.ContentLoader
import com.family.shizi.data.content.ContentPackage
import com.family.shizi.data.db.CharacterProgressEntity
import com.family.shizi.data.db.HintLevel
import com.family.shizi.data.db.InitialTeachingStep
import com.family.shizi.data.db.ItemKind
import com.family.shizi.data.db.LearningState
import com.family.shizi.data.db.OralCheckEntity
import com.family.shizi.data.db.OralStatus
import com.family.shizi.data.db.QuestionStatus
import com.family.shizi.data.db.ReviewStage
import com.family.shizi.data.db.SessionItemEntity
import com.family.shizi.data.db.ItemStatus
import com.family.shizi.data.db.ShiziDatabase
import com.family.shizi.data.repository.ShiziRepository
import com.family.shizi.data.settings.ShiziSettings
import com.family.shizi.data.settings.ShiziSettingsStore
import com.family.shizi.domain.core.IdProvider
import com.family.shizi.domain.core.RandomProvider
import com.family.shizi.domain.engine.MasteryStateEngine
import com.family.shizi.navigation.ShiziRoute
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class T06EngineInstrumentedTest {
    private lateinit var context: Context
    private lateinit var database: ShiziDatabase
    private lateinit var content: ContentPackage
    private val randomProvider = object : RandomProvider {
        override fun <T> shuffled(values: List<T>): List<T> = values.reversed()
    }
    private val idProvider = object : IdProvider {
        private var value = 0
        override fun newId(): String = "id_${value++}"
    }
    private val baseDate = LocalDate.parse("2026-07-26")
    private val baseInstant = Instant.parse("2026-07-26T08:00:00Z")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, ShiziDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        content = ContentLoader.load(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun at15DailyTaskPriorityIsDueReviewThenUnfinishedNewThenFreshNew(): Unit = runBlocking {
        val now = baseInstant
        content.learningOrder.forEach { database.characterProgressDao().insertIgnore(CharacterProgressEntity(it, updatedAt = now)) }
        database.characterProgressDao().upsert(
            CharacterProgressEntity(
                characterId = "char_kou",
                state = LearningState.REVIEWING,
                firstStartedAt = now.minusSeconds(86400),
                firstLearnDate = baseDate.minusDays(3),
                initialLessonCompleted = true,
                reviewStage = ReviewStage.D1,
                nextReviewDate = baseDate,
                updatedAt = now,
            ),
        )
        val repo = repository(now)
        val session = repo.getOrCreateDailySession(baseDate, ShiziSettings(dailyNewCharacterCount = 1, sessionLimitMinutes = 10), content, randomProvider, idProvider)
        val items = database.sessionItemDao().getForSession(session.id)
        assertEquals(listOf("char_kou"), items.filter { it.kind == ItemKind.REVIEW }.map { it.characterId })
        assertEquals("char_ren", items.last().characterId)
        assertEquals(2, items.size)
        assertEquals(session.id, repo.getOrCreateDailySession(baseDate, ShiziSettings(), content, randomProvider, idProvider).id)
    }

    @Test
    fun at12FirstLearningMovesThroughRealEntryPoints(): Unit = runBlocking {
        val now = baseInstant
        database.characterProgressDao().insertIgnore(CharacterProgressEntity("char_ren", updatedAt = now))
        repository(now).startInitialLearning("char_ren", now = now)
        assertEquals(LearningState.FIRST_LEARNING, database.characterProgressDao().getById("char_ren")?.state)
        completeToday(baseDate)
        val progress = database.characterProgressDao().getById("char_ren")
        assertEquals(LearningState.REVIEWING, progress?.state)
        assertEquals(baseDate.plusDays(1), progress?.nextReviewDate)
    }

    @Test
    fun dueReviewSessionStartsAtPracticeNotInitialTeaching(): Unit = runBlocking {
        val now = baseInstant
        content.learningOrder.forEach { database.characterProgressDao().insertIgnore(CharacterProgressEntity(it, updatedAt = now)) }
        database.characterProgressDao().upsert(
            CharacterProgressEntity(
                characterId = "char_ren",
                state = LearningState.REVIEWING,
                firstStartedAt = now.minusSeconds(5 * 86400),
                firstLearnDate = baseDate.minusDays(5),
                initialLessonCompleted = true,
                reviewStage = ReviewStage.D3,
                nextReviewDate = baseDate,
                updatedAt = now,
            ),
        )
        val repo = repository(now)
        val session = repo.getOrCreateDailySession(baseDate, ShiziSettings(), content, randomProvider, idProvider)
        assertEquals(ItemKind.REVIEW, database.sessionItemDao().getForSession(session.id).first().kind)
        assertEquals(ShiziRoute.Practice, repo.resolveNextRoute(session.id))
    }

    @Test
    fun unfinishedNewSessionStartsAtLearnBeforePractice(): Unit = runBlocking {
        val now = baseInstant
        content.learningOrder.forEach { database.characterProgressDao().insertIgnore(CharacterProgressEntity(it, updatedAt = now)) }
        database.characterProgressDao().upsert(
            CharacterProgressEntity(
                characterId = "char_ren",
                state = LearningState.FIRST_LEARNING,
                firstStartedAt = now.minusSeconds(60),
                firstLearnDate = baseDate,
                initialTeachingStep = InitialTeachingStep.B_SOUND_MEANING,
                initialLessonCompleted = false,
                updatedAt = now,
            ),
        )
        val repo = repository(now)
        val session = repo.getOrCreateDailySession(baseDate, ShiziSettings(), content, randomProvider, idProvider)
        assertEquals(ItemKind.NEW, database.sessionItemDao().getForSession(session.id).first().kind)
        assertEquals(ShiziRoute.Learn, repo.resolveNextRoute(session.id))
    }

    @Test
    fun at36FiveCharactersReachStableMasteredFromOneEmptyDatabase(): Unit = runBlocking {
        val trace = mutableListOf<String>()
        var date = baseDate
        repeat(40) { dayIndex ->
            val dayNow = baseInstant.plusSeconds(dayIndex * 86400L)
            val repo = repository(dayNow)
            val session = repo.getOrCreateDailySession(date, ShiziSettings(), content, randomProvider, idProvider)
            val items = database.sessionItemDao().getForSession(session.id)
            items.forEach { item ->
                repo.startInitialLearning(item.characterId, now = dayNow)
                answerAllQuestions(item.id, date, dayNow)
                val progress = database.characterProgressDao().getById(item.characterId)
                trace += "${date}|${item.characterId}|${item.kind}|${progress?.reviewStage}|${progress?.state}|next=${progress?.nextReviewDate}"
                if (item.reviewStageAtStart == ReviewStage.D14) {
                    repo.appendOralCheck(
                        OralCheckEntity(
                            id = "oral_${item.characterId}_$dayIndex",
                            characterId = item.characterId,
                            result = OralStatus.INDEPENDENT_PASS,
                            checkedAt = dayNow.plusSeconds(60),
                            localDate = date,
                            eligibleForStable = true,
                        ),
                        today = date,
                        now = dayNow.plusSeconds(60),
                    )
                }
            }
            if (content.learningOrder.all { database.characterProgressDao().getById(it)?.state == LearningState.STABLE_MASTERED }) {
                assertTrue(trace.isNotEmpty())
                return@runBlocking
            }
            date = date.plusDays(1)
        }
        val states = content.learningOrder.associateWith { database.characterProgressDao().getById(it)?.state }
        assertEquals(content.learningOrder.toSet(), states.filterValues { it == LearningState.STABLE_MASTERED }.keys)
    }

    @Test
    fun promptedCorrectDoesNotCountAsIndependentEvidence(): Unit = runBlocking {
        val now = baseInstant
        val repo = repository(now)
        val session = repo.getOrCreateDailySession(baseDate, ShiziSettings(), content, randomProvider, idProvider)
        val item = database.sessionItemDao().getForSession(session.id).single()
        repo.startInitialLearning(item.characterId, now = now)
        val question = database.questionInstanceDao().getForItem(item.id).first()
        repo.submitAnswer(
            questionInstanceId = question.id,
            selectedOptionId = question.correctOptionId,
            answeredAt = now,
            localDate = baseDate,
            responseTimeMs = 800,
            hintLevel = HintLevel.LIGHT,
        )
        val attempts = database.practiceAttemptDao().getForQuestion(question.id)
        assertEquals(false, attempts.single().independentCorrect)
    }

    @Test
    fun accidentalTapDoesNotCreateAttemptOrAdvanceAttemptNumber(): Unit = runBlocking {
        val now = baseInstant
        val repo = repository(now)
        val session = repo.getOrCreateDailySession(baseDate, ShiziSettings(), content, randomProvider, idProvider)
        val item = database.sessionItemDao().getForSession(session.id).single()
        repo.startInitialLearning(item.characterId, now = now)
        val question = database.questionInstanceDao().getForItem(item.id).first()
        val accidental = repo.submitAnswer(
            questionInstanceId = question.id,
            selectedOptionId = question.correctOptionId,
            answeredAt = now,
            localDate = baseDate,
            responseTimeMs = 120,
            isAccidental = true,
        )
        assertEquals(true, accidental.attempt.isAccidental)
        assertEquals(0, database.practiceAttemptDao().countForQuestion(question.id))
        repo.submitAnswer(question.id, question.correctOptionId, now.plusSeconds(1), baseDate, 900)
        val attempts = database.practiceAttemptDao().getForQuestion(question.id)
        assertEquals(1, attempts.single().attemptNumber)
    }

    @Test
    fun completedQuestionSubmissionIsIdempotentAndSessionCursorAdvances(): Unit = runBlocking {
        val now = baseInstant
        val repo = repository(now)
        val session = repo.getOrCreateDailySession(baseDate, ShiziSettings(), content, randomProvider, idProvider)
        val item = database.sessionItemDao().getForSession(session.id).single()
        repo.startInitialLearning(item.characterId, now = now)
        val question = database.questionInstanceDao().getForItem(item.id).first()
        repo.submitAnswer(question.id, question.correctOptionId, now, baseDate, 1000)
        repo.submitAnswer(question.id, question.correctOptionId, now.plusSeconds(1), baseDate, 1000)
        assertEquals(1, database.practiceAttemptDao().countForQuestion(question.id))
        val savedSession = database.learningSessionDao().getById(session.id)
        assertEquals(0, savedSession?.currentItemIndex)
        assertTrue(savedSession?.currentQuestionInstanceId != question.id)
    }

    @Test
    fun stableMasteredKeepsSingleFailureDateAndRollsBackOnSecondDate(): Unit = runBlocking {
        val now = baseInstant
        database.characterProgressDao().insertIgnore(
            CharacterProgressEntity(
                characterId = "char_ren",
                state = LearningState.STABLE_MASTERED,
                firstStartedAt = now.minusSeconds(20 * 86400),
                firstLearnDate = baseDate.minusDays(20),
                initialLessonCompleted = true,
                reviewStage = ReviewStage.D14,
                appDelayedCheckStatus = com.family.shizi.data.db.DelayedStatus.PASS,
                appDelayedCheckAt = now.minusSeconds(2 * 86400),
                currentOralStatus = OralStatus.INDEPENDENT_PASS,
                currentOralCheckAt = now.minusSeconds(86400),
                stableQualifiedAt = now.minusSeconds(86400),
                updatedAt = now,
            ),
        )
        createReviewFailure("review_one", "char_ren", baseDate, now)
        MasteryStateEngine(database).recalculate("char_ren", now, baseDate)
        assertEquals(LearningState.STABLE_MASTERED, database.characterProgressDao().getById("char_ren")?.state)
        createReviewFailure("review_two", "char_ren", baseDate.plusDays(1), now.plusSeconds(86400))
        MasteryStateEngine(database).recalculate("char_ren", now.plusSeconds(86400), baseDate.plusDays(1))
        val progress = database.characterProgressDao().getById("char_ren")
        assertEquals(LearningState.REVIEWING, progress?.state)
        assertTrue(progress?.stableRollbackAt != null)
    }

    @Test
    fun oralCheckRevisionToFailRollsBackStableMasteryWithAnchor(): Unit = runBlocking {
        val now = baseInstant
        database.characterProgressDao().insertIgnore(
            CharacterProgressEntity(
                characterId = "char_ren",
                state = LearningState.STABLE_MASTERED,
                firstStartedAt = now.minusSeconds(20 * 86400),
                firstLearnDate = baseDate.minusDays(20),
                initialLessonCompleted = true,
                reviewStage = ReviewStage.D14,
                appDelayedCheckStatus = com.family.shizi.data.db.DelayedStatus.PASS,
                appDelayedCheckAt = now.minusSeconds(2 * 86400),
                currentOralStatus = OralStatus.INDEPENDENT_PASS,
                currentOralCheckAt = now.minusSeconds(86400),
                stableQualifiedAt = now.minusSeconds(86400),
                updatedAt = now,
            ),
        )
        val repo = repository(now)
        repo.appendOralCheck(
            OralCheckEntity(
                id = "oral_pass",
                characterId = "char_ren",
                result = OralStatus.INDEPENDENT_PASS,
                checkedAt = now,
                localDate = baseDate,
                eligibleForStable = true,
            ),
            today = baseDate,
            now = now,
        )
        repo.appendOralCheck(
            OralCheckEntity(
                id = "oral_fail_revision",
                characterId = "char_ren",
                result = OralStatus.FAIL,
                checkedAt = now.plusSeconds(60),
                localDate = baseDate,
                eligibleForStable = false,
                revisionOf = "oral_pass",
            ),
            today = baseDate,
            now = now.plusSeconds(60),
        )
        val progress = database.characterProgressDao().getById("char_ren")
        val history = database.oralCheckDao().getHistory("char_ren")
        assertEquals(LearningState.REVIEWING, progress?.state)
        assertEquals(now.plusSeconds(60), progress?.stableRollbackAt)
        assertTrue(history.first { it.id == "oral_pass" }.isSuperseded)
    }

    private fun repository(now: Instant): ShiziRepository =
        ShiziRepository(
            database = database,
            settingsStore = ShiziSettingsStore(context),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    private suspend fun completeToday(date: LocalDate) {
        val repo = repository(baseInstant)
        val session = repo.getOrCreateDailySession(date, ShiziSettings(), content, randomProvider, idProvider)
        val item = database.sessionItemDao().getForSession(session.id).single()
        answerAllQuestions(item.id, date, baseInstant)
    }

    private suspend fun answerAllQuestions(itemId: String, date: LocalDate, now: Instant) {
        val repo = repository(now)
        database.questionInstanceDao().getForItem(itemId)
            .filter { it.status != QuestionStatus.COMPLETED }
            .forEachIndexed { index, question ->
                repo.submitAnswer(
                    questionInstanceId = question.id,
                    selectedOptionId = question.correctOptionId,
                    answeredAt = now.plusSeconds(index.toLong()),
                    localDate = date,
                    responseTimeMs = 1000,
                )
            }
    }

    private suspend fun createReviewFailure(idPrefix: String, characterId: String, date: LocalDate, now: Instant) {
        val session = com.family.shizi.data.db.LearningSessionEntity(
            id = "${idPrefix}_session",
            localDate = date,
            limitMinutesSnapshot = 10,
            contentVersion = "1.0.0",
        )
        database.learningSessionDao().insert(session)
        database.sessionItemDao().insertAll(
            listOf(
                SessionItemEntity(
                    id = "${idPrefix}_item",
                    sessionId = session.id,
                    characterId = characterId,
                    kind = ItemKind.REVIEW,
                    sequence = 0,
                    status = ItemStatus.COMPLETED,
                    reviewStageAtStart = ReviewStage.D14,
                    dueCheckPassed = false,
                    completedAt = now,
                    completedLocalDate = date,
                ),
            ),
        )
    }
}
