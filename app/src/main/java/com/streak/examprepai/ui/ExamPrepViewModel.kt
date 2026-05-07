package com.streak.examprepai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.streak.examprepai.data.Exam
import com.streak.examprepai.data.ExamPrepRepository
import com.streak.examprepai.data.ProgressStats
import com.streak.examprepai.data.PracticeRecommendation
import com.streak.examprepai.data.QuestionProgress
import com.streak.examprepai.data.QuestionReviewItem
import com.streak.examprepai.data.QuestionSet
import com.streak.examprepai.data.QuizHistoryItem
import com.streak.examprepai.data.QuizMode
import com.streak.examprepai.data.QuizSession
import com.streak.examprepai.data.QuizSummary
import com.streak.examprepai.data.ReviewFilter
import com.streak.examprepai.data.SavedSession
import com.streak.examprepai.data.StreakInfo
import com.streak.examprepai.data.Subject
import com.streak.examprepai.data.SubjectPerformance
import com.streak.examprepai.data.UserPreferences
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    val streakInfo: StreakInfo = StreakInfo(),
    val subjectPerformance: List<SubjectPerformance> = emptyList(),
    val weakAreaRecommendations: List<PracticeRecommendation> = emptyList(),
    val revisionQuestionCounts: Map<String, Int> = emptyMap(),
    val resumableSession: QuizSession? = null,
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
        val resumableSession = repository.getSavedSession()?.toQuizSession()
        _uiState.update {
            val availableSets = if (selectedSubjectIds.isEmpty()) {
                emptyList()
            } else {
                repository.getQuestionSetsForSubjects(selectedSubjectIds.toList())
            }
            it.copy(
                selectedExam = selectedExam,
                selectedSubjectIds = selectedSubjectIds,
                resumableSession = resumableSession,
                availableSets = availableSets,
                revisionQuestionCounts = buildRevisionCounts(availableSets),
                streakInfo = buildStreakInfo(it.recentHistory),
                subjectPerformance = buildSubjectPerformance(
                    recentHistory = it.recentHistory,
                    subjects = selectedExam?.subjects?.filter { subject -> subject.id in selectedSubjectIds }.orEmpty(),
                    availableSets = availableSets
                ),
                weakAreaRecommendations = buildWeakAreaRecommendations(
                    recentHistory = it.recentHistory,
                    availableSets = availableSets
                )
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
            val availableSets = repository.getQuestionSetsForSubjects(preferences.subjectIds)
            it.copy(
                preferences = preferences,
                availableSets = availableSets,
                revisionQuestionCounts = buildRevisionCounts(availableSets),
                streakInfo = buildStreakInfo(it.recentHistory),
                subjectPerformance = buildSubjectPerformance(
                    recentHistory = it.recentHistory,
                    subjects = subjects,
                    availableSets = availableSets
                ),
                weakAreaRecommendations = buildWeakAreaRecommendations(
                    recentHistory = it.recentHistory,
                    availableSets = availableSets
                )
            )
        }
    }

    fun startQuiz(questionSet: QuestionSet, mode: QuizMode) {
        val resolvedSet = if (mode == QuizMode.REVISION) {
            repository.getRevisionQuestionSet(questionSet.id) ?: return
        } else {
            questionSet
        }
        val firstQuestion = resolvedSet.questions.first()
        val initialProgress = QuestionProgress(
            questionId = firstQuestion.id,
            isVisited = true
        )
        val newSession = QuizSession(
            sessionId = ++sessionCounter,
            mode = mode,
            set = resolvedSet,
            questionProgress = mapOf(firstQuestion.id to initialProgress),
            timedPracticeSecondsPerQuestion = 60
        )
        persistSession(newSession)
        _uiState.update { state ->
            state.copy(
                activeSession = newSession,
                resumableSession = newSession,
                latestSummary = null,
                reviewFilter = ReviewFilter.ALL
            )
        }
    }

    fun resumeSavedSession() {
        _uiState.update { state ->
            val session = state.resumableSession ?: return@update state
            state.copy(
                activeSession = session,
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
                        repository.updateQuestionInsight(
                            setId = session.set.id.removeSuffix(" Revision"),
                            subjectId = session.set.subjectId,
                            questionId = question.id,
                            answeredCorrectly = index == question.correctOptionIndex
                        )
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
            persistSession(session.withProgress(updatedProgress))
            state.copy(
                activeSession = session.withProgress(updatedProgress),
                resumableSession = session.withProgress(updatedProgress),
                progress = repository.getProgressStats(),
                revisionQuestionCounts = buildRevisionCounts(state.availableSets)
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
            val updatedSession = session.withProgress(updatedProgress)
            persistSession(updatedSession)
            state.copy(
                activeSession = updatedSession,
                resumableSession = updatedSession,
                revisionQuestionCounts = buildRevisionCounts(state.availableSets)
            )
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
            val nextSession = updatedSession.copy(
                currentIndex = nextIndex,
                questionProgress = updatedSession.questionProgress + (nextQuestionId to nextProgress.copy(isVisited = true))
            )
            persistSession(nextSession)
            state.copy(
                activeSession = nextSession,
                resumableSession = nextSession
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
            val updatedSession = session.copy(
                currentIndex = previousIndex,
                questionProgress = session.questionProgress + (questionId to previousProgress.copy(isVisited = true))
            )
            persistSession(updatedSession)
            state.copy(
                activeSession = updatedSession,
                resumableSession = updatedSession
            )
        }
    }

    fun revisitQuestion(index: Int) {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            val questionId = session.set.questions[index].id
            val updatedProgress = session.questionProgress[questionId]?.copy(isVisited = true)
                ?: QuestionProgress(questionId = questionId, isVisited = true)
            val updatedSession = session.copy(
                currentIndex = index,
                questionProgress = session.questionProgress + (questionId to updatedProgress)
            )
            persistSession(updatedSession)
            state.copy(
                activeSession = updatedSession,
                resumableSession = updatedSession
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
            repository.updateQuestionInsight(
                setId = session.set.id.removeSuffix(" Revision"),
                subjectId = session.set.subjectId,
                questionId = session.currentQuestion.id,
                isBookmarked = updated.isBookmarked
            )
            val updatedSession = session.withProgress(updated)
            persistSession(updatedSession)
            state.copy(
                activeSession = updatedSession,
                resumableSession = updatedSession
            )
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
            val updatedSession = session.withProgress(updated)
            persistSession(updatedSession)
            state.copy(
                activeSession = updatedSession,
                resumableSession = updatedSession
            )
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
            syncQuestionInsightsFromSummary(session, summary)
            repository.saveQuizHistory(summary.toHistoryItem(session))
            repository.clearSavedSession()
            state.copy(
                activeSession = null,
                resumableSession = null,
                latestSummary = summary,
                reviewFilter = ReviewFilter.ALL,
                progress = repository.getProgressStats(),
                recentHistory = repository.getRecentQuizHistory()
            ).refreshRecommendations()
        }
    }

    fun finishPractice() {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            val summary = buildSummary(session, 0)
            syncQuestionInsightsFromSummary(session, summary)
            repository.saveQuizHistory(summary.toHistoryItem(session))
            repository.clearSavedSession()
            state.copy(
                activeSession = null,
                resumableSession = null,
                latestSummary = summary,
                reviewFilter = ReviewFilter.ALL,
                recentHistory = repository.getRecentQuizHistory()
            ).refreshRecommendations()
        }
    }

    fun leaveQuiz() {
        _uiState.update { state ->
            val session = state.activeSession ?: return@update state
            persistSession(session)
            state.copy(
                activeSession = null,
                resumableSession = session
            )
        }
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

    private fun persistSession(session: QuizSession) {
        repository.saveSession(
            SavedSession(
                sessionId = session.sessionId,
                mode = session.mode,
                setId = session.set.id,
                currentIndex = session.currentIndex,
                questionProgress = session.questionProgress.values.toList(),
                timedPracticeSecondsPerQuestion = session.timedPracticeSecondsPerQuestion
            )
        )
    }

    private fun SavedSession.toQuizSession(): QuizSession? {
        val set = repository.findQuestionSetById(setId) ?: return null
        sessionCounter = maxOf(sessionCounter, sessionId)
        val progressMap = questionProgress.associateBy { it.questionId }
        return QuizSession(
            sessionId = sessionId,
            mode = mode,
            set = set,
            currentIndex = currentIndex.coerceIn(0, set.questions.lastIndex),
            questionProgress = progressMap,
            timedPracticeSecondsPerQuestion = timedPracticeSecondsPerQuestion
        )
    }

    private fun ExamPrepUiState.refreshRecommendations(): ExamPrepUiState {
        return copy(
            streakInfo = buildStreakInfo(recentHistory),
            subjectPerformance = buildSubjectPerformance(
                recentHistory = recentHistory,
                subjects = selectedExam?.subjects?.filter { subject -> subject.id in selectedSubjectIds }.orEmpty(),
                availableSets = availableSets
            ),
            weakAreaRecommendations = buildWeakAreaRecommendations(
                recentHistory = recentHistory,
                availableSets = availableSets
            ),
            revisionQuestionCounts = buildRevisionCounts(availableSets)
        )
    }

    private fun buildRevisionCounts(availableSets: List<QuestionSet>): Map<String, Int> {
        return availableSets.associate { set ->
            set.id to repository.getRevisionQuestionCount(set.id)
        }
    }

    private fun syncQuestionInsightsFromSummary(session: QuizSession, summary: QuizSummary) {
        if (session.mode != QuizMode.EXAM && session.mode != QuizMode.SECTIONAL) {
            return
        }
        summary.reviewItems.forEach { item ->
            repository.updateQuestionInsight(
                setId = session.set.id.removeSuffix(" Revision"),
                subjectId = session.set.subjectId,
                questionId = item.question.id,
                answeredCorrectly = item.selectedOptionIndex?.let { it == item.question.correctOptionIndex },
                isBookmarked = item.isBookmarked
            )
        }
    }

    private fun buildSubjectPerformance(
        recentHistory: List<QuizHistoryItem>,
        subjects: List<Subject>,
        availableSets: List<QuestionSet>
    ): List<SubjectPerformance> {
        if (recentHistory.isEmpty() || subjects.isEmpty() || availableSets.isEmpty()) return emptyList()

        val setsById = availableSets.associateBy { it.id }
        val historyBySubject = recentHistory
            .mapNotNull { history ->
                val set = setsById[history.setId] ?: return@mapNotNull null
                set.subjectId to history
            }
            .groupBy({ it.first }, { it.second })

        return subjects.mapNotNull { subject ->
            val entries = historyBySubject[subject.id].orEmpty()
            if (entries.isEmpty()) return@mapNotNull null

            SubjectPerformance(
                subjectId = subject.id,
                subjectName = subject.name,
                attempts = entries.sumOf { it.attempted },
                correct = entries.sumOf { it.correct },
                wrong = entries.sumOf { it.wrong },
                latestAccuracy = entries.maxByOrNull { it.completedAt }?.accuracy ?: 0
            )
        }.sortedWith(
            compareBy<SubjectPerformance> { it.accuracy }
                .thenByDescending { it.wrong }
                .thenBy { it.subjectName }
        )
    }

    private fun buildWeakAreaRecommendations(
        recentHistory: List<QuizHistoryItem>,
        availableSets: List<QuestionSet>
    ): List<PracticeRecommendation> {
        if (recentHistory.isEmpty() || availableSets.isEmpty()) return emptyList()

        return recentHistory
            .filter { history -> availableSets.any { it.id == history.setId } }
            .sortedWith(compareBy<QuizHistoryItem> { it.accuracy }.thenByDescending { it.completedAt })
            .distinctBy { it.setId }
            .mapNotNull { history ->
                val set = availableSets.firstOrNull { it.id == history.setId } ?: return@mapNotNull null
                PracticeRecommendation(
                    questionSet = set,
                    latestAccuracy = history.accuracy,
                    attempts = history.attempted,
                    wrongAnswers = history.wrong,
                    markedForReview = history.markedForReview
                )
            }
            .take(3)
    }

    private fun buildStreakInfo(recentHistory: List<QuizHistoryItem>): StreakInfo {
        if (recentHistory.isEmpty()) return StreakInfo()

        val zoneId = ZoneId.systemDefault()
        val uniqueDates = recentHistory
            .map { history -> Instant.ofEpochMilli(history.completedAt).atZone(zoneId).toLocalDate() }
            .distinct()
            .sortedDescending()

        val today = LocalDate.now(zoneId)
        val practicedToday = today in uniqueDates
        val streakStart = if (practicedToday) today else today.minusDays(1)

        var currentDays = 0
        var cursor = streakStart
        while (cursor in uniqueDates) {
            currentDays += 1
            cursor = cursor.minusDays(1)
        }

        var longestDays = 0
        var runningDays = 0
        var previousDate: LocalDate? = null
        uniqueDates.sorted().forEach { date ->
            runningDays = if (previousDate != null && previousDate!!.plusDays(1) == date) {
                runningDays + 1
            } else {
                1
            }
            longestDays = maxOf(longestDays, runningDays)
            previousDate = date
        }

        return StreakInfo(
            currentDays = currentDays,
            longestDays = longestDays,
            practicedToday = practicedToday
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
