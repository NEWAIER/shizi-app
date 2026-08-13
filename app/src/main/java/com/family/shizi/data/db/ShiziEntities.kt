package com.family.shizi.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "character_progress")
data class CharacterProgressEntity(
    @PrimaryKey val characterId: String,
    val state: LearningState = LearningState.UNLEARNED,
    val firstStartedAt: Instant? = null,
    val firstLearnDate: LocalDate? = null,
    val initialTeachingStep: InitialTeachingStep = InitialTeachingStep.NOT_STARTED,
    val initialLessonCompleted: Boolean = false,
    val reviewStage: ReviewStage = ReviewStage.NONE,
    val nextReviewDate: LocalDate? = null,
    val appDelayedCheckStatus: DelayedStatus = DelayedStatus.NOT_DUE,
    val appDelayedCheckAt: Instant? = null,
    val currentOralStatus: OralStatus = OralStatus.NOT_TESTED,
    val currentOralCheckAt: Instant? = null,
    val temporaryQualifiedAt: Instant? = null,
    val stableQualifiedAt: Instant? = null,
    val temporaryRollbackAt: Instant? = null,
    val stableRollbackAt: Instant? = null,
    val isErrorProne: Boolean = false,
    val updatedAt: Instant,
)

@Entity(
    tableName = "learning_session",
    indices = [Index(value = ["localDate", "status"])],
)
data class LearningSessionEntity(
    @PrimaryKey val id: String,
    val localDate: LocalDate,
    val status: SessionStatus = SessionStatus.CREATED,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val currentItemIndex: Int = 0,
    val currentQuestionInstanceId: String? = null,
    val plannedNewCount: Int = 0,
    val plannedReviewCount: Int = 0,
    val limitMinutesSnapshot: Int,
    val contentVersion: String,
    val activeElapsedMs: Long = 0,
    val activeSegmentStartedAt: Instant? = null,
    val pauseReason: PauseReason? = null,
    val endPendingReason: EarlyEndReason? = null,
    val earlyEndReason: EarlyEndReason? = null,
)

@Entity(
    tableName = "session_item",
    foreignKeys = [
        ForeignKey(
            entity = LearningSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("sessionId"),
        Index(value = ["characterId", "kind", "completedLocalDate", "completedAt"]),
    ],
)
data class SessionItemEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val characterId: String,
    val kind: ItemKind,
    val sequence: Int,
    val status: ItemStatus = ItemStatus.PENDING,
    val reviewStageAtStart: ReviewStage = ReviewStage.NONE,
    val dueCheckPassed: Boolean? = null,
    val completedAt: Instant? = null,
    val completedLocalDate: LocalDate? = null,
)

@Entity(
    tableName = "question_instance",
    foreignKeys = [
        ForeignKey(
            entity = SessionItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionItemId")],
)
data class QuestionInstanceEntity(
    @PrimaryKey val id: String,
    val sessionItemId: String,
    val questionSeedId: String,
    val questionType: String,
    val evidenceCategory: String,
    val optionIdsJson: String = "[]",
    val correctOptionId: String,
    val status: QuestionStatus = QuestionStatus.PENDING,
    val selectedOptionId: String? = null,
    val finalOutcome: FinalOutcome? = null,
    val purpose: QuestionPurpose,
    val isMilestoneQuestion: Boolean = false,
)

@Entity(
    tableName = "practice_attempt",
    foreignKeys = [
        ForeignKey(
            entity = QuestionInstanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionInstanceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("questionInstanceId"),
        Index(value = ["questionInstanceId", "attemptNumber"], unique = true),
        Index(value = ["characterId", "localDate"]),
    ],
)
data class PracticeAttemptEntity(
    @PrimaryKey val id: String,
    val questionInstanceId: String,
    val characterId: String,
    val attemptNumber: Int = 1,
    val selectedOptionId: String,
    val isCorrect: Boolean = false,
    val hintLevel: HintLevel = HintLevel.NONE,
    val independentCorrect: Boolean = false,
    val isAccidental: Boolean = false,
    val answeredAt: Instant,
    val localDate: LocalDate,
    val responseTimeMs: Long = 0,
)

@Entity(
    tableName = "oral_check",
    indices = [
        Index("characterId"),
        Index("revisionOf"),
    ],
)
data class OralCheckEntity(
    @PrimaryKey val id: String,
    val characterId: String,
    val result: OralStatus,
    val checkedAt: Instant,
    val localDate: LocalDate,
    val eligibleForStable: Boolean = false,
    val revisionOf: String? = null,
    val isSuperseded: Boolean = false,
)

@Entity(tableName = "app_error_log")
data class AppErrorLogEntity(
    @PrimaryKey val id: String,
    val code: String,
    val occurredAt: Instant,
    val context: String = "{}",
)
