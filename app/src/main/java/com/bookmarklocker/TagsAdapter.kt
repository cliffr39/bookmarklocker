package com.bookmarklocker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bookmarklocker.databinding.ChipTagEditableBinding

/**
 * TagsAdapter: RecyclerView adapter for displaying and managing tags in the TagsManagementFragment.
 * This adapter handles the display of editable tag chips and provides callbacks for tag operations.
 */
class TagsAdapter(
    private val onTagDelete: (String) -> Unit,
    private val onTagEdit: (String, String) -> Unit
) : ListAdapter<String, TagsAdapter.TagViewHolder>(TagDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ChipTagEditableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TagViewHolder(
        private val binding: ChipTagEditableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: String) {
            binding.root.text = tag
            binding.root.setOnCloseIconClickListener {
                onTagDelete(tag)
            }
            
            // Set up click listener for editing (long press to edit)
            binding.root.setOnLongClickListener {
                // For now, we'll just show a simple edit dialog
                // In a more sophisticated implementation, you might want to show a custom dialog
                onTagEdit(tag, tag)
                true
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    private class TagDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
} 