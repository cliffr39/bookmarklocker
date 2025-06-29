package com.bookmarklocker.data

import kotlinx.coroutines.flow.Flow

/**
 * BookmarkRepository: A repository class that abstracts the data sources.
 * This class provides a clean API for the BookmarkViewModel to access data,
 * effectively isolating the ViewModel from the underlying data source (Room database).
 * It handles the logic for interacting with the BookmarkDao.
 *
 * Updated in Step 6 to include `update` and `delete` operations for bookmarks.
 */
class BookmarkRepository(private val bookmarkDao: BookmarkDao) {

    /**
     * Retrieves all bookmarks from the database.
     * This method directly exposes the Flow from the BookmarkDao, allowing the UI (ViewModel)
     * to observe changes in the database in real-time.
     *
     * @return A Flow emitting a list of all Bookmark objects.
     */
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    /**
     * Inserts a new bookmark into the database.
     * This is a suspend function, meaning it must be called from a coroutine or another suspend function.
     *
     * @param bookmark The Bookmark object to be inserted.
     */
    suspend fun insert(bookmark: Bookmark) {
        bookmarkDao.insert(bookmark)
    }

    /**
     * Updates an existing bookmark in the database.
     *
     * @param bookmark The Bookmark object to be updated.
     */
    suspend fun update(bookmark: Bookmark) {
        bookmarkDao.update(bookmark)
    }

    /**
     * Deletes a specific bookmark from the database.
     *
     * @param bookmark The Bookmark object to be deleted.
     */
    suspend fun delete(bookmark: Bookmark) {
        bookmarkDao.delete(bookmark)
    }

    /**
     * Retrieves bookmarks associated with a specific collection ID.
     * @param collectionId The ID of the collection to filter by.
     * @return A Flow emitting a list of Bookmark objects belonging to the specified collection.
     */
    fun getBookmarksByCollection(collectionId: Int?): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByCollection(collectionId)
    }
}
