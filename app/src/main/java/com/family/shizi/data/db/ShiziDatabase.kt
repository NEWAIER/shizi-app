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
         * Opens the database with a fallback strategy:
         * 1) Try normal open.
         * 2) On corruption/failure, delete the DB file and rebuild.
         * 3) If even rebuild fails, return null so the app can enter parent-recovery state.
         */
        fun getInstance(context: Context): ShiziDatabase? {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val appContext = context.applicationContext
                return try {
                    Room.databaseBuilder(
                        appContext,
                        ShiziDatabase::class.java,
                        DATABASE_NAME,
                    ).build().also { instance = it }
                } catch (t: Throwable) {
                    // Attempt destructive fallback: delete corrupted DB and rebuild
                    try {
                        appContext.deleteDatabase(DATABASE_NAME)
                        Room.databaseBuilder(
                            appContext,
                            ShiziDatabase::class.java,
                            DATABASE_NAME,
                        ).build().also { instance = it }
                    } catch (t2: Throwable) {
                        // Even rebuild failed — app must not crash; return null
                        null
                    }
                }
            }
        }

        fun clearInstance() {
            synchronized(this) {
                instance = null
            }
        }
    }
}
