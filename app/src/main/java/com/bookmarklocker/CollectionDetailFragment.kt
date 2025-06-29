package com.bookmarklocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.bookmarklocker.data.Bookmark
import com.bookmarklocker.databinding.FragmentCollectionDetailBinding
import com.google.android.material.snackbar.Snackbar

class CollectionDetailFragment : Fragment() {
    private var _binding: FragmentCollectionDetailBinding? = null
    private val binding get() = _binding!!

    private val bookmarkViewModel: BookmarkViewModel by activityViewModels()
    private var collectionId: Int = -1
    private var collectionName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            collectionId = it.getInt("collectionId", -1)
            collectionName = it.getString("collectionName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.collectionDetailTitle.text = collectionName ?: "Collection"

        val adapter = BookmarkAdapter(
            onDeleteClick = { bookmark ->
                // Only remove from collection, not delete the bookmark
                val updatedBookmark = bookmark.copy(collectionId = null)
                bookmarkViewModel.update(updatedBookmark)
                Snackbar.make(binding.root, "Bookmark removed from collection", Snackbar.LENGTH_SHORT).show()
            },
            onEditClick = { bookmark ->
                // TODO: Show edit dialog for bookmark
            },
            onItemClick = { bookmark ->
                // TODO: Open bookmark URL or details
            }
        )
        binding.collectionBookmarksRecyclerView.adapter = adapter
        binding.collectionBookmarksRecyclerView.layoutManager = LinearLayoutManager(context)

        bookmarkViewModel.getBookmarksByCollection(collectionId).observe(viewLifecycleOwner) { bookmarks ->
            adapter.submitList(bookmarks)
        }

        binding.fabAddBookmarkToCollection.setOnClickListener {
            // Navigate to BookmarksFragment in multi-select mode for adding to this collection
            val bundle = Bundle().apply {
                putBoolean("multiSelectMode", true)
                putInt("targetCollectionId", collectionId)
            }
            findNavController().navigate(R.id.bookmarksFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"
        private const val ARG_COLLECTION_NAME = "collection_name"

        fun newInstance(collectionId: Int, collectionName: String): CollectionDetailFragment {
            val fragment = CollectionDetailFragment()
            val args = Bundle()
            args.putInt(ARG_COLLECTION_ID, collectionId)
            args.putString(ARG_COLLECTION_NAME, collectionName)
            fragment.arguments = args
            return fragment
        }
    }
} 