package com.streak.examprepai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserPreferencesEntity::class, ProgressStatsEntity::class, QuizHistoryEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExamPrepDatabase : RoomDatabase() {

    abstract fun examPrepDao(): ExamPrepDao

    companion object {
        @Volatile
        private var instance: ExamPrepDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN currentStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN lastActiveDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): ExamPrepDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExamPrepDatabase::class.java,
                    "exam_prep_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
