package com.bookmarklocker

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bookmarklocker.data.Bookmark
import com.bookmarklocker.data.BookmarkDatabase
import com.bookmarklocker.data.BookmarkRepository
import com.bookmarklocker.data.Collection
import com.bookmarklocker.data.CollectionRepository
import com.bookmarklocker.databinding.DialogAddEditBookmarkBinding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import java.io.Serializable

/**
 * AddEditBookmarkDialogFragment: A DialogFragment to allow users to input details for a new bookmark
 * or to edit an existing one. It provides fields for URL, Title, Notes, Tags (editable chips),
 * and a Collection selector.
 *
 * This dialog dynamically changes its title and pre-fills fields if an existing Bookmark object
 * is passed to it for editing. It also handles adding and removing tags dynamically.
 *
 * Updated to ensure tags in the input field are always added when the save button is pressed.
 */
class AddEditBookmarkDialogFragment : DialogFragment() {

    private val TAG = "AddEditBookmarkDialog"

    private var _binding: DialogAddEditBookmarkBinding? = null
    private val binding get() = _binding!!

    // ViewModels for accessing data
    private val bookmarkViewModel: BookmarkViewModel by activityViewModels {
        Log.d(TAG, "Initializing BookmarkViewModelFactory for dialog...")
        BookmarkViewModelFactory(
            BookmarkRepository(BookmarkDatabase.getDatabase(requireContext()).bookmarkDao())
        )
    }
    private val collectionViewModel: CollectionViewModel by activityViewModels {
        Log.d(TAG, "Initializing CollectionViewModelFactory for dialog...")
        CollectionViewModelFactory(
            CollectionRepository(BookmarkDatabase.getDatabase(requireContext()).collectionDao())
        )
    }

    // Holds the bookmark being edited, if any. Null for new bookmarks.
    private var currentBookmark: Bookmark? = null
    // Temporarily holds data for a new bookmark that comes from a share intent for pre-filling
    // This is ONLY used for pre-filling. The save logic will treat it as a new bookmark.
    private var prefillBookmarkData: Bookmark? = null

    // List of tags for the current bookmark, mutable for editing
    private val currentTags = mutableListOf<String>()
    // List of available collections for the spinner
    private var availableCollections = listOf<Collection>()
    // Selected collection ID for the bookmark
    private var selectedCollectionId: Int? = null

