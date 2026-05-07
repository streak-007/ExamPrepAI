package com.streak.examprepai.data

import android.content.Context
import com.streak.examprepai.data.local.ExamPrepDatabase
import com.streak.examprepai.data.local.ProgressStatsEntity
import com.streak.examprepai.data.local.QuestionInsightEntity
import com.streak.examprepai.data.local.QuizHistoryEntity
import com.streak.examprepai.data.local.SavedSessionEntity
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
    }

    fun getSavedSession(): SavedSession? {
        return dao.getSavedSession()?.toDomain()
    }

    fun saveSession(session: SavedSession) {
        dao.upsertSavedSession(SavedSessionEntity.fromDomain(session))
    }

    fun clearSavedSession() {
        dao.clearSavedSession()
    }

    fun findQuestionSetById(setId: String): QuestionSet? {
        return SampleData.questionSets.firstOrNull { it.id == setId }
    }

    fun getRevisionQuestionCount(setId: String): Int {
        return dao.getRevisionInsightsForSet(setId).size
    }

    fun getRevisionQuestionSet(setId: String): QuestionSet? {
        val baseSet = findQuestionSetById(setId) ?: return null
        val revisionIds = dao.getRevisionInsightsForSet(setId).map { it.questionId }.toSet()
        val filteredQuestions = baseSet.questions.filter { it.id in revisionIds }
        if (filteredQuestions.isEmpty()) return null
        return baseSet.copy(
            title = "${baseSet.title} Revision",
            questions = filteredQuestions
        )
    }

    fun updateQuestionInsight(
        setId: String,
        subjectId: String,
        questionId: String,
        answeredCorrectly: Boolean? = null,
        isBookmarked: Boolean? = null
    ) {
        val current = dao.getQuestionInsight(questionId)
        val updated = QuestionInsightEntity(
            questionId = questionId,
            setId = setId,
            subjectId = subjectId,
            wrongCount = when {
                answeredCorrectly == true -> 0
                answeredCorrectly == false -> (current?.wrongCount ?: 0) + 1
                else -> current?.wrongCount ?: 0
            },
            isBookmarked = isBookmarked ?: current?.isBookmarked ?: false
        )
        dao.upsertQuestionInsight(updated)
    }
}
