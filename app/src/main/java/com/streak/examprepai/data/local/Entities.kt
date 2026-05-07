package com.streak.examprepai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.streak.examprepai.data.ProgressStats
import com.streak.examprepai.data.QuestionProgress
import com.streak.examprepai.data.QuizHistoryItem
import com.streak.examprepai.data.QuizMode
import com.streak.examprepai.data.SavedSession
import com.streak.examprepai.data.UserPreferences

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 0,
    val examId: String,
    val examName: String,
    val subjectIds: List<String>,
    val subjectNames: List<String>
) {
    fun toDomain(): UserPreferences {
        return UserPreferences(
            examId = examId,
            examName = examName,
            subjectIds = subjectIds,
            subjectNames = subjectNames
        )
    }

    companion object {
        fun fromDomain(domain: UserPreferences): UserPreferencesEntity {
            return UserPreferencesEntity(
                examId = domain.examId,
                examName = domain.examName,
                subjectIds = domain.subjectIds,
                subjectNames = domain.subjectNames
            )
        }
    }
}

@Entity(tableName = "progress_stats")
data class ProgressStatsEntity(
    @PrimaryKey val id: Int = 0,
    val attempted: Int,
    val correct: Int,
    val wrong: Int
) {
    fun toDomain(): ProgressStats {
        return ProgressStats(
            attempted = attempted,
            correct = correct,
            wrong = wrong
        )
    }

    companion object {
        fun fromDomain(domain: ProgressStats): ProgressStatsEntity {
            return ProgressStatsEntity(
                attempted = domain.attempted,
                correct = domain.correct,
                wrong = domain.wrong
            )
        }
    }
}

@Entity(tableName = "quiz_history")
data class QuizHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
) {
    fun toDomain(): QuizHistoryItem {
        return QuizHistoryItem(
            id = id,
            setId = setId,
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
            completedAt = completedAt
        )
    }

    companion object {
        fun fromDomain(domain: QuizHistoryItem): QuizHistoryEntity {
            return QuizHistoryEntity(
                id = domain.id,
                setId = domain.setId,
                setTitle = domain.setTitle,
                mode = domain.mode,
                attempted = domain.attempted,
                correct = domain.correct,
                wrong = domain.wrong,
                skipped = domain.skipped,
                markedForReview = domain.markedForReview,
                totalQuestions = domain.totalQuestions,
                score = domain.score,
                accuracy = domain.accuracy,
                timeTakenSeconds = domain.timeTakenSeconds,
                completedAt = domain.completedAt
            )
        }
    }
}

@Entity(tableName = "saved_session")
data class SavedSessionEntity(
    @PrimaryKey val id: Int = 0,
    val sessionId: Long,
    val mode: QuizMode,
    val setId: String,
    val currentIndex: Int,
    val questionProgress: List<QuestionProgress>,
    val timedPracticeSecondsPerQuestion: Int
) {
    fun toDomain(): SavedSession {
        return SavedSession(
            sessionId = sessionId,
            mode = mode,
            setId = setId,
            currentIndex = currentIndex,
            questionProgress = questionProgress,
            timedPracticeSecondsPerQuestion = timedPracticeSecondsPerQuestion
        )
    }

    companion object {
        fun fromDomain(domain: SavedSession): SavedSessionEntity {
            return SavedSessionEntity(
                sessionId = domain.sessionId,
                mode = domain.mode,
                setId = domain.setId,
                currentIndex = domain.currentIndex,
                questionProgress = domain.questionProgress,
                timedPracticeSecondsPerQuestion = domain.timedPracticeSecondsPerQuestion
            )
        }
    }
}

@Entity(tableName = "question_insight")
data class QuestionInsightEntity(
    @PrimaryKey val questionId: String,
    val setId: String,
    val subjectId: String,
    val wrongCount: Int,
    val isBookmarked: Boolean
)
