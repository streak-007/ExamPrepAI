package com.streak.examprepai.data

import android.content.Context
import com.streak.examprepai.data.local.ExamPrepDatabase
import com.streak.examprepai.data.local.ProgressStatsEntity
import com.streak.examprepai.data.local.QuizHistoryEntity
import com.streak.examprepai.data.local.UserPreferencesEntity

class ExamPrepRepository(context: Context) {

    private val dao = ExamPrepDatabase.getInstance(context).examPrepDao()

    fun getExams(): List<Exam> = SampleData.exams

    fun getQuestionSetsForSubjects(subjectIds: List<String>): List<QuestionSet> {
        return SampleData.questionSets.filter { it.subjectId in subjectIds }
    }

    fun getSavedPreferences(): UserPreferences {
        return dao.getUserPreferences()?.toDomain() ?: UserPreferences()
    }

    fun savePreferences(preferences: UserPreferences) {
        dao.upsertUserPreferences(UserPreferencesEntity.fromDomain(preferences))
    }

    fun getProgressStats(): ProgressStats {
        return dao.getProgressStats()?.toDomain() ?: ProgressStats()
    }

    fun recordAnswer(isCorrect: Boolean) {
        val current = getProgressStats()
        val updated = current.copy(
            attempted = current.attempted + 1,
            correct = current.correct + if (isCorrect) 1 else 0,
            wrong = current.wrong + if (isCorrect) 0 else 1
        )
        dao.upsertProgressStats(ProgressStatsEntity.fromDomain(updated))
    }

    fun recordBatchResults(correct: Int, wrong: Int) {
        val current = getProgressStats()
        val updated = current.copy(
            attempted = current.attempted + correct + wrong,
            correct = current.correct + correct,
            wrong = current.wrong + wrong
        )
        dao.upsertProgressStats(ProgressStatsEntity.fromDomain(updated))
    }

    fun getRecentQuizHistory(limit: Int = 5): List<QuizHistoryItem> {
        return dao.getRecentQuizHistory(limit).map { it.toDomain() }
    }

    fun saveQuizHistory(item: QuizHistoryItem) {
        dao.upsertQuizHistory(QuizHistoryEntity.fromDomain(item))
        updateStreak()
    }

    private fun updateStreak() {
        val prefs = getSavedPreferences()
        val now = System.currentTimeMillis()
        val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

        // Very basic streak logic: check days since epoch in local timezone offset
        // For a more robust solution we'd use java.time.LocalDate, but this works for MVP
        val timeZoneOffset = java.util.TimeZone.getDefault().getOffset(now)
        val todayDays = (now + timeZoneOffset) / ONE_DAY_MILLIS
        val lastActiveDays = if (prefs.lastActiveDate == 0L) 0 else (prefs.lastActiveDate + timeZoneOffset) / ONE_DAY_MILLIS

        val updatedPrefs = if (lastActiveDays == todayDays) {
            // Already active today, just update the timestamp
            prefs.copy(lastActiveDate = now)
        } else if (lastActiveDays == todayDays - 1) {
            // Active yesterday, increment streak
            prefs.copy(currentStreak = prefs.currentStreak + 1, lastActiveDate = now)
        } else {
            // Missed a day (or first time), reset streak to 1
            prefs.copy(currentStreak = 1, lastActiveDate = now)
        }

        savePreferences(updatedPrefs)
    }
}
