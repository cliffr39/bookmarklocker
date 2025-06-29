package com.bookmarklocker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bookmarklocker.data.Bookmark
import com.bookmarklocker.databinding.ItemBookmarkBinding
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log // ADDED: Import for Log

/**
 * BookmarkAdapter: A RecyclerView.Adapter for displaying a list of bookmarks.
 * It uses ListAdapter (which uses DiffUtil) for efficient updates when the list of bookmarks changes.
 * This adapter binds Bookmark data to the views defined in item_bookmark.xml.
 *
 * Updated in Step 6 to:
 * - Display tags using a FlexboxLayout and Chip views.
 * - Show an overflow menu for each bookmark with Edit, Delete, and Share options.
 */
class BookmarkAdapter(
    private val onDeleteClick: (Bookmark) -> Unit, // Callback for delete action
    private val onEditClick: (Bookmark) -> Unit,   // Callback for edit action
    private val onItemClick: (Bookmark) -> Unit,    // Callback for regular item click (e.g., open URL)
    private val isMultiSelectMode: Boolean = false,
    private val selectedIds: Set<Int> = emptySet(),
    private val onToggleSelect: ((Bookmark) -> Unit)? = null
) : ListAdapter<Bookmark, BookmarkAdapter.BookmarkViewHolder>(BookmarksComparator()) {

    /**
     * Creates and returns a new BookmarkViewHolder instance.
     * This method is called when the RecyclerView needs a new ViewHolder to represent an item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookmarkViewHolder(binding, onDeleteClick, onEditClick, onItemClick)
    }

    /**
     * Binds the data from a Bookmark object to the views within a BookmarkViewHolder.
     * This method is called to display the data at the specified position.
     */
    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        val current = getItem(position)
        val isSelected = isMultiSelectMode && selectedIds.contains(current.id)
        holder.bind(current, isSelected, isMultiSelectMode, onToggleSelect)
    }

    /**
     * BookmarkViewHolder: A ViewHolder that holds the views for a single bookmark item.
     * It uses ViewBinding to safely access the views in item_bookmark.xml.
     */
    class BookmarkViewHolder(
        private val binding: ItemBookmarkBinding,
        private val onDeleteClick: (Bookmark) -> Unit,
        private val onEditClick: (Bookmark) -> Unit,
        private val onItemClick: (Bookmark) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds a Bookmark object's data to the views in the item layout.
         *
         * @param bookmark The Bookmark object containing the data to display.
         */
        fun bind(
            bookmark: Bookmark,
            isSelected: Boolean = false,
            isMultiSelectMode: Boolean = false,
            onToggleSelect: ((Bookmark) -> Unit)? = null
        ) {
            // Set the bookmark title, defaulting to URL if title is empty/null
            val displayTitle = if (bookmark.title.isNullOrBlank()) bookmark.url else bookmark.title
            binding.bookmarkTitle.text = displayTitle

            // Set the URL
            binding.bookmarkUrl.text = bookmark.url
            binding.bookmarkUrl.setOnClickListener { // Make URL clickable
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url))
                    it.context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle cases where the URL is invalid or no app can handle it
                    // e.g., Toast.makeText(it.context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                    Log.e("BookmarkAdapter", "Failed to open URL: ${bookmark.url}", e)
                }
            }

            // Set notes, hide if empty/null
            if (bookmark.notes.isNullOrBlank()) {
                binding.bookmarkNotes.visibility = View.GONE
            } else {
                binding.bookmarkNotes.visibility = View.VISIBLE
                binding.bookmarkNotes.text = bookmark.notes
            }

            // Hide Tags in the list
            binding.flexboxTags.visibility = View.GONE

            // Hide Timestamp in the list
            binding.bookmarkTimestamp.visibility = View.GONE

            // Remove checkmark and selection background logic
            val isDarkMode = (binding.root.context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (isDarkMode) {
                binding.root.setCardBackgroundColor(binding.root.context.getColor(R.color.bookmark_card_bg))
                binding.bookmarkTitle.setTextColor(binding.root.context.getColor(R.color.bookmark_title_dark))
                binding.bookmarkUrl.setTextColor(binding.root.context.getColor(R.color.bookmark_url_dark))
            } // In light mode, do not set colors programmatically; use XML defaults

            // Set up the More Options button (overflow menu)
            binding.btnMoreOptions.setOnClickListener {
                showPopupMenu(it.context, it, bookmark)
            }

            // Click logic
            binding.root.setOnClickListener {
                if (isMultiSelectMode && onToggleSelect != null) {
                    onToggleSelect(bookmark)
                } else {
                    onItemClick(bookmark)
                }
            }
        }

        /**
         * Displays a PopupMenu with options for the bookmark (Edit, Delete, Share).
         * @param context The context from which the menu is being shown.
         * @param view The anchor view for the popup menu (the btn_more_options ImageView).
         * @param bookmark The Bookmark object associated with this menu.
         */
        private fun showPopupMenu(context: Context, view: View, bookmark: Bookmark) {
            val popup = PopupMenu(context, view)
            popup.menuInflater.inflate(R.menu.bookmark_item_menu, popup.menu) // Inflate the menu defined in XML

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit_bookmark -> {
                        onEditClick(bookmark) // Trigger the edit callback
                        true
                    }
                    R.id.action_delete_bookmark -> {
                        onDeleteClick(bookmark) // Trigger the delete callback
                        true
                    }
                    R.id.action_share_bookmark -> {
                        shareBookmark(context, bookmark) // Call the share function
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        /**
         * Shares the bookmark's URL and title using a standard Android share intent.
         * @param context The context for starting the activity.
         * @param bookmark The Bookmark object to share.
         */
        private fun shareBookmark(context: Context, bookmark: Bookmark) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, bookmark.url)
                putExtra(Intent.EXTRA_SUBJECT, bookmark.title ?: bookmark.url) // Use title or URL as subject
            }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_bookmark_via)))
        }
    }

    /**
     * BookmarksComparator: A DiffUtil.ItemCallback implementation used by ListAdapter
     * to calculate the difference between two lists of bookmarks. This helps in efficient
     * updates of the RecyclerView, avoiding full redraws.
     */
    class BookmarksComparator : DiffUtil.ItemCallback<Bookmark>() {
        /**
         * Called to check whether two objects represent the same item.
         * Used to detect if an item was added, removed, or moved.
         */
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem.id == newItem.id // Items are the same if their IDs are identical
        }

        /**
         * Called to check whether two items have the same data content.
         * Used to detect if the contents of an item have changed.
         */
        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem == newItem // Data is the same if all properties are equal (data class equality)
        }
    }
}
