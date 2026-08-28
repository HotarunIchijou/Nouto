package org.kaorun.nouto.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import org.kaorun.nouto.data.LibraryItem

class LibraryDiffCallback : DiffUtil.ItemCallback<LibraryItem>() {
    override fun areItemsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
        return when {
            oldItem is LibraryItem.FolderItem && newItem is LibraryItem.FolderItem ->
                oldItem.folder.id == newItem.folder.id
            oldItem is LibraryItem.ResourceItem && newItem is LibraryItem.ResourceItem ->
                oldItem.resource.id == newItem.resource.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
        return oldItem == newItem
    }
}
