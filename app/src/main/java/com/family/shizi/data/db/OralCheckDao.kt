package com.family.shizi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface OralCheckDao {
    @Query("SELECT * FROM oral_check WHERE characterId = :characterId AND isSuperseded = 0 ORDER BY checkedAt DESC LIMIT 1")
    suspend fun getLatestEffective(characterId: String): OralCheckEntity?

    @Query("SELECT * FROM oral_check WHERE characterId = :characterId ORDER BY checkedAt DESC")
    suspend fun getHistory(characterId: String): List<OralCheckEntity>

    @Query("SELECT COUNT(*) FROM oral_check")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: OralCheckEntity)

    @Update
    suspend fun update(entity: OralCheckEntity)

    @Query("DELETE FROM oral_check")
    suspend fun deleteAll()
}
