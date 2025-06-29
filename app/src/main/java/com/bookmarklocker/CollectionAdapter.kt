package com.bookmarklocker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bookmarklocker.data.Collection
import com.bookmarklocker.databinding.ItemCollectionBinding

/**
 * CollectionAdapter: A RecyclerView.Adapter for displaying a list of collections.
 * It uses ListAdapter (which uses DiffUtil) for efficient updates when the list of collections changes.
 * This adapter binds Collection data to the views defined in item_collection.xml.
 */
class CollectionAdapter(private val onItemClick: (Collection) -> Unit, private val onEditClick: (Collection) -> Unit) :
    ListAdapter<Collection, CollectionAdapter.CollectionViewHolder>(CollectionsComparator()) {

    /**
     * Creates and returns a new CollectionViewHolder instance.
     * This method is called when the RecyclerView needs a new ViewHolder to represent an item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        // Inflate the item_collection.xml layout using ViewBinding for a specific item view.
        val binding = ItemCollectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CollectionViewHolder(binding)
    }

    /**
     * Binds the data from a Collection object to the views within a CollectionViewHolder.
     * This method is called to display the data at the specified position.
     */
    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        val current = getItem(position) // Get the Collection object for the current position
        holder.bind(current, onItemClick, onEditClick) // Call the bind method of the ViewHolder to populate views
    }

    /**
     * CollectionViewHolder: A ViewHolder that holds the views for a single collection item.
     * It uses ViewBinding to safely access the views in item_collection.xml.
     */
    class CollectionViewHolder(private val binding: ItemCollectionBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * Binds a Collection object's data to the views in the item layout.
         *
         * @param collection The Collection object containing the data to display.
         * @param onItemClick Lambda to be called when the item is clicked.
         * @param onEditClick Lambda to be called when the edit button is clicked.
         */
        fun bind(collection: Collection, onItemClick: (Collection) -> Unit, onEditClick: (Collection) -> Unit) {
            android.util.Log.d("CollectionAdapter", "Binding collection: name='${collection.name}', color='${collection.color}'")
            // Show a fallback if the name is blank
            binding.collectionName.text = if (collection.name.isNullOrBlank()) "Untitled" else collection.name

            // Set the folder icon tint based on the collection's color, fallback to blue if missing/invalid
            val colorString = if (collection.color.isNullOrBlank()) "#448AFF" else collection.color
            try {
                binding.folderIcon.setColorFilter(Color.parseColor(colorString))
            } catch (e: IllegalArgumentException) {
                binding.folderIcon.setColorFilter(Color.parseColor("#448AFF"))
            }

            // Set click listener for the entire item
            binding.root.setOnClickListener {
                onItemClick(collection)
            }

            // Set click listener for the edit icon
            binding.editCollectionIcon.setOnClickListener {
                onEditClick(collection)
            }
        }
    }

    /**
     * CollectionsComparator: A DiffUtil.ItemCallback implementation used by ListAdapter
     * to calculate the difference between two lists of collections. This helps in efficient
     * updates of the RecyclerView, avoiding full redraws.
     */
    class CollectionsComparator : DiffUtil.ItemCallback<Collection>() {
        /**
         * Called to check whether two objects represent the same item.
         * Used to detect if an item was added, removed, or moved.
         */
        override fun areItemsTheSame(oldItem: Collection, newItem: Collection): Boolean {
            return oldItem.id == newItem.id // Items are the same if their IDs are identical
        }

        /**
         * Called to check whether two items have the same data content.
         * Used to detect if the contents of an item have changed.
         */
        override fun areContentsTheSame(oldItem: Collection, newItem: Collection): Boolean {
            return oldItem == newItem // Data is the same if all properties are equal (data class equality)
        }
    }
}
