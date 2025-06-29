package com.bookmarklocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * CollectionDao: Data Access Object (DAO) for the Collection entity.
 * This interface defines the methods that can be used to interact with the 'collections' table
 * in the Room database. It provides methods for inserting, updating, and retrieving collections.
 *
 * Updated in Step 5 to include a deleteAll method for restore operations.
 */
@Dao
interface CollectionDao {

    /**
     * Inserts a new collection into the database.
     * If a collection with the same primary key already exists, it will be replaced.
     *
     * @param collection The Collection object to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: Collection)

    /**
     * Updates an existing collection in the database.
     *
     * @param collection The Collection object to be updated. It must have a matching ID.
     */
    @Update
    suspend fun update(collection: Collection)

    /**
     * Retrieves all collections from the database, ordered by their creation timestamp in descending order.
     * The result is returned as a Flow, which means any changes to the data in the database
     * will automatically be emitted to collectors of this Flow.
     *
     * @return A Flow emitting a list of all Collection objects.
     */
    @Query("SELECT * FROM collections ORDER BY created_at DESC")
    fun getAllCollections(): Flow<List<Collection>>

    /**
     * Deletes all collections from the database.
     * Used during restore operations to clear existing data before importing.
     */
    @Query("DELETE FROM collections")
    suspend fun deleteAll()
}
