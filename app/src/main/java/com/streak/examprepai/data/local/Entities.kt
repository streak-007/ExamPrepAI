package com.streak.examprepai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.streak.examprepai.data.ProgressStats
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
