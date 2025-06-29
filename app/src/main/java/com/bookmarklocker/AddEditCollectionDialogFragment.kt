package com.bookmarklocker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bookmarklocker.data.BookmarkDatabase
import com.bookmarklocker.data.Collection
import com.bookmarklocker.data.CollectionRepository
import com.bookmarklocker.databinding.DialogAddEditCollectionBinding
import com.google.android.material.snackbar.Snackbar

/**
 * AddEditCollectionDialogFragment: A DialogFragment to allow users to add new collections
 * or edit existing ones. It provides input for the collection name and a color picker.
 * It uses the CollectionViewModel to interact with the database.
 *
 * Updated to include visual feedback for selected color with a line indicator and enhanced debugging logs.
 */
class AddEditCollectionDialogFragment : DialogFragment() {

    private val TAG = "CollectionDialog"

    private var _binding: DialogAddEditCollectionBinding? = null
    private val binding get() = _binding!!

    // ViewModel for collections, scoped to the activity
    private val collectionViewModel: CollectionViewModel by activityViewModels {
        Log.d(TAG, "Initializing CollectionViewModelFactory for dialog...")
        CollectionViewModelFactory(
            CollectionRepository(BookmarkDatabase.getDatabase(requireContext()).collectionDao())
        )
    }

    // Hold the collection being edited, if any
    private var currentCollection: Collection? = null
    private var selectedColor: String = "#0062AD" // Default color (material_dynamic_primary40 - Blue)

    // Maps RadioButton IDs to their corresponding line View IDs and hex color strings for the circle's background.
    private val colorMapping = mapOf(
        R.id.radio_color_blue to Pair(R.id.line_color_blue, "#0062AD"), // Pair(lineId, hexColor)
        R.id.radio_color_green to Pair(R.id.line_color_green, "#7F437C"),
        R.id.radio_color_red to Pair(R.id.line_color_red, "#BA1A1A"),
        R.id.radio_color_yellow to Pair(R.id.line_color_yellow, "#575F6E"),
        R.id.radio_color_purple to Pair(R.id.line_color_purple, "#800080")
    )

