package com.bookmarklocker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bookmarklocker.data.Bookmark
import com.bookmarklocker.data.BookmarkDatabase
import com.bookmarklocker.data.BookmarkRepository
import com.bookmarklocker.databinding.FragmentBookmarksBinding
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.fragment.findNavController

/**
 * BookmarksFragment: This fragment displays the main list of user's bookmarks.
 * It integrates with the BookmarkViewModel to observe and display bookmarks
 * from the local database using a RecyclerView. It also manages the visibility of the
 * "No bookmarks yet" message and the FloatingActionButton to add new bookmarks.
 *
 * Updated in Step 6 to:
 * - Handle opening URL on item click.
 * - Implement callbacks for bookmark edit and delete actions from the adapter.
 * - Show confirmation dialog for deletion.
 * - Correctly trigger `AddEditBookmarkDialogFragment` for new bookmarks by passing null.
 */
class BookmarksFragment : Fragment() {

    private val TAG = "BookmarksFragment"

    // ViewBinding for safer access to UI elements in fragment_bookmarks.xml
    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    // Lazily initialize the BookmarkViewModel using activityViewModels so it's scoped to the activity
    private val bookmarkViewModel: BookmarkViewModel by activityViewModels {
        Log.d(TAG, "Initializing BookmarkViewModelFactory...")
        BookmarkViewModelFactory(
            BookmarkRepository(BookmarkDatabase.getDatabase(requireContext()).bookmarkDao())
        )
    }

    private var multiSelectMode: Boolean = false
    private var targetCollectionId: Int? = null
    private val selectedBookmarks = mutableSetOf<Bookmark>()
    private lateinit var adapter: BookmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called.")
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called.")

        // Check for multi-select mode
        arguments?.let {
            multiSelectMode = it.getBoolean("multiSelectMode", false)
            targetCollectionId = if (it.containsKey("targetCollectionId")) it.getInt("targetCollectionId") else null
        }

        adapter = BookmarkAdapter(
            onDeleteClick = { bookmark ->
                if (multiSelectMode) {
                    toggleSelection(bookmark)
                } else {
                    Log.d(TAG, "Delete clicked for bookmark: ${bookmark.title}")
                    showDeleteConfirmationDialog(bookmark)
                }
            },
            onEditClick = { bookmark ->
                if (multiSelectMode) {
                    toggleSelection(bookmark)
                } else {
                    Log.d(TAG, "Edit clicked for bookmark: ${bookmark.title}")
                    showAddEditBookmarkDialog(bookmark)
                }
            },
            onItemClick = { bookmark ->
                if (multiSelectMode) {
                    toggleSelection(bookmark)
                } else {
                    Log.d(TAG, "Bookmark item clicked: ${bookmark.title} - Opening URL.")
                    openBookmarkUrl(bookmark.url)
                }
            },
            isMultiSelectMode = multiSelectMode,
            selectedIds = selectedBookmarks.map { it.id }.toSet(),
            onToggleSelect = { bookmark ->
                toggleSelection(bookmark)
                adapter.notifyDataSetChanged()
            }
        )
        binding.bookmarksRecyclerView.adapter = adapter
        binding.bookmarksRecyclerView.layoutManager = LinearLayoutManager(context)

        // Show/hide Add to Collection button
        if (multiSelectMode) {
            binding.fabAddBookmark.visibility = View.GONE
            binding.addToCollectionButton.visibility = View.VISIBLE
            binding.addToCollectionButton.setOnClickListener {
                targetCollectionId?.let { collectionId ->
                    selectedBookmarks.forEach { bookmark ->
                        val updated = bookmark.copy(collectionId = collectionId)
                        bookmarkViewModel.update(updated)
                    }
                    Snackbar.make(requireView(), "Added to collection", Snackbar.LENGTH_SHORT).show()
                    // Pop back stack twice to ensure return to collection detail
                    val navController = findNavController()
                    navController.popBackStack()
                    navController.popBackStack()
                    Log.d(TAG, "Navigated back to collection detail after adding bookmarks.")
                }
            }
        } else {
            binding.fabAddBookmark.visibility = View.VISIBLE
            binding.addToCollectionButton.visibility = View.GONE
        }

