package com.bookmarklocker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bookmarklocker.data.Collection
import com.bookmarklocker.data.CollectionRepository
import kotlinx.coroutines.launch

/**
 * CollectionViewModel: A ViewModel responsible for holding and managing UI-related data
 * for the Collections screen in a lifecycle-aware manner. It interacts with the CollectionRepository
 * to perform database operations and exposes data to the UI via LiveData.
 */
class CollectionViewModel(private val repository: CollectionRepository) : ViewModel() {

    // LiveData that holds a list of all collections.
    // .asLiveData() converts the Flow<List<Collection>> from the repository into LiveData,
    // making it observable by the UI. Any changes in the database will automatically
    // update this LiveData.
    val allCollections = repository.allCollections.asLiveData()

    /**
     * Inserts a new collection into the database.
     * This operation is launched in a coroutine within the viewModelScope,
     * ensuring it's cancelled when the ViewModel is cleared.
     *
     * @param collection The Collection object to be inserted.
     */
    fun insert(collection: Collection) = viewModelScope.launch {
        repository.insert(collection)
    }

    /**
     * Updates an existing collection in the database.
     * This operation is launched in a coroutine within the viewModelScope.
     *
     * @param collection The Collection object to be updated.
     */
    fun update(collection: Collection) = viewModelScope.launch {
        repository.update(collection)
    }
}

/**
 * CollectionViewModelFactory: A custom ViewModelProvider.Factory for creating instances
 * of CollectionViewModel. This is necessary because CollectionViewModel has a constructor
 * that takes a CollectionRepository as a parameter.
 */
class CollectionViewModelFactory(private val repository: CollectionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Checks if the requested ViewModel class is CollectionViewModel.
        if (modelClass.isAssignableFrom(CollectionViewModel::class.java)) {
            // If it is, create and return a new instance of CollectionViewModel.
            @Suppress("UNCHECKED_CAST")
            return CollectionViewModel(repository) as T
        }
        // If the ViewModel class is not recognized, throw an IllegalArgumentException.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