    // Keys for arguments bundle
    companion object {
        private const val ARG_COLLECTION_ID = "arg_collection_id"
        private const val ARG_COLLECTION_NAME = "arg_collection_name"
        private const val ARG_COLLECTION_COLOR = "arg_collection_color"

        /**
         * Factory method to create a new instance of AddEditCollectionDialogFragment for editing.
         * @param collection The Collection object to be edited.
         * @return A new instance of AddEditCollectionDialogFragment.
         */
        fun newInstance(collection: Collection): AddEditCollectionDialogFragment {
            val fragment = AddEditCollectionDialogFragment()
            val args = Bundle().apply {
                putInt(ARG_COLLECTION_ID, collection.id)
                putString(ARG_COLLECTION_NAME, collection.name)
                putString(ARG_COLLECTION_COLOR, collection.color)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called for AddEditCollectionDialogFragment.")
        _binding = DialogAddEditCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called for AddEditCollectionDialogFragment.")

        // Check if we are editing an existing collection
        arguments?.let { args ->
            val collectionId = args.getInt(ARG_COLLECTION_ID, 0)
            val collectionName = args.getString(ARG_COLLECTION_NAME)
            val collectionColor = args.getString(ARG_COLLECTION_COLOR)

            if (collectionId != 0 && collectionName != null && collectionColor != null) {
                currentCollection = Collection(collectionId, collectionName, collectionColor, System.currentTimeMillis())
                binding.dialogTitleCollection.text = getString(R.string.edit_collection)
                binding.editTextCollectionName.setText(collectionName)
                selectedColor = collectionColor // Set initial selected color
                Log.d(TAG, "Editing collection: ID=$collectionId, Name=$collectionName, Color=$collectionColor")
            }
        }

        // Initialize color radio group listener first
        setupColorRadioGroup()

        // Explicitly set OnClickListeners for each RadioButton.
        // This is crucial when using android:button="@null" for custom appearances.
        binding.root.findViewById<RadioButton>(R.id.radio_color_blue).setOnClickListener { binding.colorRadioGroup.check(R.id.radio_color_blue) }
        binding.root.findViewById<RadioButton>(R.id.radio_color_green).setOnClickListener { binding.colorRadioGroup.check(R.id.radio_color_green) }
        binding.root.findViewById<RadioButton>(R.id.radio_color_red).setOnClickListener { binding.colorRadioGroup.check(R.id.radio_color_red) }
        binding.root.findViewById<RadioButton>(R.id.radio_color_yellow).setOnClickListener { binding.colorRadioGroup.check(R.id.radio_color_yellow) }
        binding.root.findViewById<RadioButton>(R.id.radio_color_purple).setOnClickListener { binding.colorRadioGroup.check(R.id.radio_color_purple) }

        // Pre-select color after setting up all listeners
        preSelectColorInRadioGroup(selectedColor)


        binding.btnCancelCollection.setOnClickListener {
            Log.d(TAG, "Cancel button clicked. Dismissing dialog.")
            dismiss()
        }

        binding.btnSaveCollection.setOnClickListener {
            Log.d(TAG, "Save button clicked. Attempting to save/update collection.")
            saveCollection()
        }
    }

    /**
     * Sets up the RadioGroup for color selection.
     * Attaches an OnCheckedChangeListener to update the selectedColor and the line indicator.
     */
    private fun setupColorRadioGroup() {
        Log.d(TAG, "Setting up color radio group listener.")
        binding.colorRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            Log.d(TAG, "RadioGroup checked change listener fired. Checked ID: $checkedId")

            // Hide all lines first
            colorMapping.values.forEach { (lineId, _) ->
                binding.root.findViewById<View>(lineId)?.visibility = View.INVISIBLE
            }

            // Show the line for the newly checked RadioButton and update selectedColor
            val (lineToShowId, colorHex) = colorMapping[checkedId] ?: Pair(null, null)

            if (lineToShowId != null && colorHex != null) {
                binding.root.findViewById<View>(lineToShowId)?.visibility = View.VISIBLE
                selectedColor = colorHex // Directly use the stored hex string
                Log.d(TAG, "Selected color changed to: $selectedColor, Line visible for ID: $lineToShowId")
            } else {
                Log.w(TAG, "Unknown checked ID or missing mapping: $checkedId. Defaulting to blue.")
                selectedColor = "#0062AD" // Directly use default blue hex
            }
        }
    }

    /**
     * Pre-selects the correct RadioButton based on the initial color for editing
     * and shows its corresponding line indicator.
     */
    private fun preSelectColorInRadioGroup(colorHex: String) {
        Log.d(TAG, "Pre-selecting color: $colorHex")

        var checkedId = -1
        for ((radioId, pair) in colorMapping) {
            val (_, mappedColorHex) = pair
            if (mappedColorHex == colorHex) {
                checkedId = radioId
                break
            }
        }

        if (checkedId != -1) {
            binding.colorRadioGroup.check(checkedId)
            Log.d(TAG, "Pre-selected RadioButton with ID: $checkedId")
            // The setOnCheckedChangeListener will handle showing the line, but we can explicitly
            // ensure it's visible for the initial state.
            val lineToShowId = colorMapping[checkedId]?.first
            if (lineToShowId != null) {
                binding.root.findViewById<View>(lineToShowId)?.visibility = View.VISIBLE
                Log.d(TAG, "Explicitly set line visibility for pre-selected color.")
            }
        } else {
            // Default to blue if the pre-selected color doesn't match any radio button.
            binding.colorRadioGroup.check(R.id.radio_color_blue)
            binding.root.findViewById<View>(R.id.line_color_blue)?.visibility = View.VISIBLE
            Log.w(TAG, "Pre-selected color not found in map, defaulting to blue and showing its line.")
        }
    }


    /**
     * Validates user input and saves/updates the collection to the database if valid.
     * Shows a Snackbar message for validation errors or success.
     */
    private fun saveCollection() {
        val name = binding.editTextCollectionName.text.toString().trim()

        Log.d(TAG, "Validating input - Name: '$name', Selected Color: '$selectedColor'")

        // --- Input Validation ---
        if (name.isBlank()) {
            binding.textInputLayoutCollectionName.error = "Collection name cannot be empty"
            Log.w(TAG, "Validation failed: Collection name is blank.")
            return // Stop execution if name is empty
        } else {
            binding.textInputLayoutCollectionName.error = null // Clear any previous error
            Log.d(TAG, "Collection name validation passed.")
        }

        if (currentCollection == null) {
            // Add new collection
            val newCollection = Collection(
                name = name,
                color = selectedColor
            )
            collectionViewModel.insert(newCollection)
            Log.d(TAG, "New collection object created: $newCollection")
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                "Collection added successfully!",
                Snackbar.LENGTH_SHORT
            ).show()
        } else {
            // Update existing collection
            val updatedCollection = currentCollection!!.copy(
                name = name,
                color = selectedColor
            )
            collectionViewModel.update(updatedCollection)
            Log.d(TAG, "Collection object updated: $updatedCollection")
            Snackbar.make(
                requireActivity().findViewById(android.R.id.content),
                "Collection updated successfully!",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        Log.d(TAG, "Collection save/update called on ViewModel. Dismissing dialog.")
        dismiss() // Close the dialog after saving
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called for AddEditCollectionDialogFragment.")
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        Log.d(TAG, "onStart called for AddEditCollectionDialogFragment. Dialog layout adjusted.")
    }
}
