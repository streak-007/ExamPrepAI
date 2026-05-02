package com.streak.examprepai.data

import android.content.Context
import com.streak.examprepai.data.local.ExamPrepDatabase
import com.streak.examprepai.data.local.ProgressStatsEntity
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
}
