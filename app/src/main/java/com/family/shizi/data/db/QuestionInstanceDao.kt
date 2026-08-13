package com.family.shizi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface QuestionInstanceDao {
    @Query("SELECT * FROM question_instance WHERE id = :id")
    suspend fun getById(id: String): QuestionInstanceEntity?

    @Query("SELECT * FROM question_instance WHERE sessionItemId = :sessionItemId ORDER BY id ASC")
    suspend fun getForItem(sessionItemId: String): List<QuestionInstanceEntity>

    @Query("DELETE FROM question_instance")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM question_instance")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM question_instance WHERE sessionItemId = :sessionItemId AND questionType = :questionType")
    suspend fun countTypeInItem(sessionItemId: String, questionType: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<QuestionInstanceEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: QuestionInstanceEntity)

    @Update
    suspend fun update(entity: QuestionInstanceEntity)
}
