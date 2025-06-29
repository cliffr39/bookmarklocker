package com.bookmarklocker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * BookmarkDao: Data Access Object (DAO) for the Bookmark entity.
 * This interface defines the methods that can be used to interact with the 'bookmarks' table
 * in the Room database.
 *
 * Updated in Step 6 to include:
 * - `update`: For modifying existing bookmarks.
 * - `delete`: For removing bookmarks.
 * - `getBookmarksByCollection`: To retrieve bookmarks filtered by a specific collection ID.
 * 
 * Updated in Step 7 to include:
 * - `getAllUniqueTags`: To retrieve all unique tags used across all bookmarks.
 * - `updateBookmarkTags`: To update tags for a specific bookmark.
 * - `getBookmarksByTag`: To retrieve bookmarks that contain a specific tag.
 */
@Dao
interface BookmarkDao {

    /**
     * Inserts a new bookmark into the database.
     * If a bookmark with the same primary key already exists, it will be replaced.
     *
     * @param bookmark The Bookmark object to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark)

    /**
     * Updates an existing bookmark in the database.
     * The bookmark to be updated is identified by its primary key (id).
     *
     * @param bookmark The Bookmark object to be updated.
     */
    @Update
    suspend fun update(bookmark: Bookmark)

    /**
     * Deletes a specific bookmark from the database.
     * The bookmark to be deleted is identified by its primary key (id).
     *
     * @param bookmark The Bookmark object to be deleted.
     */
    @Delete
    suspend fun delete(bookmark: Bookmark)

    /**
     * Retrieves all bookmarks from the database, ordered by their creation timestamp in descending order.
     * The result is returned as a Flow, which means any changes to the data in the database
     * will automatically be emitted to collectors of this Flow.
     *
     * @return A Flow emitting a list of all Bookmark objects.
     */
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    /**
     * Retrieves bookmarks associated with a specific collection ID, ordered by timestamp.
     * @param collectionId The ID of the collection to filter by. If null, returns bookmarks not in any collection.
     * @return A Flow emitting a list of Bookmark objects belonging to the specified collection.
     */
    @Query("SELECT * FROM bookmarks WHERE collection_id = :collectionId ORDER BY timestamp DESC")
    fun getBookmarksByCollection(collectionId: Int?): Flow<List<Bookmark>>

    /**
     * Deletes all bookmarks from the database.
     * Used during restore operations to clear existing data before importing.
     */
    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()

    /**
     * Retrieves all unique tags used across all bookmarks.
     * This method will be used by the TagsManagementFragment to display all available tags.
     * 
     * @return A Flow emitting a list of unique tag strings.
     */
    @Query("SELECT DISTINCT tags FROM bookmarks WHERE tags IS NOT NULL AND tags != '[]'")
    fun getAllUniqueTags(): Flow<List<String>>

    /**
     * Retrieves bookmarks that contain a specific tag.
     * This method will be used to find all bookmarks that use a particular tag.
     * 
     * @param tag The tag to search for.
     * @return A Flow emitting a list of Bookmark objects that contain the specified tag.
     */
    @Query("SELECT * FROM bookmarks WHERE tags LIKE '%\"' || :tag || '\"%' ORDER BY timestamp DESC")
    fun getBookmarksByTag(tag: String): Flow<List<Bookmark>>
}
