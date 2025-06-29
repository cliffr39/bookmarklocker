package com.bookmarklocker

import android.os.Bundle
import android.util.Log // Import Log for debugging
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager // Using GridLayoutManager for collections
import com.bookmarklocker.data.BookmarkDatabase
import com.bookmarklocker.data.Collection
import com.bookmarklocker.data.CollectionRepository
import com.bookmarklocker.databinding.FragmentCollectionsBinding
import com.google.android.material.snackbar.Snackbar // For user feedback
import androidx.navigation.fragment.findNavController

/**
 * CollectionsFragment: This fragment is designed to display a list of user-defined collections (folders)
 * for organizing bookmarks. In Step 4, it integrates with the CollectionViewModel to observe and display
 * collections using a RecyclerView with a GridLayoutManager. It also handles adding new collections
 * and provides basic feedback for interactions.
 */
class CollectionsFragment : Fragment() {

    private val TAG = "CollectionsFragment" // Tag for Logcat messages

    // ViewBinding for safer access to UI elements in fragment_collections.xml
    private var _binding: FragmentCollectionsBinding? = null
    private val binding get() = _binding!!

    // Lazily initialize the CollectionViewModel using activityViewModels so it's scoped to the activity
    // and shared across fragments within the same activity.
    private val collectionViewModel: CollectionViewModel by activityViewModels {
        Log.d(TAG, "Initializing CollectionViewModelFactory for collections...")
        CollectionViewModelFactory(
            CollectionRepository(BookmarkDatabase.getDatabase(requireContext()).collectionDao())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called for CollectionsFragment.")
        // Inflate the layout for this fragment using ViewBinding
        _binding = FragmentCollectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called for CollectionsFragment.")

        // Initialize the CollectionAdapter for the RecyclerView.
        // Pass lambdas for item click and edit button click.
        val adapter = CollectionAdapter(
            onItemClick = { collection ->
                Log.d(TAG, "Collection clicked: ${collection.name}")
                // Navigate to CollectionDetailFragment using Bundle
                val bundle = Bundle().apply {
                    putInt("collectionId", collection.id)
                    putString("collectionName", collection.name)
                }
                findNavController().navigate(R.id.collectionDetailFragment, bundle)
            },
            onEditClick = { collection ->
                Log.d(TAG, "Edit icon clicked for collection: ${collection.name}")
                showAddEditCollectionDialog(collection) // Pass the collection to edit
            }
        )
        binding.collectionsRecyclerView.adapter = adapter
        // Use GridLayoutManager for a folder-like appearance, 1 column for wide cards
        binding.collectionsRecyclerView.layoutManager = GridLayoutManager(context, 1)


        // Observe changes in allCollections LiveData from the ViewModel.
        collectionViewModel.allCollections.observe(viewLifecycleOwner) { collections ->
            collections.let {
                Log.d(TAG, "Collections LiveData updated. Count: ${it.size}")
                adapter.submitList(it) // Update the RecyclerView with the new list of collections
                // Toggle visibility based on whether the collection list is empty
                if (it.isEmpty()) {
                    binding.collectionsRecyclerView.visibility = View.GONE
                    binding.textNoCollections.visibility = View.VISIBLE
                } else {
                    binding.collectionsRecyclerView.visibility = View.VISIBLE
                    binding.textNoCollections.visibility = View.GONE
                }
            }
        }

        // Set an OnClickListener for the FloatingActionButton.
        // When clicked, it will show the AddEditCollectionDialogFragment for adding a new collection.
        binding.fabAddCollection.setOnClickListener {
            Log.d(TAG, "FAB Add Collection clicked!")
            try {
                showAddEditCollectionDialog(null) // Pass null for new collection
                Log.d(TAG, "AddEditCollectionDialog show() called.")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing AddEditCollectionDialog: ${e.message}", e)
            }
        }
    }

    /**
     * Shows the AddEditCollectionDialogFragment.
     * @param collection Optional: The collection to edit. If null, a new collection will be added.
     */
    private fun showAddEditCollectionDialog(collection: Collection?) {
        val dialog = if (collection == null) {
            AddEditCollectionDialogFragment() // For adding a new collection
        } else {
            AddEditCollectionDialogFragment.newInstance(collection) // For editing an existing collection
        }
        dialog.show(parentFragmentManager, "AddEditCollectionDialog")
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called for CollectionsFragment.")
        _binding = null
    }
}
