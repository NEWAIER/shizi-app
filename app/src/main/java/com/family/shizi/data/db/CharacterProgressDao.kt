package com.family.shizi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate

@Dao
interface CharacterProgressDao {
    @Query("SELECT * FROM character_progress ORDER BY characterId")
    suspend fun getAll(): List<CharacterProgressEntity>

    @Query("SELECT * FROM character_progress WHERE characterId = :characterId")
    suspend fun getById(characterId: String): CharacterProgressEntity?

    @Query(
        """
        SELECT * FROM character_progress
        WHERE nextReviewDate IS NOT NULL AND nextReviewDate <= :localDate
        ORDER BY nextReviewDate ASC, isErrorProne DESC, characterId ASC
        LIMIT :limit
        """,
    )
    suspend fun getDueForReview(localDate: LocalDate, limit: Int): List<CharacterProgressEntity>

    @Query("SELECT COUNT(*) FROM character_progress WHERE initialLessonCompleted = 1")
    suspend fun countInitialLessonsCompleted(): Int

    @Query("SELECT COUNT(*) FROM character_progress")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: CharacterProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CharacterProgressEntity)

    @Update
    suspend fun update(entity: CharacterProgressEntity)

    @Query("DELETE FROM character_progress")
    suspend fun deleteAll()
}
