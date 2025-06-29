package com.bookmarklocker

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * ConfirmationDialogFragment: A generic DialogFragment to display a confirmation message
 * with customizable title, message, and button texts. It provides a callback for the result.
 *
 * This dialog is used, for example, to confirm bookmark deletion.
 */
class ConfirmationDialogFragment : DialogFragment() {

    private val TAG = "ConfirmDialog"

    // Callback to be invoked when the user makes a choice
    private var onConfirmListener: ((Boolean) -> Unit)? = null

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_POSITIVE_BUTTON = "positive_button"
        private const val ARG_NEGATIVE_BUTTON = "negative_button"

        /**
         * Factory method to create a new instance of ConfirmationDialogFragment.
         * @param title The title of the dialog.
         * @param message The message to display.
         * @param positiveButtonText Text for the positive action button.
         * @param negativeButtonText Text for the negative action button.
         * @param onConfirmListener Lambda to be called with true if confirmed, false otherwise.
         */
        fun newInstance(
            title: String,
            message: String,
            positiveButtonText: String,
            negativeButtonText: String,
            onConfirmListener: (Boolean) -> Unit
        ): ConfirmationDialogFragment {
            val fragment = ConfirmationDialogFragment()
            val args = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
                putString(ARG_POSITIVE_BUTTON, positiveButtonText)
                putString(ARG_NEGATIVE_BUTTON, negativeButtonText)
            }
            fragment.arguments = args
            fragment.onConfirmListener = onConfirmListener
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(ARG_TITLE)
        val message = arguments?.getString(ARG_MESSAGE)
        val positiveButtonText = arguments?.getString(ARG_POSITIVE_BUTTON)
        val negativeButtonText = arguments?.getString(ARG_NEGATIVE_BUTTON)

        Log.d(TAG, "Creating confirmation dialog. Title: '$title', Message: '$message'")

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                Log.d(TAG, "Positive button clicked.")
                onConfirmListener?.invoke(true) // Invoke callback with true for confirmation
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                Log.d(TAG, "Negative button clicked.")
                onConfirmListener?.invoke(false) // Invoke callback with false for cancellation
            }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called for ConfirmationDialogFragment.")
        // Clear the listener to prevent memory leaks if dialog outlives the fragment
        onConfirmListener = null
    }
}
