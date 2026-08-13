package com.family.shizi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppErrorLogDao {
    @Query("SELECT * FROM app_error_log ORDER BY occurredAt DESC LIMIT :limit")
    suspend fun latest(limit: Int = 50): List<AppErrorLogEntity>

    @Query("SELECT COUNT(*) FROM app_error_log")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AppErrorLogEntity)

    @Query(
        """
        DELETE FROM app_error_log
        WHERE id NOT IN (
            SELECT id FROM app_error_log ORDER BY occurredAt DESC LIMIT 50
        )
        """,
    )
    suspend fun trimToLatest50()

    @Query("DELETE FROM app_error_log")
    suspend fun deleteAll()
}