        // Observe changes in allBookmarks LiveData from the ViewModel.
        bookmarkViewModel.allBookmarks.observe(viewLifecycleOwner) { bookmarks ->
            bookmarks.let {
                Log.d(TAG, "Bookmarks LiveData updated. Count: ${it.size}")
                // Filter out tag template bookmarks (those with tag:// URLs)
                val filteredBookmarks = it.filter { bookmark -> !bookmark.url.startsWith("tag://") }
                adapter.submitList(filteredBookmarks)
                if (filteredBookmarks.isEmpty()) {
                    binding.bookmarksRecyclerView.visibility = View.GONE
                    binding.textNoBookmarks.visibility = View.VISIBLE
                } else {
                    binding.bookmarksRecyclerView.visibility = View.VISIBLE
                    binding.textNoBookmarks.visibility = View.GONE
                }
            }
        }

        // Set an OnClickListener for the FloatingActionButton to add new bookmarks.
        binding.fabAddBookmark.setOnClickListener {
            Log.d(TAG, "FAB Add Bookmark clicked!")
            try {
                // Corrected: Pass null to indicate a new, blank bookmark.
                showAddEditBookmarkDialog(null)
                Log.d(TAG, "AddEditBookmarkDialog show() called for NEW bookmark.")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing AddEditBookmarkDialog: ${e.message}", e)
            }
        }
    }

    /**
     * Shows the AddEditBookmarkDialogFragment for adding a new bookmark or editing an existing one.
     * @param bookmark Optional: The Bookmark object to edit. If null, a new bookmark will be added.
     */
    private fun showAddEditBookmarkDialog(bookmark: Bookmark?) {
        val dialog = if (bookmark == null) {
            AddEditBookmarkDialogFragment() // For adding a new bookmark (no arguments for truly blank)
        } else {
            AddEditBookmarkDialogFragment.newInstance(bookmark) // For editing an existing bookmark or pre-filling from share
        }
        dialog.show(parentFragmentManager, "AddEditBookmarkDialog")
    }

    /**
     * Shows a confirmation dialog before deleting a bookmark.
     * @param bookmark The Bookmark object to be deleted.
     */
    private fun showDeleteConfirmationDialog(bookmark: Bookmark) {
        val dialog = ConfirmationDialogFragment.newInstance(
            title = getString(R.string.confirm_delete_title),
            message = getString(R.string.confirm_delete_message, bookmark.title ?: bookmark.url),
            positiveButtonText = getString(R.string.action_delete),
            negativeButtonText = getString(R.string.cancel)
        ) { confirmed ->
            if (confirmed) {
                bookmarkViewModel.delete(bookmark)
                Snackbar.make(
                    requireView(),
                    getString(R.string.bookmark_deleted, bookmark.title ?: bookmark.url),
                    Snackbar.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Bookmark deleted: ${bookmark.title}")
            } else {
                Snackbar.make(
                    requireView(),
                    getString(R.string.delete_cancelled),
                    Snackbar.LENGTH_SHORT
                ).show()
                Log.d(TAG, "Bookmark deletion cancelled.")
            }
        }
        dialog.show(parentFragmentManager, "DeleteConfirmationDialog")
    }

    /**
     * Opens the given URL in an external web browser.
     * @param url The URL string to open.
     */
    private fun openBookmarkUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(
                requireView(),
                getString(R.string.error_opening_url, url),
                Snackbar.LENGTH_LONG
            ).show()
            Log.e(TAG, "Error opening URL: $url", e)
        }
    }

    private fun toggleSelection(bookmark: Bookmark) {
        if (selectedBookmarks.contains(bookmark)) {
            selectedBookmarks.remove(bookmark)
        } else {
            selectedBookmarks.add(bookmark)
        }
        // Optionally, update UI to reflect selection
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called.")
        _binding = null
    }
}
