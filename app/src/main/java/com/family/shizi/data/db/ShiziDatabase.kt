package com.family.shizi.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CharacterProgressEntity::class,
        LearningSessionEntity::class,
        SessionItemEntity::class,
        QuestionInstanceEntity::class,
        PracticeAttemptEntity::class,
        OralCheckEntity::class,
        AppErrorLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ShiziTypeConverters::class)
abstract class ShiziDatabase : RoomDatabase() {
    abstract fun characterProgressDao(): CharacterProgressDao
    abstract fun learningSessionDao(): LearningSessionDao
    abstract fun sessionItemDao(): SessionItemDao
    abstract fun questionInstanceDao(): QuestionInstanceDao
    abstract fun practiceAttemptDao(): PracticeAttemptDao
    abstract fun oralCheckDao(): OralCheckDao
    abstract fun appErrorLogDao(): AppErrorLogDao

    companion object {
        const val DATABASE_NAME = "shizi.db"

        @Volatile private var instance: ShiziDatabase? = null

        /**
         * Opens the existing database without destructive fallback. A failed open must preserve
         * the original files for diagnosis and an explicitly parent-confirmed recovery action.
         */
        fun getInstance(
            context: Context,
            databaseName: String = DATABASE_NAME,
            opener: (Context) -> ShiziDatabase = { appContext ->
                Room.databaseBuilder(appContext, ShiziDatabase::class.java, databaseName).build()
            },
        ): ShiziDatabase? {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val appContext = context.applicationContext
                return runCatching { opener(appContext).also { instance = it } }.getOrNull()
            }
        }

        fun clearInstance() {
            synchronized(this) {
                instance = null
            }
        }
    }
}
