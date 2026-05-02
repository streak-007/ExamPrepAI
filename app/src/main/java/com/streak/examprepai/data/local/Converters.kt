package com.streak.examprepai.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString("||")

    @TypeConverter
    fun toList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value.split("||").filter { it.isNotBlank() }
    }
}
