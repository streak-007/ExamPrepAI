package com.streak.examprepai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [UserPreferencesEntity::class, ProgressStatsEntity::class, QuizHistoryEntity::class, SavedSessionEntity::class, QuestionInsightEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExamPrepDatabase : RoomDatabase() {

    abstract fun examPrepDao(): ExamPrepDao

    companion object {
        @Volatile
        private var instance: ExamPrepDatabase? = null

        fun getInstance(context: Context): ExamPrepDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExamPrepDatabase::class.java,
                    "exam_prep_database"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
