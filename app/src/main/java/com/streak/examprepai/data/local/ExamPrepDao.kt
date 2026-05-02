package com.streak.examprepai.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ExamPrepDao {

    @Query("SELECT * FROM user_preferences WHERE id = 0")
    fun getUserPreferences(): UserPreferencesEntity?

    @Upsert
    fun upsertUserPreferences(entity: UserPreferencesEntity)

    @Query("SELECT * FROM progress_stats WHERE id = 0")
    fun getProgressStats(): ProgressStatsEntity?

    @Upsert
    fun upsertProgressStats(entity: ProgressStatsEntity)
}
