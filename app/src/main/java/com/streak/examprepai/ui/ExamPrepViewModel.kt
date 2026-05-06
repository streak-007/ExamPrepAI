package com.streak.examprepai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.streak.examprepai.data.Exam
import com.streak.examprepai.data.ExamPrepRepository
import com.streak.examprepai.data.ProgressStats
import com.streak.examprepai.data.QuestionProgress
import com.streak.examprepai.data.QuestionReviewItem
import com.streak.examprepai.data.QuestionSet
import com.streak.examprepai.data.QuizHistoryItem
import com.streak.examprepai.data.QuizMode
import com.streak.examprepai.data.QuizSession
import com.streak.examprepai.data.QuizSummary
import com.streak.examprepai.data.ReviewFilter
import com.streak.examprepai.data.Subject
import com.streak.examprepai.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class ExamPrepUiState(
    val exams: List<Exam> = emptyList(),
    val selectedExam: Exam? = null,
    val selectedSubjectIds: Set<String> = emptySet(),
    val preferences: UserPreferences = UserPreferences(),
    val progress: ProgressStats = ProgressStats(),
    val availableSets: List<QuestionSet> = emptyList(),
    val recentHistory: List<QuizHistoryItem> = emptyList(),
    val activeSession: QuizSession? = null,
    val latestSummary: QuizSummary? = null,
    val reviewFilter: ReviewFilter = ReviewFilter.ALL
) {
    val hasOnboarded: Boolean
        get() = preferences.examId.isNotBlank()
}

