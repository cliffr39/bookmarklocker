package com.bookmarklocker.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * BackupData: A data class to encapsulate all application data (Bookmarks and Collections)
 * for the purpose of backup and restore operations.
 * This class will be serialized to JSON when backing up and deserialized from JSON when restoring.
 *
 * @param bookmarks A list of all Bookmark objects.
 * @param collections A list of all Collection objects.
 */
data class BackupData(
    @SerializedName("bookmarks") // Annotation to specify the JSON field name
    val bookmarks: List<Bookmark>,
    @SerializedName("collections") // Annotation to specify the JSON field name
    val collections: List<Collection>
) : Serializable // Make BackupData class Serializable
