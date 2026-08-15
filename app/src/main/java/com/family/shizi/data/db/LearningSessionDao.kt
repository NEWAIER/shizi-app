package com.family.shizi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate

@Dao
interface LearningSessionDao {
    @Query("SELECT * FROM learning_session WHERE id = :id")
    suspend fun getById(id: String): LearningSessionEntity?

    @Query("SELECT * FROM learning_session WHERE localDate = :localDate AND status != 'ERROR' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getUsableByDate(localDate: LocalDate): LearningSessionEntity?

    @Query("SELECT * FROM learning_session WHERE localDate = :localDate AND status != 'ERROR' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getMostRecentForDate(localDate: LocalDate): LearningSessionEntity?

    @Query("SELECT * FROM learning_session WHERE status IN ('CREATED', 'ACTIVE', 'PAUSED') ORDER BY localDate ASC")
    suspend fun getOpenSessions(): List<LearningSessionEntity>

    @Query("SELECT * FROM learning_session WHERE status IN ('CREATED', 'ACTIVE', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun getMostRecentlyActive(): LearningSessionEntity?

    @Query("SELECT COUNT(*) FROM learning_session")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(DISTINCT localDate) FROM learning_session WHERE status = 'COMPLETED' AND (plannedNewCount > 0 OR plannedReviewCount > 0)")
    suspend fun countCompletedLearningDays(): Int

    @Query("SELECT * FROM learning_session WHERE status = 'COMPLETED' AND plannedNewCount = 0 AND plannedReviewCount = 0 ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLatestCompletedStageTest(): LearningSessionEntity?

    @Query("SELECT * FROM learning_session WHERE status = 'COMPLETED' AND plannedNewCount = 0 AND plannedReviewCount = 0 ORDER BY completedAt ASC")
    suspend fun getCompletedStageTests(): List<LearningSessionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LearningSessionEntity)

    @Update
    suspend fun update(entity: LearningSessionEntity)

    @Query("DELETE FROM learning_session")
    suspend fun deleteAll()
}
