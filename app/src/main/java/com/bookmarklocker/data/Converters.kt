package com.bookmarklocker.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken // Import TypeToken for generic types

/**
 * Converters: A utility class for Room TypeConverters.
 * Room doesn't natively support storing complex objects or lists directly.
 * TypeConverters provide methods to convert complex types (like List<String>)
 * into types that Room can store (like String) and vice-versa.
 */
class Converters {

    // Gson instance for JSON serialization/deserialization
    private val gson = Gson()

    /**
     * Converts a List of Strings into a JSON string.
     * Used to store a list of tags in a single database column.
     * @param list The List<String> to convert.
     * @return A JSON string representing the list.
     */
    @TypeConverter // Marks this method as a Room TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    /**
     * Converts a JSON string back into a List of Strings.
     * Used to retrieve a list of tags from a database column.
     * @param value The JSON string to convert.
     * @return A List<String> deserialized from the JSON string.
     */
    @TypeConverter // Marks this method as a Room TypeConverter
    fun toStringList(value: String): List<String> {
        // Define the type for Gson to correctly deserialize List<String>
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
