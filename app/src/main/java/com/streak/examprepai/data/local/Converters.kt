package com.streak.examprepai.data.local

import androidx.room.TypeConverter
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
}
