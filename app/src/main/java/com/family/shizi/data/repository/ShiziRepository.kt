package com.family.shizi.data.repository

import com.family.shizi.data.content.ContentPackage
import com.family.shizi.data.db.AppErrorLogEntity
import com.family.shizi.data.db.CharacterProgressEntity
import com.family.shizi.data.db.DelayedStatus
import com.family.shizi.data.db.EarlyEndReason
import com.family.shizi.data.db.FinalOutcome
import com.family.shizi.data.db.HintLevel
import com.family.shizi.data.db.InitialTeachingStep
import com.family.shizi.data.db.ItemKind
import com.family.shizi.data.db.ItemStatus
import com.family.shizi.data.db.LearningSessionEntity
import com.family.shizi.data.db.LearningState
import com.family.shizi.data.db.OralCheckEntity
import com.family.shizi.data.db.OralStatus
import com.family.shizi.data.db.PauseReason
import com.family.shizi.data.db.PracticeAttemptEntity
import com.family.shizi.data.db.QuestionInstanceEntity
import com.family.shizi.data.db.QuestionPurpose
import com.family.shizi.data.db.QuestionStatus
import com.family.shizi.data.db.ReviewStage
import com.family.shizi.data.db.SessionItemEntity
import com.family.shizi.data.db.SessionStatus
import com.family.shizi.data.db.ShiziDatabase
import com.family.shizi.data.settings.ShiziSettings
import com.family.shizi.data.settings.ShiziSettingsStore
import com.family.shizi.domain.core.IdProvider
import com.family.shizi.domain.core.RandomProvider
import com.family.shizi.domain.diagnostics.DiagnosticsExporter
import com.family.shizi.domain.engine.MasteryStateEngine
import com.family.shizi.domain.engine.ReviewScheduler
import com.family.shizi.domain.engine.StageTestBatches
import com.family.shizi.navigation.ShiziRoute
import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ShiziRepository(
    private val database: ShiziDatabase,
    private val settingsStore: ShiziSettingsStore,
    private val reviewScheduler: ReviewScheduler = ReviewScheduler(listOf(1, 3, 7, 14, 30, 60)),
    private val masteryStateEngine: Lazy<MasteryStateEngine> = lazy { MasteryStateEngine(database) },
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    val settings: Flow<ShiziSettings> = settingsStore.settings

    data class CurrentItemSnapshot(
        val session: LearningSessionEntity,
        val item: SessionItemEntity,
        val progress: CharacterProgressEntity?,
    )

    data class CurrentPracticeSnapshot(
        val session: LearningSessionEntity?,
        val item: SessionItemEntity?,
        val question: QuestionInstanceEntity?,
    )

    data class StageTestSummary(
        val localDate: LocalDate,
        val totalQuestions: Int,
        val firstTryCorrectCount: Int,
        val reinforceCharacterIds: List<String>,
    )

    data class ParentCharacterReport(
        val characterId: String,
        val state: String,
        val firstLearnDate: String,
        val validDateCount: Int,
        val questionTypeCount: Int,
        val evidenceCount: Int,
        val nextReviewDate: String,
        val appDelayedStatus: String,
        val oralStatus: String,
        val lastStudiedAt: String,
        val lastReviewResult: String,
        val temporaryQualifiedAt: String,
        val stableQualifiedAt: String,
        val stableRollbackAt: String,
        val rollbackReason: String,
        val missingEvidence: String,
        val errorCount: Int,
    ) {
        fun asLine(): String =
            "$characterId | 状态=$state | 首学=$firstLearnDate | 最近学习=$lastStudiedAt | " +
                "有效日期=$validDateCount | 题型=$questionTypeCount | 证据=$evidenceCount | " +
                "最近复习=$lastReviewResult | 下次=$nextReviewDate | " +
                "暂时掌握=$temporaryQualifiedAt | 稳定掌握=$stableQualifiedAt | " +
                "回退=$stableRollbackAt($rollbackReason) | 缺少=$missingEvidence | " +
                "错误次数=$errorCount | APP=$appDelayedStatus | 口头=$oralStatus"
    }

    // ========== READ-ONLY QUERIES (still useful; read does not need to be forced through Repo but can be) ==========

    suspend fun getCharacterProgress(): List<CharacterProgressEntity> =
        database.characterProgressDao().getAll()

    suspend fun getLatestStageTestSummary(): StageTestSummary? {
        val session = database.learningSessionDao().getLatestCompletedStageTest() ?: return null
        val items = database.sessionItemDao().getForSession(session.id)
        val questions = items.flatMap { database.questionInstanceDao().getForItem(it.id) }
        if (questions.isEmpty()) return null
        val attempts = database.practiceAttemptDao().getForQuestions(questions.map { it.id })
            .filter { !it.isAccidental }
            .groupBy { it.questionInstanceId }
        val firstTryCorrect = questions.count { question ->
            attempts[question.id]?.minByOrNull { it.attemptNumber }?.independentCorrect == true
        }
        val itemByQuestion = questions.associate { question -> question.id to items.first { it.id == question.sessionItemId } }
        val reinforce = questions.mapNotNull { question ->
            val first = attempts[question.id]?.minByOrNull { it.attemptNumber }
            itemByQuestion[question.id]?.characterId?.takeIf { first?.independentCorrect != true }
        }.distinct()
        return StageTestSummary(session.localDate, questions.size, firstTryCorrect, reinforce)
    }

    suspend fun getCompletedLearningDayCount(): Int =
        database.learningSessionDao().countCompletedLearningDays()

    suspend fun getUsableSession(localDate: LocalDate): LearningSessionEntity? =
        database.learningSessionDao().getUsableByDate(localDate)

    suspend fun getLatestOpenOrTodaySession(localDate: LocalDate): LearningSessionEntity? =
        database.learningSessionDao().getMostRecentForDate(localDate)
            ?: database.learningSessionDao().getMostRecentlyActive()

    suspend fun getCurrentItemSnapshot(): CurrentItemSnapshot? {
        val session = database.learningSessionDao().getMostRecentlyActive() ?: return null
        val item = database.sessionItemDao().getForSession(session.id).getOrNull(session.currentItemIndex) ?: return null
        val progress = database.characterProgressDao().getById(item.characterId)
        return CurrentItemSnapshot(session, item, progress)
    }

    suspend fun getCurrentPracticeSnapshot(): CurrentPracticeSnapshot {
        val session = database.learningSessionDao().getMostRecentlyActive()
        val item = session?.let { database.sessionItemDao().getForSession(it.id).getOrNull(it.currentItemIndex) }
        val pending = item?.let {
            database.questionInstanceDao().getForItem(it.id).firstOrNull { q -> q.status != QuestionStatus.COMPLETED }
        }
        return CurrentPracticeSnapshot(session, item, pending)
    }

    suspend fun getSessionById(sessionId: String): LearningSessionEntity? =
        database.learningSessionDao().getById(sessionId)

    suspend fun getItemsForSession(sessionId: String): List<SessionItemEntity> =
        database.sessionItemDao().getForSession(sessionId)

    suspend fun getQuestionsForItem(itemId: String): List<QuestionInstanceEntity> =
        database.questionInstanceDao().getForItem(itemId)

    suspend fun getProgress(characterId: String): CharacterProgressEntity? =
        database.characterProgressDao().getById(characterId)

    suspend fun getOralHistory(characterId: String): List<OralCheckEntity> =
        database.oralCheckDao().getHistory(characterId)

    suspend fun buildParentReports(): List<ParentCharacterReport> =
        database.characterProgressDao().getAll().map { progress ->
            val attempts = database.practiceAttemptDao().getForCharacter(progress.characterId)
            val questionIds = attempts.map { attempt -> attempt.questionInstanceId }.toSet()
            val questions = questionIds.mapNotNull { id -> database.questionInstanceDao().getById(id) }
            val dates = attempts.filter { attempt -> attempt.independentCorrect && !attempt.isAccidental }
                .map { attempt -> attempt.localDate }
                .toSet()
            val wrongAttempts = attempts.filter { !it.isCorrect && !it.isAccidental }
            val lastStudyAttempt = attempts.maxByOrNull { it.answeredAt }
            val lastReviewItem = database.sessionItemDao().getCompletedReviews(progress.characterId)
                .maxByOrNull { it.completedAt ?: Instant.EPOCH }
            // Missing evidence calculation
            val missing = mutableListOf<String>()
            if (dates.size < 2) missing.add("有效日期不足2天")
            if (questions.map { it.questionType }.toSet().size < 3) missing.add("题型不足3种")
            if (questions.map { it.evidenceCategory }.toSet().size < 3) missing.add("证据类别不足3种")
            if (progress.state == LearningState.REVIEWING && progress.appDelayedCheckStatus != DelayedStatus.PASS) missing.add("D14延迟检测未通过")
            if (progress.currentOralStatus != OralStatus.INDEPENDENT_PASS) missing.add("口头抽检未通过")
            if (missing.isEmpty()) missing.add("无")
            // Rollback reason
            val rollbackReason = when {
                progress.stableRollbackAt != null -> "连续2次不同日期失败或口头未通过"
                progress.temporaryRollbackAt != null -> "暂时掌握资格丢失"
                else -> "无"
            }
            ParentCharacterReport(
                characterId = progress.characterId,
                state = progress.state.name,
                firstLearnDate = progress.firstLearnDate?.toString() ?: "无",
                validDateCount = dates.size,
                questionTypeCount = questions.map { it.questionType }.toSet().size,
                evidenceCount = questions.map { it.evidenceCategory }.toSet().size,
                nextReviewDate = progress.nextReviewDate?.toString() ?: "无",
                appDelayedStatus = progress.appDelayedCheckStatus.name,
                oralStatus = progress.currentOralStatus.name,
                lastStudiedAt = lastStudyAttempt?.answeredAt?.toString() ?: "无",
                lastReviewResult = lastReviewItem?.dueCheckPassed?.let { if (it) "通过" else "未通过" } ?: "无",
                temporaryQualifiedAt = progress.temporaryQualifiedAt?.toString() ?: "无",
                stableQualifiedAt = progress.stableQualifiedAt?.toString() ?: "无",
                stableRollbackAt = progress.stableRollbackAt?.toString() ?: "无",
                rollbackReason = rollbackReason,
                missingEvidence = missing.joinToString(","),
                errorCount = wrongAttempts.size,
            )
        }

    suspend fun countAppErrors(): Int = database.appErrorLogDao().countAll()
    suspend fun countOralChecks(): Int = database.oralCheckDao().countAll()

    suspend fun exportDiagnostics(context: android.content.Context, appVersion: String, exportedAt: Instant): String {
        val settings = settings.first()
        val exporter = DiagnosticsExporter(database)
        return exporter.exportJson(context = context, settings = settings, appVersion = appVersion, exportedAt = exportedAt)
    }

    // ========== SEED / BOOTSTRAP ==========

    suspend fun seedCharacterProgressIfMissing(characterIds: List<String>) {
        database.withTransaction {
            val now = clock.instant()
            characterIds.forEach { characterId ->
                database.characterProgressDao().insertIgnore(
                    CharacterProgressEntity(characterId = characterId, updatedAt = now),
                )
            }
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    // ========== DAILY COURSE GENERATION (was DailyTaskGenerator) ==========

    suspend fun getOrCreateDailySession(
        date: LocalDate,
        settings: ShiziSettings,
        content: ContentPackage,
        randomProvider: RandomProvider,
        idProvider: IdProvider,
    ): LearningSessionEntity =
        database.withTransaction {
            database.learningSessionDao().getUsableByDate(date)?.let { return@withTransaction it }
            closeOpenSessionsBeforeInternal(date)
            database.learningSessionDao().getUsableByDate(date)?.let { return@withTransaction it }

            val now = clock.instant()
            content.learningOrder.forEach { characterId ->
                database.characterProgressDao().insertIgnore(
                    CharacterProgressEntity(characterId = characterId, updatedAt = now),
                )
            }
            val orderRank = content.learningOrder.withIndex().associate { it.value to it.index }
            val due = database.characterProgressDao().getDueForReview(date, 3)
                .sortedWith(
                    compareBy(
                        { it.nextReviewDate },
                        { !it.isErrorProne },
                        { orderRank[it.characterId] ?: Int.MAX_VALUE },
                    ),
                )
            val unfinishedNew = database.sessionItemDao().getUnfinishedNewItems()
                .distinctBy { it.characterId }
                .filter { due.none { review -> review.characterId == it.characterId } }
            // Due reviews keep priority, then honor the family-selected new-character count.
            // The setting is read for every today's-session creation, so a parent change applies immediately today.
            val dailyNewLimit = settings.dailyNewCharacterCount.coerceIn(1, 10).coerceAtMost(content.learningOrder.size)
            val existingProgress = database.characterProgressDao().getAll().associateBy { it.characterId }
            val unfinishedNewIds = unfinishedNew.map { it.characterId }.toSet()
            val inProgressNew = content.learningOrder
                .mapNotNull { existingProgress[it] }
                .filter {
                    it.firstStartedAt != null &&
                        !it.initialLessonCompleted &&
                        it.characterId !in unfinishedNewIds &&
                        due.none { review -> review.characterId == it.characterId }
                }
            val remainingNewCount = (dailyNewLimit - unfinishedNew.size - inProgressNew.size).coerceAtLeast(0)
            val fresh = content.learningOrder
                .mapNotNull { existingProgress[it] }
                .filter { it.firstStartedAt == null }
                .take(remainingNewCount)

            val session = LearningSessionEntity(
                id = idProvider.newId(),
                localDate = date,
                plannedNewCount = unfinishedNew.size + inProgressNew.size + fresh.size,
                plannedReviewCount = due.size,
                limitMinutesSnapshot = settings.sessionLimitMinutes,
                contentVersion = content.contentVersion,
            )
            database.learningSessionDao().insert(session)

            var sequence = 0
            due.forEach { progress ->
                createItemWithQuestionsInternal(session.id, progress.characterId, ItemKind.REVIEW, sequence++, progress.reviewStage, content, randomProvider, idProvider)
            }
            unfinishedNew.forEach { unfinished ->
                createItemWithQuestionsInternal(session.id, unfinished.characterId, ItemKind.NEW, sequence++, unfinished.reviewStageAtStart, content, randomProvider, idProvider)
            }
            inProgressNew.forEach { progress ->
                createItemWithQuestionsInternal(session.id, progress.characterId, ItemKind.NEW, sequence++, ReviewStage.NONE, content, randomProvider, idProvider)
            }
            fresh.forEach { progress ->
                createItemWithQuestionsInternal(session.id, progress.characterId, ItemKind.NEW, sequence++, ReviewStage.NONE, content, randomProvider, idProvider)
            }
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
            session
        }

    private suspend fun createItemWithQuestionsInternal(
        sessionId: String,
        characterId: String,
        kind: ItemKind,
        sequence: Int,
        reviewStage: ReviewStage,
        content: ContentPackage,
        randomProvider: RandomProvider,
        idProvider: IdProvider,
    ) {
        val item = SessionItemEntity(
            id = idProvider.newId(),
            sessionId = sessionId,
            characterId = characterId,
            kind = kind,
            sequence = sequence,
            reviewStageAtStart = reviewStage,
        )
        database.sessionItemDao().insertAll(listOf(item))
        val character = requireNotNull(content.characters.firstOrNull { it.id == characterId })
        val learnedCount = database.characterProgressDao().countInitialLessonsCompleted()
        val seedTypes = if (kind == ItemKind.NEW) {
            listOf(
                com.family.shizi.data.content.QuestionType.CHARACTER_CHOOSE_IMAGE,
                com.family.shizi.data.content.QuestionType.LISTEN_CHOOSE_CHARACTER,
                com.family.shizi.data.content.QuestionType.CHARACTER_CHOOSE_AUDIO,
            )
        } else {
            when (reviewStage) {
                ReviewStage.D1 -> listOf(com.family.shizi.data.content.QuestionType.SHAPE_RECOGNITION, com.family.shizi.data.content.QuestionType.LISTEN_CHOOSE_CHARACTER)
                ReviewStage.D3 -> listOf(com.family.shizi.data.content.QuestionType.CHARACTER_CHOOSE_IMAGE, com.family.shizi.data.content.QuestionType.CHARACTER_CHOOSE_AUDIO)
                ReviewStage.D7 -> listOf(com.family.shizi.data.content.QuestionType.SHAPE_RECOGNITION, com.family.shizi.data.content.QuestionType.CHARACTER_CHOOSE_AUDIO)
                ReviewStage.D14 -> listOf(com.family.shizi.data.content.QuestionType.LISTEN_CHOOSE_CHARACTER, com.family.shizi.data.content.QuestionType.CHARACTER_CHOOSE_AUDIO)
                else -> character.questionSeeds.map { it.type }.distinct().take(2)
            }
        }
        val preferredSeeds = seedTypes.mapNotNull { type ->
            character.questionSeeds
                .filter { it.type == type && it.minLearnedCount <= learnedCount }
                .minByOrNull { it.id }
        }
        val seeds = if (kind == ItemKind.REVIEW && preferredSeeds.size < 2) {
            (preferredSeeds + character.questionSeeds
                .filter { seed -> seed.minLearnedCount <= learnedCount && preferredSeeds.none { it.type == seed.type } }
                .distinctBy { it.type }
                .sortedBy { it.id })
                .take(2)
        } else {
            preferredSeeds
        }
        database.questionInstanceDao().insertAll(
            seeds.map { seed ->
                QuestionInstanceEntity(
                    id = idProvider.newId(),
                    sessionItemId = item.id,
                    questionSeedId = seed.id,
                    questionType = seed.type.name,
                    evidenceCategory = seed.evidenceCategory.name,
                    optionIdsJson = Json.encodeToString(randomProvider.shuffled(seed.optionIds)),
                    correctOptionId = seed.correctOptionId,
                    purpose = if (kind == ItemKind.NEW) QuestionPurpose.INITIAL else QuestionPurpose.REVIEW,
                    isMilestoneQuestion = kind == ItemKind.REVIEW,
                )
            },
        )
    }

    /**
     * 树洞测试关卡：只使用该批次（每 [StageTestBatches.BATCH_SIZE] 个字一批）中已学完的字，
     * 整批学完后树洞才解锁。测试保留真实答题记录，但刻意与间隔复习解耦：
     * 通过测试不会跳过到期复习，也不会单独提升掌握等级。
     */
    suspend fun createStageTestSession(
        content: ContentPackage,
        randomProvider: RandomProvider,
        idProvider: IdProvider,
        batchIndex: Int = 0,
        now: Instant = clock.instant(),
    ): LearningSessionEntity = database.withTransaction {
        val batchIds = StageTestBatches.characterIdsOf(content.learningOrder, batchIndex)
        require(batchIds.isNotEmpty()) { "测试批次超出课程范围" }
        val learned = batchIds.filter { characterId ->
            database.characterProgressDao().getById(characterId)?.initialLessonCompleted == true
        }
        require(learned.size == batchIds.size) { "本关还有 ${batchIds.size - learned.size} 个字没学完" }
        val session = LearningSessionEntity(
            id = idProvider.newId(),
            localDate = LocalDate.now(clock.zone),
            status = SessionStatus.ACTIVE,
            startedAt = now,
            activeSegmentStartedAt = now,
            plannedNewCount = 0,
            plannedReviewCount = 0,
            limitMinutesSnapshot = 10,
            contentVersion = content.contentVersion,
        )
        database.learningSessionDao().insert(session)
        learned.forEachIndexed { index, characterId ->
            val character = requireNotNull(content.characters.firstOrNull { it.id == characterId })
            val seed = character.questionSeeds
                .filter { it.minLearnedCount <= learned.size }
                .sortedBy { it.id }
                .let { candidates -> candidates[index % candidates.size] }
            val item = SessionItemEntity(
                id = idProvider.newId(),
                sessionId = session.id,
                characterId = characterId,
                kind = ItemKind.TEST,
                sequence = index,
            )
            database.sessionItemDao().insertAll(listOf(item))
            database.questionInstanceDao().insertAll(listOf(
                QuestionInstanceEntity(
                    id = idProvider.newId(),
                    sessionItemId = item.id,
                    questionSeedId = seed.id,
                    questionType = seed.type.name,
                    evidenceCategory = seed.evidenceCategory.name,
                    optionIdsJson = Json.encodeToString(randomProvider.shuffled(seed.optionIds)),
                    correctOptionId = seed.correctOptionId,
                    purpose = QuestionPurpose.EVIDENCE,
                ),
            ))
        }
        settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        session
    }

    // ========== SESSION LIFECYCLE ==========

    suspend fun createSessionIfAbsent(
        localDate: LocalDate,
        plannedNewCount: Int,
        plannedReviewCount: Int,
        limitMinutesSnapshot: Int,
        contentVersion: String,
        id: String = UUID.randomUUID().toString(),
    ): LearningSessionEntity =
        database.withTransaction {
            closeOpenSessionsBeforeInternal(localDate)
            val now = clock.instant()
            val session =
                database.learningSessionDao().getUsableByDate(localDate)
                    ?: LearningSessionEntity(
                        id = id,
                        localDate = localDate,
                        plannedNewCount = plannedNewCount,
                        plannedReviewCount = plannedReviewCount,
                        limitMinutesSnapshot = limitMinutesSnapshot,
                        contentVersion = contentVersion,
                    ).also { database.learningSessionDao().insert(it) }
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
            session
        }

    suspend fun markSessionActive(sessionId: String, now: Instant = clock.instant()) {
        database.withTransaction {
            val session = requireNotNull(database.learningSessionDao().getById(sessionId))
            database.learningSessionDao().update(
                session.copy(
                    status = SessionStatus.ACTIVE,
                    startedAt = session.startedAt ?: now,
                    activeSegmentStartedAt = session.activeSegmentStartedAt ?: now,
                    pauseReason = null,
                ),
            )
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    suspend fun reconcileLaunchDate(today: LocalDate = LocalDate.now(clock.zone), now: Instant = clock.instant()): Boolean {
        val settings = settings.first()
        val lastKnownDate = settings.lastKnownLocalDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (lastKnownDate != null && today.isBefore(lastKnownDate)) {
            logErrorInternal(
                code = "DATE_ROLLBACK_DETECTED",
                context = """{"lastKnownLocalDate":"$lastKnownDate","currentLocalDate":"$today"}""",
                now = now,
            )
            return false
        }
        database.withTransaction {
            closeOpenSessionsBeforeInternal(today, now)
            cleanupAbandonedActiveSegmentsInternal(now)
        }
        settingsStore.updateSettings {
            it.copy(lastKnownLocalDate = today.toString(), lastSuccessfulSaveAt = now.toEpochMilli())
        }
        return true
    }

    suspend fun tickActiveSession(sessionId: String, now: Instant = clock.instant()): LearningSessionEntity? =
        database.withTransaction {
            val session = database.learningSessionDao().getById(sessionId) ?: return@withTransaction null
            if (session.status != SessionStatus.ACTIVE) return@withTransaction session
            // If segment was settled (e.g. by onStop), do NOT restart it from a background tick
            if (session.activeSegmentStartedAt == null) return@withTransaction session
            val elapsed = activeElapsedInternal(session, now)
            val limitMs = session.limitMinutesSnapshot * 60_000L
            val updated = session.copy(
                activeElapsedMs = elapsed,
                activeSegmentStartedAt = now,
                endPendingReason = if (elapsed >= limitMs) EarlyEndReason.TIME_LIMIT else session.endPendingReason,
            )
            database.learningSessionDao().update(updated)
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
            updated
        }

    suspend fun settleActiveSegment(sessionId: String, now: Instant = clock.instant()) {
        database.withTransaction {
            val session = requireNotNull(database.learningSessionDao().getById(sessionId))
            val elapsed = activeElapsedInternal(session, now)
            database.learningSessionDao().update(
                session.copy(activeElapsedMs = elapsed, activeSegmentStartedAt = null),
            )
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    suspend fun settleAllOpenActiveSegments(now: Instant = clock.instant()) {
        database.withTransaction {
            database.learningSessionDao().getOpenSessions()
                .filter { it.activeSegmentStartedAt != null }
                .forEach { session ->
                    database.learningSessionDao().update(
                        session.copy(
                            activeElapsedMs = activeElapsedInternal(session, now),
                            activeSegmentStartedAt = null,
                        ),
                    )
                }
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    suspend fun pauseForRest(sessionId: String, now: Instant = clock.instant()) {
        database.withTransaction {
            val session = requireNotNull(database.learningSessionDao().getById(sessionId))
            // ENDED_EARLY sessions (e.g. TIME_LIMIT) must not be downgraded to PAUSED
            if (session.status == SessionStatus.ENDED_EARLY) return@withTransaction
            val startedAt = session.activeSegmentStartedAt
            val elapsed = if (startedAt == null) session.activeElapsedMs else {
                session.activeElapsedMs + (now.toEpochMilli() - startedAt.toEpochMilli()).coerceAtLeast(0)
            }
            database.learningSessionDao().update(
                session.copy(
                    status = SessionStatus.PAUSED,
                    activeElapsedMs = elapsed,
                    activeSegmentStartedAt = null,
                    pauseReason = PauseReason.USER_REST,
                ),
            )
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    suspend fun completeSession(sessionId: String, now: Instant = clock.instant()) {
        database.withTransaction {
            val session = requireNotNull(database.learningSessionDao().getById(sessionId))
            val items = database.sessionItemDao().getForSession(sessionId)
            check(items.isNotEmpty() && items.all { it.status == ItemStatus.COMPLETED }) {
                "Session cannot complete before all planned items are completed"
            }
            database.learningSessionDao().update(
                session.copy(
                    status = SessionStatus.COMPLETED,
                    completedAt = now,
                    activeSegmentStartedAt = null,
                    pauseReason = null,
                    earlyEndReason = null,
                    endPendingReason = null,
                ),
            )
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    suspend fun requestTimeLimitEnd(sessionId: String) {
        database.withTransaction {
            val now = clock.instant()
            val session = requireNotNull(database.learningSessionDao().getById(sessionId))
            database.learningSessionDao().update(session.copy(endPendingReason = EarlyEndReason.TIME_LIMIT))
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    suspend fun endEarly(sessionId: String, reason: EarlyEndReason, now: Instant = clock.instant()) {
        database.withTransaction {
            val session = requireNotNull(database.learningSessionDao().getById(sessionId))
            database.learningSessionDao().update(
                session.copy(
                    status = SessionStatus.ENDED_EARLY,
                    completedAt = now,
                    activeSegmentStartedAt = null,
                    endPendingReason = null,
                    earlyEndReason = reason,
                ),
            )
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    suspend fun closeOpenSessionsBefore(localDate: LocalDate, now: Instant = clock.instant()) {
        database.withTransaction { closeOpenSessionsBeforeInternal(localDate, now) }
        settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
    }

    // ========== INITIAL LEARNING (A/B/C steps) ==========

    suspend fun startInitialLearning(
        characterId: String,
        step: InitialTeachingStep = InitialTeachingStep.A_CONTEXT,
        now: Instant = clock.instant(),
    ) {
        database.withTransaction {
            val progress = requireNotNull(database.characterProgressDao().getById(characterId))
            if (progress.firstStartedAt == null) {
                database.characterProgressDao().update(
                    progress.copy(
                        state = LearningState.FIRST_LEARNING,
                        firstStartedAt = now,
                        initialTeachingStep = step,
                        updatedAt = now,
                    ),
                )
            }
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    suspend fun saveTeachingStep(
        characterId: String,
        step: InitialTeachingStep,
        now: Instant = clock.instant(),
    ) {
        database.withTransaction {
            val progress = requireNotNull(database.characterProgressDao().getById(characterId))
            database.characterProgressDao().update(progress.copy(initialTeachingStep = step, updatedAt = now))
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    // ========== ANSWER SUBMISSION - 8 STEP ATOMIC TRANSACTION ==========

    data class SubmitAnswerResult(
        val attempt: PracticeAttemptEntity,
        val itemCompleted: Boolean,
        val sessionCompleted: Boolean,
        val endedEarly: Boolean,
    )

    suspend fun submitAnswer(
        questionInstanceId: String,
        selectedOptionId: String,
        answeredAt: Instant,
        localDate: LocalDate,
        responseTimeMs: Long,
        hintLevel: HintLevel = HintLevel.NONE,
        isAccidental: Boolean = false,
    ): SubmitAnswerResult =
        database.withTransaction {
            // STEP 1: Idempotency check — already completed questions return last attempt and do nothing
            val question = requireNotNull(database.questionInstanceDao().getById(questionInstanceId))
            if (question.status == QuestionStatus.COMPLETED) {
                val existingAttempt = requireNotNull(database.practiceAttemptDao().getForQuestion(questionInstanceId).lastOrNull())
                return@withTransaction SubmitAnswerResult(
                    attempt = existingAttempt,
                    itemCompleted = false,
                    sessionCompleted = false,
                    endedEarly = false,
                )
            }
            val item = requireNotNull(database.sessionItemDao().getById(question.sessionItemId))
            val session = requireNotNull(database.learningSessionDao().getById(item.sessionId))
            val progress = requireNotNull(database.characterProgressDao().getById(item.characterId))
            val attemptNumber = database.practiceAttemptDao().countForQuestion(questionInstanceId) + 1
            val isCorrect = selectedOptionId == question.correctOptionId
            val effectiveHintLevel = if (attemptNumber >= 2 && !isCorrect && hintLevel == HintLevel.NONE) {
                HintLevel.STRONG
            } else {
                hintLevel
            }
            val independentCorrect = attemptNumber == 1 && isCorrect && effectiveHintLevel == HintLevel.NONE && !isAccidental
            val attempt = PracticeAttemptEntity(
                id = UUID.randomUUID().toString(),
                questionInstanceId = questionInstanceId,
                characterId = item.characterId,
                attemptNumber = attemptNumber,
                selectedOptionId = selectedOptionId,
                isCorrect = isCorrect,
                hintLevel = effectiveHintLevel,
                independentCorrect = independentCorrect,
                isAccidental = isAccidental,
                answeredAt = answeredAt,
                localDate = localDate,
                responseTimeMs = responseTimeMs,
            )
            if (isAccidental) {
                // Accidental touches are never persisted. Do not update anything else.
                return@withTransaction SubmitAnswerResult(
                    attempt = attempt,
                    itemCompleted = false,
                    sessionCompleted = false,
                    endedEarly = false,
                )
            }

            // STEP 2: Write attempt
            database.practiceAttemptDao().insert(attempt)

            val sessionQuestions = database.sessionItemDao().getForSession(session.id)
                .flatMap { database.questionInstanceDao().getForItem(it.id) }
            val firstWrongQuestionCount = if (sessionQuestions.isEmpty()) 0 else {
                database.practiceAttemptDao().getForQuestions(sessionQuestions.map { it.id })
                    .groupBy { it.questionInstanceId }
                    .count { (_, attempts) ->
                        attempts.minByOrNull { it.attemptNumber }?.let { first ->
                            !first.isCorrect && !first.isAccidental
                        } == true
                    }
            }
            val fatigueReached = firstWrongQuestionCount >= 3
            val finalOutcome = when {
                isCorrect -> FinalOutcome.CORRECT
                attemptNumber >= 2 -> FinalOutcome.TAUGHT_AFTER_ERROR
                else -> null
            }
            val completedQuestion = finalOutcome != null

            // STEP 3: Update question
            database.questionInstanceDao().update(
                question.copy(
                    status = if (completedQuestion) QuestionStatus.COMPLETED else QuestionStatus.ACTIVE,
                    selectedOptionId = selectedOptionId,
                    finalOutcome = finalOutcome,
                ),
            )

            // STEP 4: Update item — strictly check ALL questions are COMPLETED (no shortcut for current question)
            val itemQuestions = database.questionInstanceDao().getForItem(item.id)
            val itemCompleted = itemQuestions.all { it.status == QuestionStatus.COMPLETED }
            val finalItem = if (itemCompleted) {
                item.copy(
                    status = ItemStatus.COMPLETED,
                    dueCheckPassed = if (item.kind == ItemKind.REVIEW) {
                        itemQuestions.all { q ->
                            val qAttempts = database.practiceAttemptDao().getForQuestion(q.id)
                            qAttempts.firstOrNull()?.let {
                                it.independentCorrect && it.isCorrect && !it.isAccidental
                            } == true
                        }
                    } else {
                        null
                    },
                    completedAt = answeredAt,
                    completedLocalDate = localDate,
                )
            } else {
                item.copy(status = ItemStatus.ACTIVE)
            }
            database.sessionItemDao().update(finalItem)

            // STEP 5 (partial): build progress update; actual write is combined with mastery recalc below
            var updatedProgress = progress
            if (itemCompleted) {
                updatedProgress = if (item.kind == ItemKind.NEW) {
                    val firstLearnDate = progress.firstLearnDate ?: localDate
                    val next = reviewScheduler.next(progress.reviewStage, firstLearnDate, localDate)
                    progress.copy(
                        state = LearningState.REVIEWING,
                        firstLearnDate = firstLearnDate,
                        initialLessonCompleted = true,
                        initialTeachingStep = InitialTeachingStep.DONE,
                        reviewStage = next?.first ?: progress.reviewStage,
                        nextReviewDate = next?.second,
                        updatedAt = answeredAt,
                    )
                } else if (finalItem.dueCheckPassed == true) {
                    val firstLearnDate = requireNotNull(progress.firstLearnDate)
                    val next = if (item.reviewStageAtStart == ReviewStage.D14 && progress.currentOralStatus != OralStatus.INDEPENDENT_PASS) {
                        // Spec: D14 APP passed but oral not INDEPENDENT_PASS yet → keep D14 stage, recheck in 7 days
                        ReviewStage.D14 to localDate.plusDays(7)
                    } else {
                        reviewScheduler.next(item.reviewStageAtStart, firstLearnDate, localDate)
                    }
                    progress.copy(
                        reviewStage = next?.first ?: progress.reviewStage,
                        nextReviewDate = next?.second,
                        appDelayedCheckStatus = if (item.reviewStageAtStart == ReviewStage.D14) DelayedStatus.PASS else progress.appDelayedCheckStatus,
                        appDelayedCheckAt = if (item.reviewStageAtStart == ReviewStage.D14) answeredAt else progress.appDelayedCheckAt,
                        temporaryRollbackAt = null,
                        updatedAt = answeredAt,
                    )
                } else if (item.kind == ItemKind.REVIEW) {
                    val failedDatesAfterStable = progress.stableQualifiedAt?.let {
                        database.sessionItemDao().countFailedReviewDatesAfter(item.characterId, it.toEpochMilli())
                    } ?: 0
                    val keepStableAfterOneFailure = progress.state == LearningState.STABLE_MASTERED && failedDatesAfterStable < 2
                    progress.copy(
                        state = if (keepStableAfterOneFailure) LearningState.STABLE_MASTERED else LearningState.REVIEWING,
                        nextReviewDate = localDate.plusDays(1),
                        temporaryRollbackAt = if (keepStableAfterOneFailure) progress.temporaryRollbackAt else answeredAt,
                        stableRollbackAt = if (!keepStableAfterOneFailure && progress.state == LearningState.STABLE_MASTERED) answeredAt else progress.stableRollbackAt,
                        updatedAt = answeredAt,
                    )
                } else {
                    progress.copy(updatedAt = answeredAt)
                }
                database.characterProgressDao().update(updatedProgress)
            }

            // STEP 5 (session cursor) + STEP 6 mastery (via engine) + STEP 7 reviewDate already above
            val refreshedQuestions = database.questionInstanceDao().getForItem(item.id)
            val nextQuestion = refreshedQuestions.firstOrNull { it.status != QuestionStatus.COMPLETED }
            val refreshedItems = database.sessionItemDao().getForSession(session.id)
            val nextItemIndex = refreshedItems.indexOfFirst { it.status != ItemStatus.COMPLETED }.let { if (it < 0) refreshedItems.size else it }
            val sessionCompleted = refreshedItems.isNotEmpty() && refreshedItems.all { it.status == ItemStatus.COMPLETED }
            val shouldEndEarlyNow = (session.endPendingReason != null || fatigueReached) && completedQuestion
            val elapsedAtAnswer = session.activeSegmentStartedAt?.let {
                session.activeElapsedMs + (answeredAt.toEpochMilli() - it.toEpochMilli()).coerceAtLeast(0)
            } ?: session.activeElapsedMs
            val nextSessionStatus = when {
                shouldEndEarlyNow -> SessionStatus.ENDED_EARLY
                sessionCompleted -> SessionStatus.COMPLETED
                else -> SessionStatus.ACTIVE
            }
            val earlyReason = when {
                shouldEndEarlyNow -> session.endPendingReason ?: EarlyEndReason.FATIGUE
                sessionCompleted -> null
                else -> session.earlyEndReason
            }
            val finalSession = session.copy(
                status = nextSessionStatus,
                completedAt = if (sessionCompleted || shouldEndEarlyNow) answeredAt else session.completedAt,
                currentItemIndex = nextItemIndex,
                currentQuestionInstanceId = nextQuestion?.id,
                activeElapsedMs = elapsedAtAnswer,
                activeSegmentStartedAt = if (nextSessionStatus == SessionStatus.ACTIVE) answeredAt else null,
                endPendingReason = if (sessionCompleted || shouldEndEarlyNow) null else if (fatigueReached) EarlyEndReason.FATIGUE else session.endPendingReason,
                earlyEndReason = earlyReason,
            ).let {
                if (it.earlyEndReason == EarlyEndReason.FATIGUE || it.earlyEndReason == EarlyEndReason.TIME_LIMIT) {
                    it.copy(status = SessionStatus.ENDED_EARLY)
                } else {
                    it
                }
            }
            database.learningSessionDao().update(finalSession)

            // STEP 6: Recompute mastery state in same transaction
            if (itemCompleted && item.kind != ItemKind.TEST) {
                masteryStateEngine.value.recalculateInCurrentTransaction(item.characterId, answeredAt, localDate)
            }

            // STEP 8: Update lastSuccessfulSaveAt in Settings (this step is REQUIRED per 8-step spec)
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = answeredAt.toEpochMilli()) }

            SubmitAnswerResult(
                attempt = attempt,
                itemCompleted = itemCompleted,
                sessionCompleted = sessionCompleted,
                endedEarly = finalSession.status == SessionStatus.ENDED_EARLY,
            )
        }

    // ========== ROUTE RESOLVER ==========

    suspend fun resolveNextRoute(sessionId: String): ShiziRoute {
        val session = database.learningSessionDao().getById(sessionId) ?: return ShiziRoute.Home
        val items = database.sessionItemDao().getForSession(session.id)
        val item = items.firstOrNull { it.status != ItemStatus.COMPLETED } ?: return ShiziRoute.Result
        val progress = database.characterProgressDao().getById(item.characterId)
        return if (item.kind == ItemKind.NEW && progress?.initialLessonCompleted != true) {
            ShiziRoute.Learn
        } else {
            val hasPendingQuestion = database.questionInstanceDao().getForItem(item.id)
                .any { it.status != QuestionStatus.COMPLETED }
            if (hasPendingQuestion) ShiziRoute.Practice else ShiziRoute.Result
        }
    }

    // ========== ORAL CHECK ==========

    suspend fun appendOralCheck(
        check: OralCheckEntity,
        today: LocalDate = LocalDate.now(clock.zone),
        now: Instant = clock.instant(),
    ) {
        database.withTransaction {
            check.revisionOf?.let { previousId ->
                database.oralCheckDao().getHistory(check.characterId)
                    .firstOrNull { it.id == previousId }
                    ?.let { database.oralCheckDao().update(it.copy(isSuperseded = true)) }
            }
            database.oralCheckDao().insert(check)
            val progress = requireNotNull(database.characterProgressDao().getById(check.characterId))
            database.characterProgressDao().update(
                progress.copy(
                    currentOralStatus = check.result,
                    currentOralCheckAt = check.checkedAt,
                    updatedAt = now,
                ),
            )
            masteryStateEngine.value.recalculateInCurrentTransaction(check.characterId, now, today)
            settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
        }
    }

    // ========== SETTINGS ==========

    suspend fun updateSettings(transform: (ShiziSettings) -> ShiziSettings): ShiziSettings {
        val now = clock.instant()
        var result: ShiziSettings = ShiziSettings()
        database.withTransaction {
            result = settingsStore.updateSettings {
                transform(it).copy(lastSuccessfulSaveAt = now.toEpochMilli())
            }
        }
        return result
    }

    suspend fun completeOnboarding(nickname: String = "", contentVersion: String = "1.0.0"): ShiziSettings {
        val now = clock.instant()
        return settingsStore.updateSettings { current ->
            ShiziSettings(
                onboardingCompleted = true,
                nickname = nickname,
                dailyNewCharacterCount = current.dailyNewCharacterCount,
                sessionLimitMinutes = current.sessionLimitMinutes,
                volumePercent = current.volumePercent,
                isMuted = current.isMuted,
                lastKnownLocalDate = current.lastKnownLocalDate,
                lastSuccessfulSaveAt = now.toEpochMilli(),
                contentVersion = contentVersion,
            )
        }
    }

    // ========== SAFE DATA CLEAR ==========

    sealed class ClearResult {
        data object Success : ClearResult()
        data class Failed(val stage: String, val cause: Throwable?) : ClearResult()
    }

    /**
     * Atomic safe clear:
     * 1) Snapshot current settings BEFORE any change.
     * 2) Reset Settings to defaults FIRST (if this fails, Room data is untouched — fully recoverable).
     * 3) Only AFTER Settings success, clear Room DB in a transaction (atomic all-or-nothing for tables).
     * 4) If step 3 fails, restore Settings from snapshot (Room data still intact).
     *
     * This ordering ensures: if ANY step fails, original learning records are preserved.
     * Caller is responsible for adult-gate verification AND second confirmation.
     */
    suspend fun clearLearningDataAndResetSettingsSafely(): ClearResult {
        val snapshotBefore = settings.first()
        val defaults = ShiziSettings()
        // Stage 1: Reset Settings first. If this fails, Room data is completely untouched.
        try {
            settingsStore.updateSettings { defaults }
            // DataStore edit completion and a newly collected Flow can briefly race on some
            // vendor builds. Do not clear Room until the default values are actually readable.
            withTimeout(5_000L) {
                settings.first { current ->
                    !current.onboardingCompleted &&
                        current.nickname.isEmpty() &&
                        current.dailyNewCharacterCount == defaults.dailyNewCharacterCount &&
                        current.sessionLimitMinutes == defaults.sessionLimitMinutes &&
                        current.volumePercent == defaults.volumePercent &&
                        !current.isMuted
                }
            }
        } catch (t: Throwable) {
            return ClearResult.Failed("SETTINGS_RESET", t)
        }
        // Stage 2: Clear Room. If this fails, restore Settings snapshot so original state is preserved.
        try {
            database.withTransaction {
                database.practiceAttemptDao().deleteAll()
                database.questionInstanceDao().deleteAll()
                database.sessionItemDao().deleteAll()
                database.learningSessionDao().deleteAll()
                database.oralCheckDao().deleteAll()
                database.characterProgressDao().deleteAll()
                database.appErrorLogDao().deleteAll()
            }
        } catch (t: Throwable) {
            // Room clear failed — restore Settings so we're back to the original state
            runCatching { settingsStore.updateSettings { snapshotBefore } }
            return ClearResult.Failed("DATA_CLEAR", t)
        }
        return ClearResult.Success
    }

    @Deprecated(
        message = "Use clearLearningDataAndResetSettingsSafely() which provides ordered two-phase clear.",
        replaceWith = ReplaceWith("clearLearningDataAndResetSettingsSafely()"),
    )
    suspend fun clearLearningDataAndResetSettings() {
        when (clearLearningDataAndResetSettingsSafely()) {
            ClearResult.Success -> Unit
            is ClearResult.Failed -> error("Legacy clear failed; see cause in app_error_log")
        }
    }

    // ========== ERROR LOGGING ==========

    suspend fun logError(code: String, context: String = "{}", now: Instant = clock.instant()) {
        database.withTransaction { logErrorInternal(code, context, now) }
    }

    suspend fun recordUxEvent(
        event: String,
        testChildId: String,
        characterId: String? = null,
        sessionId: String? = null,
        metadata: String = "{}",
        now: Instant = clock.instant(),
    ) {
        val context = """{"event":"$event","testChildId":"${testChildId.replace("\"", "\\\"")}","characterId":${characterId?.let { "\"${it.replace("\"", "\\\"")}\"" } ?: "null"},"sessionId":${sessionId?.let { "\"${it.replace("\"", "\\\"")}\"" } ?: "null"},"metadata":$metadata}"""
        logError("UX_EVENT", context, now)
    }

    suspend fun latestUxEvents(limit: Int = 30): List<String> =
        database.appErrorLogDao().latest(limit).filter { it.code == "UX_EVENT" }.map { "${it.occurredAt} ${it.context}" }

    private suspend fun logErrorInternal(code: String, context: String, now: Instant) {
        database.appErrorLogDao().insert(
            AppErrorLogEntity(
                id = UUID.randomUUID().toString(),
                code = code,
                occurredAt = now,
                context = context,
            ),
        )
        database.appErrorLogDao().trimToLatest50()
        settingsStore.updateSettings { it.copy(lastSuccessfulSaveAt = now.toEpochMilli()) }
    }

    suspend fun logAudioError(
        error: com.family.shizi.ui.audio.AudioPlayerError,
        now: Instant = clock.instant(),
    ) {
        val safeAsset = error.assetPath.replace("\"", "\\\"")
        val safeMsg = error.message.replace("\"", "\\\"")
        val safeThrowable = (error.throwableClass ?: "").replace("\"", "\\\"")
        logError(
            code = "AUDIO_${safeMsg.ifBlank { "UNKNOWN" }}",
            context = """{"asset":"$safeAsset","message":"$safeMsg","throwable":"$safeThrowable"}""",
            now = now,
        )
    }

    // ========== INTERNAL HELPERS ==========

    private suspend fun closeOpenSessionsBeforeInternal(localDate: LocalDate, now: Instant = clock.instant()) {
        val dao = database.learningSessionDao()
        dao.getOpenSessions()
            .filter { it.localDate < localDate }
            .forEach { session ->
                dao.update(
                    session.copy(
                        status = SessionStatus.ENDED_EARLY,
                        completedAt = now,
                        activeSegmentStartedAt = null,
                        endPendingReason = null,
                        earlyEndReason = EarlyEndReason.DAY_ROLLOVER,
                    ),
                )
            }
    }

    private suspend fun cleanupAbandonedActiveSegmentsInternal(now: Instant) {
        database.learningSessionDao().getOpenSessions()
            .filter { it.status == SessionStatus.ACTIVE && it.activeSegmentStartedAt != null }
            .forEach { session ->
                database.learningSessionDao().update(
                    session.copy(
                        status = SessionStatus.PAUSED,
                        activeElapsedMs = session.activeElapsedMs,
                        activeSegmentStartedAt = null,
                        pauseReason = PauseReason.COLD_START_CLEANUP,
                    ),
                )
            }
    }

    private fun activeElapsedInternal(session: LearningSessionEntity, now: Instant, maxDeltaMs: Long? = null): Long {
        val startedAt = session.activeSegmentStartedAt ?: return session.activeElapsedMs
        val rawDelta = (now.toEpochMilli() - startedAt.toEpochMilli()).coerceAtLeast(0)
        val delta = maxDeltaMs?.let { rawDelta.coerceAtMost(it) } ?: rawDelta
        return session.activeElapsedMs + delta
    }
}
