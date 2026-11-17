package com.example.data.local

import androidx.room.TypeConverter

/**
 * Room TypeConverters for local SQLite persistence.
 * Converts complex types (such as lists of date strings) to and from flat strings.
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value.isNullOrEmpty()) return ""
        return value.joinToString(separator = "|")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("|").filter { it.isNotBlank() }
    }
}
