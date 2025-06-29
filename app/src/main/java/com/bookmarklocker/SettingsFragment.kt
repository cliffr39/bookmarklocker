package com.bookmarklocker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asFlow
import androidx.navigation.fragment.findNavController // Import for navigation
import com.bookmarklocker.data.BackupData
import com.bookmarklocker.data.BookmarkDatabase
import com.bookmarklocker.data.BookmarkRepository
import com.bookmarklocker.data.CollectionRepository
import com.bookmarklocker.databinding.FragmentSettingsBinding
import com.bookmarklocker.utils.JsonUtil
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

/**
 * SettingsFragment: This fragment provides various application settings options.
 * In Step 5, it implemented backup and restore functionality.
 * In Step 7, it adds a UI section and navigation for "Manage Tags".
 */
class SettingsFragment : Fragment() {

    private val TAG = "SettingsFragment"

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // ViewModels for accessing data
    private val bookmarkViewModel: BookmarkViewModel by activityViewModels {
        BookmarkViewModelFactory(
            BookmarkRepository(BookmarkDatabase.getDatabase(requireContext()).bookmarkDao())
        )
    }

    private val collectionViewModel: CollectionViewModel by activityViewModels {
        CollectionViewModelFactory(
            CollectionRepository(BookmarkDatabase.getDatabase(requireContext()).collectionDao())
        )
    }

    // ActivityResultLauncher for picking a file for restore
    private val restoreFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "Restore file selected: $uri")
                readAndRestoreData(uri)
            } ?: Log.w(TAG, "No data URI returned for restore.")
        } else {
            Log.d(TAG, "Restore file selection cancelled.")
            showSnackbar("Restore cancelled.")
        }
    }

    // ActivityResultLauncher for creating a file for backup
    private val backupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "Backup file URI received: $uri")
                writeBackupData(uri)
            } ?: Log.w(TAG, "No data URI returned for backup.")
        } else {
            Log.d(TAG, "Backup cancelled.")
            showSnackbar("Backup cancelled.")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called for SettingsFragment.")
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called for SettingsFragment.")

        // Set up click listeners for the Backup and Restore buttons
        binding.btnBackup.setOnClickListener {
            Log.d(TAG, "Backup button clicked.")
            performBackup()
        }

        binding.btnRestore.setOnClickListener {
            Log.d(TAG, "Restore button clicked.")
            performRestore()
        }

        // --- Tag Management Setup (Step 7) ---
        binding.btnManageTags.setOnClickListener {
            Log.d(TAG, "Manage Tags button clicked. Navigating to TagsManagementFragment.")
            // Navigate to the new TagsManagementFragment
            findNavController().navigate(R.id.tagsManagementFragment) // This destination will be added to nav_graph
        }

        // --- Theme Settings Setup ---
        binding.btnThemeSettings.setOnClickListener {
            Log.d(TAG, "Theme Settings button clicked.")
            showThemeSelectionDialog()
        }
    }


    /**
     * Initiates the backup process.
     * Uses Storage Access Framework (SAF) to let the user choose a location to save the backup file.
     */
    private fun performBackup() {
        // Create an intent to create a new file
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE) // Allow opening a URI that can be read or written
            type = "application/json" // Set MIME type to JSON
            putExtra(Intent.EXTRA_TITLE, "BookmarkLocker_Backup_${System.currentTimeMillis()}.json") // Default filename
        }
        backupFileLauncher.launch(intent)
    }

    /**
     * Writes the current application data to the specified URI as a JSON file.
     * @param uri The URI where the backup file should be saved.
     */
    private fun writeBackupData(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) { // Perform I/O operation on a background thread
            try {
                // Fetch all bookmarks and collections from the database
                val bookmarks = bookmarkViewModel.allBookmarks.asFlow().firstOrNull() ?: emptyList()
                val collections = collectionViewModel.allCollections.asFlow().firstOrNull() ?: emptyList()

                val backupData = BackupData(bookmarks, collections)
                val jsonString = JsonUtil.serializeBackupData(backupData)

                // Write the JSON string to the selected file URI
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    showSnackbar("Backup successful to ${uri.lastPathSegment}!")
                    Log.d(TAG, "Backup data written to: $uri")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Backup failed: ${e.message}")
                    Log.e(TAG, "Backup failed", e)
                }
            }
        }
    }

    /**
     * Initiates the restore process.
     * Uses Storage Access Framework (SAF) to let the user select a backup file.
     */
    private fun performRestore() {
        // Create an intent to open a document (e.g., a JSON file)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE) // Only URI that can be opened as a stream
            type = "application/json" // Look for JSON files
        }
        restoreFileLauncher.launch(intent)
    }

    /**
     * Reads data from the selected URI and restores it into the database.
     * @param uri The URI of the backup file to read.
     */
    private fun readAndRestoreData(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) { // Perform I/O operation on a background thread
            try {
                val stringBuilder = StringBuilder()
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                        }
                    }
                }
                val jsonString = stringBuilder.toString()
                val backupData = JsonUtil.deserializeBackupData(jsonString)

                if (backupData != null) {
                    // Restore data to the database
                    // For simplicity, we'll clear existing data and insert new data.
                    // For a real app, you might want a more sophisticated merge/update strategy.
                    BookmarkDatabase.getDatabase(requireContext()).bookmarkDao().deleteAll()
                    BookmarkDatabase.getDatabase(requireContext()).collectionDao().deleteAll()

                    backupData.bookmarks.forEach { bookmark ->
                        BookmarkDatabase.getDatabase(requireContext()).bookmarkDao().insert(bookmark.copy(id = 0))
                    }
                    backupData.collections.forEach { collection ->
                        BookmarkDatabase.getDatabase(requireContext()).collectionDao().insert(collection.copy(id = 0))
                    }

                    withContext(Dispatchers.Main) {
                        showSnackbar("Restore successful from ${uri.lastPathSegment}!")
                        Log.d(TAG, "Data restored from: $uri")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showSnackbar("Restore failed: Invalid backup file format.")
                        Log.e(TAG, "Restore failed: Invalid backup file format.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Restore failed: ${e.message}")
                    Log.e(TAG, "Restore failed", e)
                }
            }
        }
    }

    /**
     * Helper function to display a Snackbar message.
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(
            requireActivity().findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    /**
     * Shows the theme selection dialog.
     */
    private fun showThemeSelectionDialog() {
        ThemeSelectionDialogFragment.newInstance { themeMode ->
            ThemeManager.setThemeMode(requireContext(), themeMode)
            showSnackbar(getString(R.string.theme_changed))
            // Restart the activity to apply the theme changes
            requireActivity().recreate()
        }.show(childFragmentManager, "theme_selection_dialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called for SettingsFragment.")
        _binding = null
    }
}
