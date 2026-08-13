package com.family.shizi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PracticeAttemptDao {
    @Query("SELECT * FROM practice_attempt WHERE questionInstanceId = :questionInstanceId ORDER BY attemptNumber ASC")
    suspend fun getForQuestion(questionInstanceId: String): List<PracticeAttemptEntity>

    @Query("SELECT COUNT(*) FROM practice_attempt WHERE questionInstanceId = :questionInstanceId")
    suspend fun countForQuestion(questionInstanceId: String): Int

    @Query("SELECT COUNT(*) FROM practice_attempt")
    suspend fun countAll(): Int

    @Query("SELECT * FROM practice_attempt WHERE characterId = :characterId ORDER BY answeredAt ASC")
    suspend fun getForCharacter(characterId: String): List<PracticeAttemptEntity>

    @Query("SELECT * FROM practice_attempt WHERE questionInstanceId IN (:questionIds) ORDER BY answeredAt ASC")
    suspend fun getForQuestions(questionIds: List<String>): List<PracticeAttemptEntity>

    @Query("DELETE FROM practice_attempt")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PracticeAttemptEntity)
}
