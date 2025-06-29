package com.bookmarklocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bookmarklocker.databinding.DialogAddTagBinding
import com.google.android.material.snackbar.Snackbar

/**
 * AddTagDialogFragment: A simple dialog for adding new tags.
 * This dialog provides a text input field for entering a new tag name.
 */
class AddTagDialogFragment : DialogFragment() {

    private var _binding: DialogAddTagBinding? = null
    private val binding get() = _binding!!

    private var onTagAdded: ((String) -> Unit)? = null

    companion object {
        fun newInstance(onTagAdded: (String) -> Unit): AddTagDialogFragment {
            val fragment = AddTagDialogFragment()
            fragment.onTagAdded = onTagAdded
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val tagName = binding.editTextTagName.text.toString().trim()
            if (tagName.isNotBlank()) {
                onTagAdded?.invoke(tagName)
                dismiss()
            } else {
                Snackbar.make(binding.root, "Tag name cannot be empty", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
} 