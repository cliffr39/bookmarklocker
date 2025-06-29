package com.bookmarklocker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // ADDED: Import TypeConverters annotation

/**
 * BookmarkDatabase: The Room database class for the Bookmark Locker application.
 * This abstract class serves as the main access point for the underlying SQLite database.
 * It defines the entities (tables) included in the database and provides abstract methods
 * to access the DAOs (Data Access Objects).
 *
 * Updated in Step 6 to:
 * - Include the 'Collection' entity and its DAO.
 * - Increment the database version to 3, requiring a migration strategy (using destructive migration for now).
 *
 * Updated in Step 7 to register the `Converters` class for handling `List<String>` for tags.
 */
@Database(entities = [Bookmark::class, Collection::class], version = 3, exportSchema = false)
// entities: Includes both Bookmark and Collection.
// version: Version 3 for new schema changes (tags, collectionId)
// exportSchema: Set to false to avoid exporting schema to a folder.
@TypeConverters(Converters::class) // ADDED: Register the Converters class for type conversions
abstract class BookmarkDatabase : RoomDatabase() {

    // Abstract method to get an instance of the BookmarkDao.
    abstract fun bookmarkDao(): BookmarkDao

    // Abstract method to get an instance of the new CollectionDao.
    abstract fun collectionDao(): CollectionDao

    companion object {
        // Singleton instance of the database to prevent multiple instances of the database opening
        // at the same time, which can lead to performance issues or crashes.
        @Volatile
        private var INSTANCE: BookmarkDatabase? = null

        /**
         * Provides the singleton instance of the BookmarkDatabase.
         * If the instance does not exist, it creates one using Room.databaseBuilder.
         *
         * @param context The application context, used to build the database.
         * @return The singleton instance of BookmarkDatabase.
         */
        fun getDatabase(context: Context): BookmarkDatabase {
            // If INSTANCE is not null, then return it,
            // otherwise create a new instance of the database.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, // Use application context to prevent memory leaks
                    BookmarkDatabase::class.java, // The database class
                    "bookmark_database" // The name of the database file
                )
                // IMPORTANT: For development, to easily handle schema changes, you can use:
                .fallbackToDestructiveMigration() // Allows Room to recreate database if schema changes.
                                                 // WARNING: This deletes all existing data on schema change.
                                                 // For a real app, you MUST implement proper migrations!
                // Add any migrations here if your schema changes in the future for production apps.
                // .addMigrations(MIGRATION_1_2) // Example migration
                .build()
                INSTANCE = instance // Assign the created instance to INSTANCE
                instance // Return the instance
            }
        }
    }
}
