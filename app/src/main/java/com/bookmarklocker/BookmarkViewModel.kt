package com.bookmarklocker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bookmarklocker.data.Bookmark
import com.bookmarklocker.data.BookmarkRepository
import kotlinx.coroutines.launch

/**
 * BookmarkViewModel: A ViewModel responsible for holding and managing UI-related data
 * for the Bookmarks screen in a lifecycle-aware manner. It interacts with the BookmarkRepository
 * to perform database operations and exposes data to the UI via LiveData.
 *
 * Updated in Step 6 to include:
 * - `update`: For modifying existing bookmarks.
 * - `delete`: For removing bookmarks.
 */
class BookmarkViewModel(private val repository: BookmarkRepository) : ViewModel() {

    // LiveData that holds a list of all bookmarks.
    // .asLiveData() converts the Flow<List<Bookmark>> from the repository into LiveData,
    // making it observable by the UI. Any changes in the database will automatically
    // update this LiveData.
    val allBookmarks = repository.allBookmarks.asLiveData()

    /**
     * Inserts a new bookmark into the database.
     * This operation is launched in a coroutine within the viewModelScope,
     * ensuring it's cancelled when the ViewModel is cleared.
     *
     * @param bookmark The Bookmark object to be inserted.
     */
    fun insert(bookmark: Bookmark) = viewModelScope.launch {
        repository.insert(bookmark)
    }

    /**
     * Updates an existing bookmark in the database.
     *
     * @param bookmark The Bookmark object to be updated.
     */
    fun update(bookmark: Bookmark) = viewModelScope.launch {
        repository.update(bookmark)
    }

    /**
     * Deletes a specific bookmark from the database.
     *
     * @param bookmark The Bookmark object to be deleted.
     */
    fun delete(bookmark: Bookmark) = viewModelScope.launch {
        repository.delete(bookmark)
    }

    fun getBookmarksByCollection(collectionId: Int) = repository.getBookmarksByCollection(collectionId).asLiveData()
}

/**
 * BookmarkViewModelFactory: A custom ViewModelProvider.Factory for creating instances
 * of BookmarkViewModel. This is necessary because BookmarkViewModel has a constructor
 * that takes a BookmarkRepository as a parameter.
 */
class BookmarkViewModelFactory(private val repository: BookmarkRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Checks if the requested ViewModel class is BookmarkViewModel.
        if (modelClass.isAssignableFrom(BookmarkViewModel::class.java)) {
            // If it is, create and return a new instance of BookmarkViewModel.
            @Suppress("UNCHECKED_CAST")
            return BookmarkViewModel(repository) as T
        }
        // If the ViewModel class is not recognized, throw an IllegalArgumentException.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