class ExamPrepViewModel(
    private val repository: ExamPrepRepository
) : ViewModel() {

    private var sessionCounter: Long = 0L

    private val _uiState = MutableStateFlow(
        ExamPrepUiState(
            exams = repository.getExams(),
            preferences = repository.getSavedPreferences(),
            progress = repository.getProgressStats(),
            recentHistory = repository.getRecentQuizHistory()
        )
    )
    val uiState: StateFlow<ExamPrepUiState> = _uiState

    init {
        val preferences = _uiState.value.preferences
        val selectedExam = _uiState.value.exams.firstOrNull { it.id == preferences.examId }
        val selectedSubjectIds = preferences.subjectIds.toSet()
        _uiState.update {
            it.copy(
                selectedExam = selectedExam,
                selectedSubjectIds = selectedSubjectIds,
                availableSets = if (selectedSubjectIds.isEmpty()) {
                    emptyList()
                } else {
                    repository.getQuestionSetsForSubjects(selectedSubjectIds.toList())
                }
            )
        }
    }

    fun selectExam(exam: Exam) {
        _uiState.update {
            it.copy(
                selectedExam = exam,
                selectedSubjectIds = emptySet()
            )
        }
    }

    fun toggleSubject(subject: Subject) {
        _uiState.update { state ->
            val updated = state.selectedSubjectIds.toMutableSet().apply {
                if (contains(subject.id)) remove(subject.id) else add(subject.id)
            }
            state.copy(selectedSubjectIds = updated)
        }
    }

    fun completeOnboarding() {
        val state = _uiState.value
        val exam = state.selectedExam ?: return
        val subjects = exam.subjects.filter { it.id in state.selectedSubjectIds }
        val preferences = UserPreferences(
            examId = exam.id,
            examName = exam.name,
            subjectIds = subjects.map { it.id },
            subjectNames = subjects.map { it.name }
        )
        repository.savePreferences(preferences)
        _uiState.update {
            it.copy(
                preferences = preferences,
                availableSets = repository.getQuestionSetsForSubjects(preferences.subjectIds)
            )
        }
    }

    fun startQuiz(questionSet: QuestionSet, mode: QuizMode) {
        val firstQuestion = questionSet.questions.first()
        val initialProgress = QuestionProgress(
            questionId = firstQuestion.id,
            isVisited = true
        )
        _uiState.update { state ->
            state.copy(
                activeSession = QuizSession(
                    sessionId = ++sessionCounter,
                    mode = mode,
                    set = questionSet,
                    questionProgress = mapOf(firstQuestion.id to initialProgress),
                    timedPracticeSecondsPerQuestion = 60
                ),
                latestSummary = null,
                reviewFilter = ReviewFilter.ALL
            )
        }
    }

    fun selectOption(index: Int) {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            val question = session.currentQuestion
            val progress = session.currentProgress
            val updatedProgress = when (session.mode) {
                QuizMode.PRACTICE, QuizMode.TIMED_PRACTICE, QuizMode.REVISION -> {
                    if (progress.isLocked) {
                        progress
                    } else {
                        val wasUnanswered = !progress.isAnswered
                        if (wasUnanswered) {
                            repository.recordAnswer(index == question.correctOptionIndex)
                        }
                        progress.copy(
                            selectedOptionIndex = index,
                            isVisited = true,
                            isLocked = true,
                            isTimedOut = false
                        )
                    }
                }

                else -> {
                    progress.copy(
                        selectedOptionIndex = index,
                        isVisited = true
                    )
                }
            }
            state.copy(
                activeSession = session.withProgress(updatedProgress),
                progress = repository.getProgressStats()
            )
        }
    }

    fun clearResponse() {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            if (session.mode == QuizMode.PRACTICE) return@update state
            if (session.mode == QuizMode.TIMED_PRACTICE && session.currentProgress.isLocked) return@update state
            val updatedProgress = session.currentProgress.copy(
                selectedOptionIndex = null,
                isVisited = true
            )
            state.copy(activeSession = session.withProgress(updatedProgress))
        }
    }

    fun saveAndNext() {
        moveToNextQuestion(markForReview = false)
    }

    fun markForReviewAndNext() {
        moveToNextQuestion(markForReview = true)
    }

    private fun moveToNextQuestion(markForReview: Boolean) {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            val currentProgress = session.currentProgress
            val updatedCurrent = currentProgress.copy(
                isVisited = true,
                isMarkedForReview = if (markForReview) true else currentProgress.isMarkedForReview
            )
            val updatedSession = session.withProgress(updatedCurrent)
            val nextIndex = (updatedSession.currentIndex + 1) % updatedSession.set.questions.size
            val nextQuestionId = updatedSession.set.questions[nextIndex].id
            val nextProgress = updatedSession.questionProgress[nextQuestionId] ?: QuestionProgress(
                questionId = nextQuestionId,
                isVisited = true
            )
            state.copy(
                activeSession = updatedSession.copy(
                    currentIndex = nextIndex,
                    questionProgress = updatedSession.questionProgress + (nextQuestionId to nextProgress.copy(isVisited = true))
                )
            )
        }
    }

    fun previousQuestion() {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            val previousIndex = if (session.currentIndex == 0) {
                session.set.questions.lastIndex
            } else {
                session.currentIndex - 1
            }
            val questionId = session.set.questions[previousIndex].id
            val previousProgress = session.questionProgress[questionId] ?: QuestionProgress(
                questionId = questionId,
                isVisited = true
            )
            state.copy(
                activeSession = session.copy(
                    currentIndex = previousIndex,
                    questionProgress = session.questionProgress + (questionId to previousProgress.copy(isVisited = true))
                )
            )
        }
    }

    fun revisitQuestion(index: Int) {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            val questionId = session.set.questions[index].id
            val updatedProgress = session.questionProgress[questionId]?.copy(isVisited = true)
                ?: QuestionProgress(questionId = questionId, isVisited = true)
            state.copy(
                activeSession = session.copy(
                    currentIndex = index,
                    questionProgress = session.questionProgress + (questionId to updatedProgress)
                )
            )
        }
    }

    fun toggleBookmark() {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            val updated = session.currentProgress.copy(
                isBookmarked = !session.currentProgress.isBookmarked,
                isVisited = true
            )
            state.copy(activeSession = session.withProgress(updated))
        }
    }

    fun timeOutCurrentQuestion() {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            if (session.mode != QuizMode.TIMED_PRACTICE) return@update state
            val progress = session.currentProgress
            if (progress.isLocked) return@update state
            val updated = progress.copy(
                isVisited = true,
                isLocked = true,
                isTimedOut = true
            )
            state.copy(activeSession = session.withProgress(updated))
        }
    }

    fun submitExam(timeTakenSeconds: Int) {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            repository.recordBatchResults(
                correct = session.correctCount,
                wrong = session.incorrectCount
            )
            val summary = buildSummary(session, timeTakenSeconds)
            repository.saveQuizHistory(summary.toHistoryItem(session))
            state.copy(
                activeSession = null,
                latestSummary = summary,
                reviewFilter = ReviewFilter.ALL,
                progress = repository.getProgressStats(),
                recentHistory = repository.getRecentQuizHistory(),
                preferences = repository.getSavedPreferences()
            )
        }
    }

    fun finishPractice() {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            val summary = buildSummary(session, 0)
            repository.saveQuizHistory(summary.toHistoryItem(session))
            state.copy(
                activeSession = null,
                latestSummary = summary,
                reviewFilter = ReviewFilter.ALL,
                recentHistory = repository.getRecentQuizHistory(),
                preferences = repository.getSavedPreferences()
            )
        }
    }

    fun leaveQuiz() {
        _uiState.update { it.copy(activeSession = null) }
    }

    fun clearSummary() {
        _uiState.update { it.copy(latestSummary = null, reviewFilter = ReviewFilter.ALL) }
    }

    fun setReviewFilter(filter: ReviewFilter) {
        _uiState.update { it.copy(reviewFilter = filter) }
    }

    private fun QuizSession.withProgress(progress: QuestionProgress): QuizSession {
        return copy(
            questionProgress = questionProgress + (progress.questionId to progress)
        )
    }

    private fun buildSummary(session: QuizSession, timeTakenSeconds: Int): QuizSummary {
        val reviewItems = session.set.questions.map { question ->
            val progress = session.questionProgress[question.id] ?: QuestionProgress(questionId = question.id)
            QuestionReviewItem(
                question = question,
                selectedOptionIndex = progress.selectedOptionIndex,
                isMarkedForReview = progress.isMarkedForReview,
                isBookmarked = progress.isBookmarked
            )
        }
        val correct = reviewItems.count { it.isCorrect && !it.isSkipped }
        val wrong = reviewItems.count { !it.isCorrect && !it.isSkipped }
        val skipped = reviewItems.count { it.isSkipped }
        val attempted = correct + wrong
        val score = when (session.mode) {
            QuizMode.EXAM, QuizMode.SECTIONAL -> (correct * 4) - wrong
            else -> correct
        }
        return QuizSummary(
            setTitle = session.set.title,
            mode = session.mode,
            attempted = attempted,
            correct = correct,
            wrong = wrong,
            skipped = skipped,
            markedForReview = reviewItems.count { it.isMarkedForReview },
            totalQuestions = session.set.questions.size,
            score = score,
            accuracy = if (attempted == 0) 0 else ((correct * 100f) / attempted).toInt(),
            timeTakenSeconds = timeTakenSeconds,
            reviewItems = reviewItems
        )
    }

    private fun QuizSummary.toHistoryItem(session: QuizSession): QuizHistoryItem {
        return QuizHistoryItem(
            setId = session.set.id,
            setTitle = setTitle,
            mode = mode,
            attempted = attempted,
            correct = correct,
            wrong = wrong,
            skipped = skipped,
            markedForReview = markedForReview,
            totalQuestions = totalQuestions,
            score = score,
            accuracy = accuracy,
            timeTakenSeconds = timeTakenSeconds,
            completedAt = System.currentTimeMillis()
        )
    }
}

class ExamPrepViewModelFactory(
    private val repository: ExamPrepRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamPrepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamPrepViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
