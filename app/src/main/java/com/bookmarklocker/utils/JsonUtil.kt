package com.bookmarklocker.utils

import com.bookmarklocker.data.BackupData
import com.google.gson.Gson // Import Gson
import com.google.gson.GsonBuilder // Import GsonBuilder for pretty printing

/**
 * JsonUtil: A utility object for handling JSON serialization and deserialization
 * of application data (BackupData). It uses the Gson library.
 */
object JsonUtil {

    // Create a Gson instance for JSON processing. Use GsonBuilder to enable pretty printing for readability.
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Serializes a BackupData object into a JSON string.
     * @param data The BackupData object to serialize.
     * @return The JSON string representation of the data.
     */
    fun serializeBackupData(data: BackupData): String {
        return gson.toJson(data)
    }

    /**
     * Deserializes a JSON string into a BackupData object.
     * @param jsonString The JSON string to deserialize.
     * @return The deserialized BackupData object, or null if deserialization fails.
     */
    fun deserializeBackupData(jsonString: String): BackupData? {
        return try {
            gson.fromJson(jsonString, BackupData::class.java)
        } catch (e: Exception) {
            e.printStackTrace() // Print stack trace for debugging deserialization errors
            null
        }
    }
}
