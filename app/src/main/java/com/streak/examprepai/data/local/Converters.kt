package com.streak.examprepai.data.local

import androidx.room.TypeConverter
import com.streak.examprepai.data.QuestionProgress
import com.streak.examprepai.data.QuizMode

class Converters {

    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString("||")

    @TypeConverter
    fun toList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split("||").filter { it.isNotBlank() }
    }

    @TypeConverter
    fun fromQuizMode(value: QuizMode): String = value.name

    @TypeConverter
    fun toQuizMode(value: String): QuizMode = QuizMode.valueOf(value)

    @TypeConverter
    fun fromQuestionProgressList(value: List<QuestionProgress>): String {
        return value.joinToString(";;") { progress ->
            listOf(
                progress.questionId,
                progress.isVisited.toString(),
                progress.selectedOptionIndex?.toString().orEmpty(),
                progress.isLocked.toString(),
                progress.isTimedOut.toString(),
                progress.isMarkedForReview.toString(),
                progress.isBookmarked.toString()
            ).joinToString("|")
        }
    }

    @TypeConverter
    fun toQuestionProgressList(value: String): List<QuestionProgress> {
        if (value.isBlank()) return emptyList()
        return value.split(";;").filter { it.isNotBlank() }.map { row ->
            val parts = row.split("|")
            QuestionProgress(
                questionId = parts[0],
                isVisited = parts.getOrElse(1) { "false" }.toBoolean(),
                selectedOptionIndex = parts.getOrElse(2) { "" }.ifBlank { null }?.toInt(),
                isLocked = parts.getOrElse(3) { "false" }.toBoolean(),
                isTimedOut = parts.getOrElse(4) { "false" }.toBoolean(),
                isMarkedForReview = parts.getOrElse(5) { "false" }.toBoolean(),
                isBookmarked = parts.getOrElse(6) { "false" }.toBoolean()
            )
        }
    }
}
