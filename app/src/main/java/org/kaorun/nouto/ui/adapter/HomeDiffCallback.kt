package org.kaorun.nouto.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import org.kaorun.nouto.data.HomeItem

class HomeDiffCallback : DiffUtil.ItemCallback<HomeItem>() {
    override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
        return when {
            oldItem is HomeItem.FolderItem && newItem is HomeItem.FolderItem ->
                oldItem.folder.id == newItem.folder.id
            oldItem is HomeItem.NoteItem && newItem is HomeItem.NoteItem ->
                oldItem.note.id == newItem.note.id
            oldItem is HomeItem.ResourceItem && newItem is HomeItem.ResourceItem ->
                oldItem.resource.id == newItem.resource.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
        return oldItem == newItem
    }
}
