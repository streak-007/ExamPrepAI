package com.streak.examprepai.data

data class Exam(
    val id: String,
    val name: String,
    val tagline: String,
    val subjects: List<Subject>
)

data class Subject(
    val id: String,
    val name: String
)

data class QuestionSet(
    val id: String,
    val title: String,
    val subjectId: String,
    val difficulty: String,
    val estimatedMinutes: Int,
    val isPremium: Boolean,
    val questions: List<Question>
)

data class Question(
    val id: String,
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
    val reference: String,
    val commonMistake: String
)

data class UserPreferences(
    val examId: String = "",
    val examName: String = "",
    val subjectIds: List<String> = emptyList(),
    val subjectNames: List<String> = emptyList()
)

data class ProgressStats(
    val attempted: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0
) {
    val accuracy: Int
        get() = if (attempted == 0) 0 else ((correct * 100f) / attempted).toInt()
}

data class QuizHistoryItem(
    val id: Long = 0L,
    val setId: String,
    val setTitle: String,
    val mode: QuizMode,
    val attempted: Int,
    val correct: Int,
    val wrong: Int,
    val skipped: Int,
    val markedForReview: Int,
    val totalQuestions: Int,
    val score: Int,
    val accuracy: Int,
    val timeTakenSeconds: Int,
    val completedAt: Long
)

data class SavedSession(
    val sessionId: Long,
    val mode: QuizMode,
    val setId: String,
    val currentIndex: Int,
    val questionProgress: List<QuestionProgress>,
    val timedPracticeSecondsPerQuestion: Int
)

data class PracticeRecommendation(
    val questionSet: QuestionSet,
    val latestAccuracy: Int,
    val attempts: Int,
    val wrongAnswers: Int,
    val markedForReview: Int
)

data class StreakInfo(
    val currentDays: Int = 0,
    val longestDays: Int = 0,
    val practicedToday: Boolean = false
)

data class SubjectPerformance(
    val subjectId: String,
    val subjectName: String,
    val attempts: Int,
    val correct: Int,
    val wrong: Int,
    val latestAccuracy: Int
) {
    val accuracy: Int
        get() = if (attempts == 0) 0 else ((correct * 100f) / attempts).toInt()
}

enum class QuizMode {
    PRACTICE,
    EXAM,
    TIMED_PRACTICE,
    SECTIONAL,
    REVISION
}

enum class PaletteState {
    ANSWERED,
    NOT_ANSWERED,
    MARKED,
    ANSWERED_AND_MARKED,
    NOT_VISITED
}

enum class ReviewFilter {
    ALL,
    CORRECT,
    INCORRECT,
    SKIPPED,
    MARKED
}

data class QuestionProgress(
    val questionId: String,
    val isVisited: Boolean = false,
    val selectedOptionIndex: Int? = null,
    val isLocked: Boolean = false,
    val isTimedOut: Boolean = false,
    val isMarkedForReview: Boolean = false,
    val isBookmarked: Boolean = false
) {
    val isAnswered: Boolean
        get() = selectedOptionIndex != null

    val paletteState: PaletteState
        get() = when {
            isAnswered && isMarkedForReview -> PaletteState.ANSWERED_AND_MARKED
            isMarkedForReview -> PaletteState.MARKED
            isAnswered -> PaletteState.ANSWERED
            isVisited -> PaletteState.NOT_ANSWERED
            else -> PaletteState.NOT_VISITED
        }
}

data class QuizSession(
    val sessionId: Long,
    val mode: QuizMode,
    val set: QuestionSet,
    val currentIndex: Int = 0,
    val questionProgress: Map<String, QuestionProgress> = emptyMap(),
    val timedPracticeSecondsPerQuestion: Int = 60
) {
    val currentQuestion: Question
        get() = set.questions[currentIndex]

    val currentProgress: QuestionProgress
        get() = questionProgress[currentQuestion.id] ?: QuestionProgress(questionId = currentQuestion.id)

    val answeredCount: Int
        get() = questionProgress.values.count { it.isAnswered }

    val answeredOnlyCount: Int
        get() = questionProgress.values.count { it.paletteState == PaletteState.ANSWERED }

    val answeredAndMarkedCount: Int
        get() = questionProgress.values.count { it.paletteState == PaletteState.ANSWERED_AND_MARKED }

    val bookmarkedCount: Int
        get() = questionProgress.values.count { it.isBookmarked }

    val markedCount: Int
        get() = questionProgress.values.count { it.isMarkedForReview }

    val markedOnlyCount: Int
        get() = questionProgress.values.count { it.paletteState == PaletteState.MARKED }

    val correctCount: Int
        get() = set.questions.count { question ->
            questionProgress[question.id]?.selectedOptionIndex == question.correctOptionIndex
        }

    val incorrectCount: Int
        get() = set.questions.count { question ->
            val answer = questionProgress[question.id]?.selectedOptionIndex
            answer != null && answer != question.correctOptionIndex
        }

    val skippedCount: Int
        get() = questionProgress.values.count { it.isVisited && !it.isAnswered }

    val notAnsweredCount: Int
        get() = questionProgress.values.count { it.paletteState == PaletteState.NOT_ANSWERED }

    val notVisitedCount: Int
        get() = set.questions.count { question ->
            !(questionProgress[question.id]?.isVisited ?: false)
        }

    val allQuestionsVisited: Boolean
        get() = set.questions.all { question -> questionProgress[question.id]?.isVisited == true }
}

data class QuizSummary(
    val setTitle: String,
    val mode: QuizMode,
    val attempted: Int,
    val correct: Int,
    val wrong: Int,
    val skipped: Int,
    val markedForReview: Int,
    val totalQuestions: Int,
    val score: Int,
    val accuracy: Int,
    val timeTakenSeconds: Int,
    val reviewItems: List<QuestionReviewItem>
)

data class QuestionReviewItem(
    val question: Question,
    val selectedOptionIndex: Int?,
    val isMarkedForReview: Boolean,
    val isBookmarked: Boolean
) {
    val isCorrect: Boolean
        get() = selectedOptionIndex == question.correctOptionIndex

    val isSkipped: Boolean
        get() = selectedOptionIndex == null
}
