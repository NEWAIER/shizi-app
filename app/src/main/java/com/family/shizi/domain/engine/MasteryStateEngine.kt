package com.family.shizi.domain.engine

import androidx.room.withTransaction
import com.family.shizi.data.db.DelayedStatus
import com.family.shizi.data.db.LearningState
import com.family.shizi.data.db.OralStatus
import com.family.shizi.data.db.ShiziDatabase
import java.time.Instant
import java.time.LocalDate

class MasteryStateEngine(private val database: ShiziDatabase) {
    suspend fun recalculate(characterId: String, now: Instant, today: LocalDate): LearningState {
        return database.withTransaction { recalculateInCurrentTransaction(characterId, now, today) }
    }

    suspend fun recalculateInCurrentTransaction(characterId: String, now: Instant, today: LocalDate): LearningState {
        val progress = requireNotNull(database.characterProgressDao().getById(characterId))
            val allAttempts = database.practiceAttemptDao().getForCharacter(characterId)
                .filter { it.independentCorrect && it.isCorrect && !it.isAccidental }
            val attempts = progress.stableRollbackAt?.let { rollbackAt ->
                allAttempts.filter { it.answeredAt > rollbackAt }
            } ?: allAttempts
            val questionIds = attempts.map { it.questionInstanceId }.toSet()
            val questions = questionIds.mapNotNull { database.questionInstanceDao().getById(it) }
            val completedReviews = database.sessionItemDao().getCompletedReviews(characterId)
                .filter { it.completedAt != null && it.completedLocalDate != null }
            val latestReview = completedReviews.maxByOrNull { it.completedAt!! }
            val firstLearnDate = progress.firstLearnDate
            val validDateCount = attempts.map { it.localDate }.toSet().size
            val questionTypeCount = questions.map { it.questionType }.toSet().size
            val evidenceCount = questions.map { it.evidenceCategory }.toSet().size

            val baseState = when {
                progress.firstStartedAt == null -> LearningState.UNLEARNED
                !progress.initialLessonCompleted -> LearningState.FIRST_LEARNING
                else -> LearningState.REVIEWING
            }
            val stableFailureDates = progress.stableQualifiedAt?.let {
                database.sessionItemDao().countFailedReviewDatesAfter(characterId, it.toEpochMilli())
            } ?: 0
            val oralRollbackRequired = progress.state == LearningState.STABLE_MASTERED &&
                progress.currentOralStatus in setOf(OralStatus.PROMPTED, OralStatus.FAIL)
            val stableCanBeKeptAfterSingleFailure = progress.state == LearningState.STABLE_MASTERED &&
                progress.stableRollbackAt == null &&
                stableFailureDates < 2 &&
                !oralRollbackRequired
            val tempEligible = baseState == LearningState.REVIEWING &&
                validDateCount >= 2 &&
                questionTypeCount >= 3 &&
                evidenceCount >= 3 &&
                latestReview?.dueCheckPassed == true &&
                progress.temporaryRollbackAt == null
            val oralAt = progress.currentOralCheckAt
            val rollbackAt = progress.stableRollbackAt
            val appEvidenceAfterRollback = rollbackAt == null || progress.appDelayedCheckAt?.let { it > rollbackAt } == true
            val oralEvidenceAfterRollback = rollbackAt == null || progress.currentOralCheckAt?.let { it > rollbackAt } == true
            val stableOralEligible = progress.currentOralStatus == OralStatus.INDEPENDENT_PASS &&
                firstLearnDate != null &&
                oralAt != null &&
                !LocalDate.ofInstant(oralAt, java.time.ZoneOffset.UTC).isBefore(firstLearnDate.plusDays(14)) &&
                oralEvidenceAfterRollback
            val stableEligible = tempEligible &&
                firstLearnDate != null &&
                !today.isBefore(firstLearnDate.plusDays(14)) &&
                validDateCount >= 3 &&
                questionTypeCount >= 4 &&
                progress.appDelayedCheckStatus == DelayedStatus.PASS &&
                appEvidenceAfterRollback &&
                stableOralEligible &&
                (progress.stableRollbackAt == null || (appEvidenceAfterRollback && oralEvidenceAfterRollback))

            val newState = when {
                stableEligible -> LearningState.STABLE_MASTERED
                stableCanBeKeptAfterSingleFailure -> LearningState.STABLE_MASTERED
                tempEligible -> LearningState.TEMP_MASTERED
                else -> baseState
            }
            // Distinguish two rollback types: TEMP downgrade vs STABLE downgrade.
            // temporaryRollbackAt tracks when TEMP_MASTERED was lost (not STABLE rollback).
            // stableRollbackAt tracks when STABLE_MASTERED was lost due to >=2 distinct-date failures or oral FAIL.
            val tempLost = progress.state == LearningState.TEMP_MASTERED &&
                newState != LearningState.TEMP_MASTERED &&
                newState != LearningState.STABLE_MASTERED &&
                progress.temporaryRollbackAt == null
            val stableLost = progress.state == LearningState.STABLE_MASTERED &&
                progress.stableRollbackAt == null &&
                (stableFailureDates >= 2 || oralRollbackRequired)

            val tempQualifiedAt = when {
            newState == LearningState.TEMP_MASTERED && progress.temporaryQualifiedAt == null -> now
            newState == LearningState.TEMP_MASTERED && progress.temporaryRollbackAt == null && progress.state == LearningState.REVIEWING -> now
            else -> progress.temporaryQualifiedAt
        }
        val stableAnchor = listOfNotNull(progress.appDelayedCheckAt, progress.currentOralCheckAt).maxOrNull() ?: now
        database.characterProgressDao().update(
            progress.copy(
                state = newState,
                temporaryQualifiedAt = tempQualifiedAt,
                stableQualifiedAt = if (newState == LearningState.STABLE_MASTERED && (progress.stableQualifiedAt == null || progress.stableRollbackAt != null)) stableAnchor else progress.stableQualifiedAt,
                stableRollbackAt = if (stableEligible) null else if (stableLost) now else progress.stableRollbackAt,
                temporaryRollbackAt = if (stableEligible) null else if (tempLost) now else progress.temporaryRollbackAt,
                updatedAt = now,
            )
        )
        return newState
    }
}
