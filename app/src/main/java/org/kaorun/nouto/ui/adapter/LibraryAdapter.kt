package org.kaorun.nouto.ui.adapter

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.listitem.ListItemCardView.SwipeCallback
import com.google.android.material.listitem.ListItemViewHolder
import com.google.android.material.listitem.RevealableListItem
import com.google.android.material.listitem.SwipeableListItem
import com.google.android.material.listitem.SwipeableListItem.STATE_SWIPE_PRIMARY_ACTION
import org.kaorun.nouto.R
import org.kaorun.nouto.data.LibraryItem
import org.kaorun.nouto.databinding.ItemLibraryBinding

class LibraryAdapter(
    private val onItemClick: (LibraryItem) -> Unit,
    private val onDeleteClick: (LibraryItem) -> Unit,
    private val onRenameClick: (LibraryItem) -> Unit,
    private val onMoveClick: (LibraryItem) -> Unit
) : ListAdapter<LibraryItem, LibraryAdapter.LibraryViewHolder>(LibraryDiffCallback()) {

    inner class LibraryViewHolder(
        private val binding: ItemLibraryBinding
    ) : ListItemViewHolder(binding.root) {
        private lateinit var item: LibraryItem

        init {
            binding.cardView.addSwipeCallback(object : SwipeCallback() {
                override fun onSwipe(p0: Int) {}
                override fun <T> onSwipeStateChanged(
                    newState: Int,
                    activeRevealableListItem: T,
                    gravity: Int
                ) where T : View, T : RevealableListItem {
                    if (newState == STATE_SWIPE_PRIMARY_ACTION &&
                        bindingAdapterPosition != RecyclerView.NO_POSITION
                    ) {
                        onDeleteClick(item)
                    }
                }
            })

            binding.cardView.setOnClickListener { onItemClick(item) }
            binding.buttonStart.setOnClickListener { onDeleteClick(item) }
            binding.buttonEnd.setOnClickListener { onDeleteClick(item) }

            binding.buttonMenu.setOnClickListener { view ->
                showPopupMenu(view)
            }

            binding.cardView.setOnLongClickListener { view ->
                showPopupMenu(view)
                true
            }
        }

        private fun showPopupMenu(view: View) {
            val popup = PopupMenu(view.context, view, Gravity.END, 0, R.style.Widget_Custom_PopupMenu)
            popup.menu.add(0, 1, 0, R.string.rename).setIcon(R.drawable.edit_24px)
            popup.menu.add(0, 2, 1, R.string.move).setIcon(R.drawable.drive_file_move_24px)
            popup.menu.add(0, 3, 2, R.string.delete).setIcon(R.drawable.delete_24px)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> {
                        onRenameClick(item)
                        true
                    }
                    2 -> {
                        onMoveClick(item)
                        true
                    }
                    3 -> {
                        onDeleteClick(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        fun bind(currentItem: LibraryItem) {
            listOf(Gravity.START, Gravity.END).forEach {
                binding.root.setSwipeState(SwipeableListItem.STATE_CLOSED, it)
            }
            item = currentItem

            binding.itemTitle.text = item.name

            binding.cardView.setCardBackgroundColor(
                ColorStateList.valueOf(
                    MaterialColors.getColor(
                        binding.cardView,
                        com.google.android.material.R.attr.colorSurfaceBright
                    )
                )
            )
            binding.itemTitle.setTextColor(
                ColorStateList.valueOf(
                    MaterialColors.getColor(
                        binding.itemTitle,
                        com.google.android.material.R.attr.colorOnSurface
                    )
                )
            )

            when (currentItem) {
                is LibraryItem.FolderItem -> {
                    binding.itemIcon.setImageResource(R.drawable.folder_24px)
                    binding.itemIcon.imageTintList = ColorStateList.valueOf(
                        MaterialColors.getColor(
                            binding.itemIcon,
                            androidx.appcompat.R.attr.colorPrimary
                        )
                    )
                }
                is LibraryItem.ResourceItem -> {
                    val resource = currentItem.resource
                    val iconRes = when (resource.resourceType) {
                        "LINK" -> R.drawable.link_24px
                        else -> getIconForMimeType(resource.mimeType, resource.originalName)
                    }
                    binding.itemIcon.setImageResource(iconRes)
                    binding.itemIcon.imageTintList = ColorStateList.valueOf(
                        MaterialColors.getColor(
                            binding.itemIcon,
                            if (resource.resourceType == "LINK")
                                com.google.android.material.R.attr.colorTertiary
                            else
                                androidx.appcompat.R.attr.colorPrimary
                        )
                    )
                }
            }
        }

        private fun getIconForMimeType(mimeType: String?, originalName: String): Int {
            val lowerMime = mimeType?.lowercase() ?: ""
            val lowerName = originalName.lowercase()

            return when {
                lowerMime.contains("pdf") || lowerName.endsWith(".pdf") -> R.drawable.picture_as_pdf_24px
                lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif") -> R.drawable.image_24px
                lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".webm") || lowerName.endsWith(".avi") -> R.drawable.video_file_24px
                lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a") || lowerName.endsWith(".flac") || lowerName.endsWith(".ogg") -> R.drawable.audio_file_24px
                lowerMime.contains("presentation") || lowerMime.contains("powerpoint") || lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") -> R.drawable.slideshow_24px
                else -> R.drawable.description_24px
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryViewHolder {
        val binding = ItemLibraryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LibraryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LibraryViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.bind(position, itemCount)
    }
}
