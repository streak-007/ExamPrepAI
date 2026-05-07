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

    @Query("SELECT * FROM quiz_history ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentQuizHistory(limit: Int): List<QuizHistoryEntity>

    @Upsert
    fun upsertQuizHistory(entity: QuizHistoryEntity)

    @Query("SELECT * FROM saved_session WHERE id = 0")
    fun getSavedSession(): SavedSessionEntity?

    @Upsert
    fun upsertSavedSession(entity: SavedSessionEntity)

    @Query("DELETE FROM saved_session WHERE id = 0")
    fun clearSavedSession()

    @Query("SELECT * FROM question_insight WHERE questionId = :questionId")
    fun getQuestionInsight(questionId: String): QuestionInsightEntity?

    @Upsert
    fun upsertQuestionInsight(entity: QuestionInsightEntity)

    @Query("SELECT * FROM question_insight WHERE setId = :setId AND (wrongCount > 0 OR isBookmarked = 1)")
    fun getRevisionInsightsForSet(setId: String): List<QuestionInsightEntity>
}
