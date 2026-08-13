package com.family.shizi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate

@Dao
interface SessionItemDao {
    @Query("SELECT * FROM session_item WHERE sessionId = :sessionId ORDER BY sequence ASC")
    suspend fun getForSession(sessionId: String): List<SessionItemEntity>

    @Query("SELECT * FROM session_item WHERE id = :id")
    suspend fun getById(id: String): SessionItemEntity?

    @Query("SELECT COUNT(*) FROM session_item")
    suspend fun countAll(): Int

    @Query("DELETE FROM session_item")
    suspend fun deleteAll()

    @Query("SELECT * FROM session_item WHERE characterId = :characterId AND kind = 'REVIEW' AND completedLocalDate IS NOT NULL ORDER BY completedAt DESC")
    suspend fun getCompletedReviews(characterId: String): List<SessionItemEntity>

    @Query(
        """
        SELECT * FROM session_item
        WHERE kind = 'NEW' AND status != 'COMPLETED'
        ORDER BY sequence ASC
        """,
    )
    suspend fun getUnfinishedNewItems(): List<SessionItemEntity>

    @Query(
        """
        SELECT COUNT(DISTINCT completedLocalDate) FROM session_item
        WHERE characterId = :characterId AND kind = 'REVIEW' AND dueCheckPassed = 0
          AND completedLocalDate IS NOT NULL AND completedAt > :afterEpochMillis
        """,
    )
    suspend fun countFailedReviewDatesAfter(characterId: String, afterEpochMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<SessionItemEntity>)

    @Update
    suspend fun update(entity: SessionItemEntity)
}
