package com.bookmarklocker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.bookmarklocker.databinding.ActivityMainBinding
import com.bookmarklocker.data.Bookmark

/**
 * MainActivity: The primary activity for the Bookmark Locker application.
 * This activity is responsible for setting up the navigation architecture using
 * the Navigation Component. It hosts a NavHostFragment which manages the display
 * of different UI fragments (Bookmarks, Collections, Settings).
 * A BottomNavigationView is used to allow users to easily switch between these main sections.
 *
 * In Step 3, this activity was updated to handle incoming ACTION_SEND intents.
 * In Step 6, it ensures new Bookmark objects from share intents are correctly
 * passed for pre-filling the dialog.
 */
class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply the saved theme before setting the content view
        ThemeManager.applyThemeToActivity(this)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNavView: BottomNavigationView = binding.bottomNavView
        bottomNavView.setupWithNavController(navController)

        // Custom navigation: If in Manage Tags and Settings is tapped, pop back to main Settings
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.settingsFragment -> {
                    val current = navController.currentDestination?.id
                    if (current == R.id.tagsManagementFragment) {
                        navController.popBackStack(R.id.settingsFragment, false)
                        true
                    } else {
                        navController.navigate(R.id.settingsFragment)
                        true
                    }
                }
                R.id.bookmarksFragment -> {
                    navController.navigate(R.id.bookmarksFragment)
                    true
                }
                R.id.collectionsFragment -> {
                    navController.navigate(R.id.collectionsFragment)
                    true
                }
                else -> false
            }
        }

        // Handle the intent if the activity was launched via a share action
        handleIntent(intent)
    }

    /**
     * Called when the activity receives a new intent (e.g., from a share action while already running).
     * @param intent The new Intent that was started for the activity.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /**
     * Processes the incoming Intent to check for shared URLs.
     * If an ACTION_SEND intent with text/plain or text/html is found, it extracts the URL
     * and pre-fills the AddEditBookmarkDialogFragment.
     * @param intent The Intent to process.
     */
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.action == Intent.ACTION_SEND && (it.type == "text/plain" || it.type == "text/html")) {
                val sharedText = it.getStringExtra(Intent.EXTRA_TEXT)
                val sharedTitle = it.getStringExtra(Intent.EXTRA_SUBJECT)

                Log.d(TAG, "Received ACTION_SEND intent.")
                Log.d(TAG, "Shared Text: $sharedText")
                Log.d(TAG, "Shared Title: $sharedTitle")

                // Try to extract a valid URL from the shared text
                val urlRegex = "(https?://[^\\s]+)" // Simple regex for URLs
                val foundUrl = sharedText?.let { text ->
                    urlRegex.toRegex().find(text)?.value
                }

                if (foundUrl != null) {
                    // Navigate to the Bookmarks fragment if not already there,
                    // and then show the dialog with pre-filled data.
                    navController.navigate(R.id.bookmarksFragment)
                    // For a new bookmark from share, pass a Bookmark object with default ID (0)
                    // The dialog will recognize ID 0 as a new bookmark for pre-filling.
                    val tempBookmark = Bookmark(url = foundUrl, title = sharedTitle, notes = null)
                    showAddEditBookmarkDialogFromShare(tempBookmark)
                } else {
                    Log.w(TAG, "No valid URL found in shared text.")
                    // Optionally, show a toast or message to the user that no URL was found.
                }

                // Clear the intent to prevent it from being processed again
                intent.replaceExtras(Bundle())
                intent.setAction("")
                intent.setData(null)
                intent.setFlags(0)
            }
        }
    }

    /**
     * Shows the AddEditBookmarkDialogFragment with pre-filled data from a share intent.
     * @param bookmark A temporary Bookmark object containing the URL, title, and notes from the shared intent.
     * Its ID will be 0, signaling a new bookmark to the dialog for pre-filling purposes.
     */
    private fun showAddEditBookmarkDialogFromShare(bookmark: Bookmark) {
        val dialog = AddEditBookmarkDialogFragment.newInstance(bookmark)
        dialog.show(supportFragmentManager, "AddEditBookmarkDialog")
    }
}
