package com.bookmarklocker.data

import kotlinx.coroutines.flow.Flow

/**
 * CollectionRepository: A repository class that abstracts the data sources for collections.
 * This class provides a clean API for the CollectionViewModel to access data,
 * effectively isolating the ViewModel from the underlying data source (Room database).
 * It handles the logic for interacting with the CollectionDao.
 */
class CollectionRepository(private val collectionDao: CollectionDao) {

    /**
     * Retrieves all collections from the database.
     * This method directly exposes the Flow from the CollectionDao, allowing the UI (ViewModel)
     * to observe changes in the database in real-time.
     *
     * @return A Flow emitting a list of all Collection objects.
     */
    val allCollections: Flow<List<Collection>> = collectionDao.getAllCollections()

    /**
     * Inserts a new collection into the database.
     * This is a suspend function, meaning it must be called from a coroutine or another suspend function.
     *
     * @param collection The Collection object to be inserted.
     */
    suspend fun insert(collection: Collection) {
        collectionDao.insert(collection)
    }

    /**
     * Updates an existing collection in the database.
     * This is a suspend function.
     *
     * @param collection The Collection object to be updated.
     */
    suspend fun update(collection: Collection) {
        collectionDao.update(collection)
    }
}
