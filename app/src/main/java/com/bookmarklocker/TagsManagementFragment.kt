package com.bookmarklocker

import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bookmarklocker.data.BookmarkDatabase
import com.bookmarklocker.data.BookmarkRepository
import com.bookmarklocker.databinding.FragmentTagsManagementBinding
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * TagsManagementFragment: This fragment provides the UI for managing (creating, editing, deleting)
 * global tags for the application. It will display a list of all unique tags used across bookmarks
 * and allow the user to modify them.
 */
class TagsManagementFragment : Fragment() {

    private val TAG = "TagsManagementFragment"

    private var _binding: FragmentTagsManagementBinding? = null
    private val binding get() = _binding!!

    // ViewModel for accessing bookmark data
    private val bookmarkViewModel: BookmarkViewModel by activityViewModels {
        BookmarkViewModelFactory(
            BookmarkRepository(BookmarkDatabase.getDatabase(requireContext()).bookmarkDao())
        )
    }

    // Vibrant chip colors
    private val chipColors by lazy {
        listOf(
            R.color.material_dynamic_primary40,
            R.color.material_dynamic_secondary40,
            R.color.material_dynamic_primary80,
            R.color.material_dynamic_secondary80,
            R.color.material_dynamic_error40
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTagsManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        bookmarkViewModel.allBookmarks.observe(viewLifecycleOwner) { bookmarks ->
            val allTags = bookmarks.flatMap { it.tags }.distinct().sorted()
            updateTagsDisplay(allTags)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTag.setOnClickListener {
            showAddTagDialog()
        }
    }

    private fun updateTagsDisplay(tags: List<String>) {
        val flexbox = binding.flexboxTags
        val emptyState = binding.emptyState
        flexbox.removeAllViews()
        if (tags.isEmpty()) {
            flexbox.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            flexbox.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            tags.forEach { tag ->
                val chip = createTagChip(tag)
                flexbox.addView(chip)
                animateChipIn(chip)
            }
        }
    }

    private fun createTagChip(tag: String): Chip {
        val chip = Chip(requireContext())
        chip.text = tag
        chip.isCloseIconVisible = true
        chip.isClickable = true
        chip.isFocusable = true
        chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        chip.setChipBackgroundColorResource(randomChipColor())
        chip.chipCornerRadius = resources.getDimension(R.dimen.chip_corner_radius)
        chip.closeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete_24)
        chip.closeIconTint = ContextCompat.getColorStateList(requireContext(), R.color.material_dynamic_error40)
        chip.setOnCloseIconClickListener {
            deleteTag(tag)
        }
        chip.setOnLongClickListener {
            editTag(tag, tag)
            true
        }
        val params = FlexboxLayout.LayoutParams(
            FlexboxLayout.LayoutParams.WRAP_CONTENT,
            FlexboxLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(12, 12, 12, 12)
        chip.layoutParams = params
        return chip
    }

    private fun randomChipColor(): Int {
        return chipColors[Random.nextInt(chipColors.size)]
    }

    private fun animateChipIn(chip: Chip) {
        chip.alpha = 0f
        chip.scaleX = 0.8f
        chip.scaleY = 0.8f
        chip.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun showAddTagDialog() {
        AddTagDialogFragment.newInstance { newTag ->
            lifecycleScope.launch {
                val tempBookmark = com.bookmarklocker.data.Bookmark(
                    url = "tag://$newTag",
                    title = "Tag: $newTag",
                    notes = "This is a tag template bookmark",
                    tags = listOf(newTag),
                    collectionId = null
                )
                bookmarkViewModel.insert(tempBookmark)
                Snackbar.make(
                    binding.root,
                    "Tag '$newTag' added successfully!",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }.show(childFragmentManager, "add_tag_dialog")
    }

    private fun deleteTag(tag: String) {
        ConfirmationDialogFragment.newInstance(
            title = "Delete Tag",
            message = "Are you sure you want to delete the tag '$tag'? This will remove it from all bookmarks that use it.",
            positiveButtonText = "Delete",
            negativeButtonText = "Cancel",
            onConfirmListener = { confirmed ->
                if (confirmed) {
                    lifecycleScope.launch {
                        val bookmarksWithTag = bookmarkViewModel.allBookmarks.value?.filter {
                            it.tags.contains(tag)
                        } ?: emptyList()
                        bookmarksWithTag.forEach { bookmark ->
                            val updatedTags = bookmark.tags.filter { it != tag }
                            val updatedBookmark = bookmark.copy(tags = updatedTags)
                            bookmarkViewModel.update(updatedBookmark)
                        }
                        val tagTemplateBookmarks = bookmarkViewModel.allBookmarks.value?.filter {
                            it.url == "tag://$tag"
                        } ?: emptyList()
                        tagTemplateBookmarks.forEach { bookmark ->
                            bookmarkViewModel.delete(bookmark)
                        }
                        Snackbar.make(
                            binding.root,
                            "Tag '$tag' removed from ${bookmarksWithTag.size} bookmark(s)",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        ).show(childFragmentManager, "delete_tag_dialog")
    }

    private fun editTag(oldTag: String, newTag: String) {
        AddTagDialogFragment.newInstance { newTagName ->
            lifecycleScope.launch {
                val bookmarksWithTag = bookmarkViewModel.allBookmarks.value?.filter {
                    it.tags.contains(oldTag)
                } ?: emptyList()
                bookmarksWithTag.forEach { bookmark ->
                    val updatedTags = bookmark.tags.map { if (it == oldTag) newTagName else it }
                    val updatedBookmark = bookmark.copy(tags = updatedTags)
                    bookmarkViewModel.update(updatedBookmark)
                }
                Snackbar.make(
                    binding.root,
                    "Tag '$oldTag' renamed to '$newTagName' in ${bookmarksWithTag.size} bookmark(s)",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }.show(childFragmentManager, "edit_tag_dialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