    // Keys for arguments bundle
    companion object {
        private const val ARG_BOOKMARK = "arg_bookmark"

        /**
         * Factory method to create a new instance of AddEditBookmarkDialogFragment.
         * Used for editing an existing bookmark, or for pre-filling a new one (e.g. from share intent).
         * For a brand new, blank bookmark (from FAB), use the default constructor `AddEditBookmarkDialogFragment()`.
         * @param bookmark The Bookmark object to be edited (has non-0 ID) or a temporary one for pre-filling (has 0 ID).
         * @return A new instance of AddEditBookmarkDialogFragment.
         */
        fun newInstance(bookmark: Bookmark): AddEditBookmarkDialogFragment {
            val fragment = AddEditBookmarkDialogFragment()
            val args = Bundle().apply {
                putSerializable(ARG_BOOKMARK, bookmark as Serializable)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the bookmark object if passed as argument (for editing or pre-filling)
        arguments?.let {
            val bookmarkArg = it.getSerializable(ARG_BOOKMARK) as Bookmark?
            if (bookmarkArg != null) {
                if (bookmarkArg.id != 0) { // This is an existing bookmark for editing
                    currentBookmark = bookmarkArg
                    currentBookmark?.tags?.let { tags -> currentTags.addAll(tags) } // Initialize tags
                    selectedCollectionId = currentBookmark?.collectionId // Initialize collection
                    Log.d(TAG, "onCreate: Editing existing bookmark with ID: ${currentBookmark?.id}")
                } else { // This is a new bookmark (ID 0) for pre-filling (e.g., from share intent)
                    prefillBookmarkData = bookmarkArg
                    prefillBookmarkData?.tags?.let { tags -> currentTags.addAll(tags) } // Initialize tags from prefill data
                    selectedCollectionId = prefillBookmarkData?.collectionId // Initialize collection from prefill data
                    Log.d(TAG, "onCreate: Pre-filling new bookmark from share (ID 0).")
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called for AddEditBookmarkDialogFragment.")
        _binding = DialogAddEditBookmarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called for AddEditBookmarkDialogFragment.")

        // Set dialog title and pre-fill fields based on the type of bookmark
        if (currentBookmark != null) { // It's an existing bookmark being edited
            binding.dialogTitleBookmark.text = getString(R.string.edit_bookmark)
            binding.editTextUrl.setText(currentBookmark?.url)
            binding.editTextTitle.setText(currentBookmark?.title)
            binding.editTextNotes.setText(currentBookmark?.notes)
            Log.d(TAG, "onViewCreated: Pre-filling fields for editing existing bookmark.")
        } else if (prefillBookmarkData != null) { // It's a new bookmark from share (for pre-filling)
            binding.dialogTitleBookmark.text = getString(R.string.add_new_bookmark)
            binding.editTextUrl.setText(prefillBookmarkData?.url)
            binding.editTextTitle.setText(prefillBookmarkData?.title)
            binding.editTextNotes.setText(prefillBookmarkData?.notes)
            Log.d(TAG, "onViewCreated: Pre-filling fields for new bookmark from share.")
            // Tags and collection ID for prefillBookmarkData are already in currentTags/selectedCollectionId
        } else { // It's a completely new, blank bookmark (from FAB)
            binding.dialogTitleBookmark.text = getString(R.string.add_new_bookmark)
            Log.d(TAG, "onViewCreated: Setting up for new, blank bookmark.")
        }


        // Setup Tags input
        setupTagInput()
        displayTags() // Display any pre-existing tags (from currentBookmark or prefillBookmarkData, or empty)

        // Setup Collections spinner
        setupCollectionSpinner()

        binding.btnCancel.setOnClickListener {
            Log.d(TAG, "Cancel button clicked. Dismissing dialog.")
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            Log.d(TAG, "Save button clicked. Attempting to save/update bookmark.")
            // CRITICAL FIX: Ensure any text in the tag input is added as a tag before saving the bookmark
            addTagFromInput() // Process any pending tag text
            saveBookmark()
        }
    }

    /**
     * Sets up the tag input field and its associated ChipGroup.
     */
    private fun setupTagInput() {
        binding.textInputLayoutAddTag.setEndIconOnClickListener {
            addTagFromInput()
        }

        binding.editTextAddTag.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addTagFromInput()
                true
            } else {
                false
            }
        }
    }

    /**
     * Adds a tag from the input field to the currentTags list and updates the display.
     */
    private fun addTagFromInput() {
        val tagText = binding.editTextAddTag.text.toString().trim()
        if (tagText.isNotBlank() && !currentTags.contains(tagText)) {
            currentTags.add(tagText)
            displayTags()
            binding.editTextAddTag.text?.clear() // Clear input field
            Snackbar.make(binding.root, getString(R.string.tag_added, tagText), Snackbar.LENGTH_SHORT).show()
        } else if (tagText.isNotBlank() && currentTags.contains(tagText)) {
            Snackbar.make(binding.root, getString(R.string.tag_already_exists, tagText), Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * Dynamically adds chips to the ChipGroup based on the currentTags list.
     */
    private fun displayTags() {
        binding.chipGroupTags.removeAllViews() // Clear existing chips
        currentTags.forEach { tag ->
            val chip = Chip(context).apply {
                text = tag
                isCloseIconVisible = true // Show close icon to remove tag
                isClickable = true
                isFocusable = true
                setChipBackgroundColorResource(R.color.material_dynamic_neutral80)
                setTextColor(resources.getColor(R.color.material_dynamic_primary10, null))
                chipCornerRadius = resources.getDimension(R.dimen.chip_corner_radius)
                setEnsureMinTouchTargetSize(false) // Allow smaller touch target for chips
                closeIconContentDescription = getString(R.string.remove_tag_description, tag)

                setOnCloseIconClickListener {
                    currentTags.remove(tag)
                    displayTags() // Refresh chips
                    Snackbar.make(binding.root, getString(R.string.tag_removed, tag), Snackbar.LENGTH_SHORT).show()
                }
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    /**
     * Sets up the Spinner for selecting a collection.
     * Observes all collections from the ViewModel and populates the spinner.
     */
    private fun setupCollectionSpinner() {
        collectionViewModel.allCollections.observe(viewLifecycleOwner) { collections ->
            availableCollections = collections
            val collectionNames = collections.map { it.name }.toMutableList()
            // Add an option for "No Collection"
            collectionNames.add(0, getString(R.string.no_collection_option))

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item, // Default spinner item layout
                collectionNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) // Default dropdown layout
            binding.spinnerCollections.adapter = adapter

            // Select the initial collection in the spinner (from existing bookmark or prefill data)
            val initialCollectionIdForSpinner = currentBookmark?.collectionId ?: prefillBookmarkData?.collectionId
            initialCollectionIdForSpinner?.let { initialId ->
                val index = collections.indexOfFirst { it.id == initialId }
                if (index != -1) {
                    binding.spinnerCollections.setSelection(index + 1) // +1 because of "No Collection" option
                }
            } ?: run {
                binding.spinnerCollections.setSelection(0) // Default to "No Collection"
            }


            binding.spinnerCollections.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCollectionId = if (position == 0) {
                        null // "No Collection" selected
                    } else {
                        availableCollections[position - 1].id // Adjust index for "No Collection"
                    }
                    Log.d(TAG, "Selected collection ID: $selectedCollectionId")
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    selectedCollectionId = null // Default to no collection
                    Log.d(TAG, "Nothing selected in collection spinner. Default to null.")
                }
            }
        }
    }


    /**
     * Validates user input and saves/updates the bookmark to the database if valid.
     * Shows a Snackbar message for validation errors or success.
     */
    private fun saveBookmark() {
        var url = binding.editTextUrl.text.toString().trim()
        val title = binding.editTextTitle.text.toString().trim()
        val notes = binding.editTextNotes.text.toString().trim()

        Log.d(TAG, "Validating input - URL: '$url', Title: '$title', Notes: '$notes', Tags: $currentTags, CollectionId: $selectedCollectionId")

        // --- Input Validation ---
        if (url.isBlank()) {
            binding.textInputLayoutUrl.error = "URL cannot be empty"
            Log.w(TAG, "Validation failed: URL is blank.")
            return // Stop execution if URL is empty
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Automatically add https:// if no scheme is present
            url = "https://$url"
            Log.d(TAG, "No scheme found, auto-prefixed URL to: $url")
        }

        // Now validate the potentially modified URL
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            binding.textInputLayoutUrl.error = "Please enter a valid URL (e.g., https://example.com)"
            Log.w(TAG, "Validation failed: Invalid URL format even after prefixing.")
            return // Stop execution if URL is invalid
        } else {
            binding.textInputLayoutUrl.error = null // Clear any previous error
            Log.d(TAG, "URL validation passed.")
        }

        if (currentBookmark == null) {
            // Add new bookmark (currentBookmark is null for new entries from FAB or share intent)
            val newBookmark = Bookmark(
                url = url,
                title = if (title.isEmpty()) null else title,
                notes = if (notes.isEmpty()) null else notes,
                tags = currentTags.toList(),
                collectionId = selectedCollectionId
            )
            bookmarkViewModel.insert(newBookmark)
            Log.d(TAG, "New bookmark object created for insertion: $newBookmark")
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                getString(R.string.bookmark_saved),
                Snackbar.LENGTH_SHORT
            ).show()
        } else {
            // Update existing bookmark (currentBookmark is not null, its ID is > 0)
            val updatedBookmark = currentBookmark!!.copy(
                url = url,
                title = if (title.isEmpty()) null else title,
                notes = if (notes.isEmpty()) null else notes,
                tags = currentTags.toList(),
                collectionId = selectedCollectionId,
                id = currentBookmark!!.id // Preserve original ID for update
            )
            bookmarkViewModel.update(updatedBookmark)
            Log.d(TAG, "Bookmark object updated for update: $updatedBookmark")
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                getString(R.string.bookmark_updated),
                Snackbar.LENGTH_SHORT
            ).show()
        }

        Log.d(TAG, "Bookmark save/update called on ViewModel. Dismissing dialog.")
        dismiss() // Close the dialog after saving
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called for AddEditBookmarkDialogFragment.")
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        Log.d(TAG, "onStart called for AddEditBookmarkDialogFragment. Dialog layout adjusted.")
    }
}
