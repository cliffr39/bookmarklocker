package com.bookmarklocker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bookmarklocker.databinding.DialogThemeSelectionBinding
import com.google.android.material.snackbar.Snackbar

/**
 * ThemeSelectionDialogFragment: A dialog for selecting the app theme.
 * Allows users to choose between Light Mode, Dark Mode, and System Default.
 */
class ThemeSelectionDialogFragment : DialogFragment() {

    private var _binding: DialogThemeSelectionBinding? = null
    private val binding get() = _binding!!

    private var onThemeSelected: ((Int) -> Unit)? = null

    companion object {
        fun newInstance(onThemeSelected: (Int) -> Unit): ThemeSelectionDialogFragment {
            val fragment = ThemeSelectionDialogFragment()
            fragment.onThemeSelected = onThemeSelected
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogThemeSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up click listeners for theme options
        binding.btnLightMode.setOnClickListener {
            onThemeSelected?.invoke(ThemeManager.THEME_LIGHT)
            dismiss()
        }

        binding.btnDarkMode.setOnClickListener {
            onThemeSelected?.invoke(ThemeManager.THEME_DARK)
            dismiss()
        }

        binding.btnSystemDefault.setOnClickListener {
            onThemeSelected?.invoke(ThemeManager.THEME_SYSTEM)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Highlight the current theme
        val currentTheme = ThemeManager.getThemeMode(requireContext())
        when (currentTheme) {
            ThemeManager.THEME_LIGHT -> binding.btnLightMode.isSelected = true
            ThemeManager.THEME_DARK -> binding.btnDarkMode.isSelected = true
            ThemeManager.THEME_SYSTEM -> binding.btnSystemDefault.isSelected = true
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