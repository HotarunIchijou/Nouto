package org.kaorun.nouto.ui.adapter

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.listitem.ListItemCardView.SwipeCallback
import com.google.android.material.listitem.ListItemViewHolder
import com.google.android.material.listitem.RevealableListItem
import com.google.android.material.listitem.SwipeableListItem
import com.google.android.material.listitem.SwipeableListItem.STATE_SWIPE_PRIMARY_ACTION
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Folder
import org.kaorun.nouto.data.HomeItem
import org.kaorun.nouto.data.LibraryResource
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.data.PreferenceAppearanceKeys
import org.kaorun.nouto.databinding.ItemFolderBinding
import org.kaorun.nouto.databinding.ItemNoteBinding
import org.kaorun.nouto.databinding.ItemResourceBinding

class HomeAdapter(
    private val onItemClick: (HomeItem) -> Unit,
    private val onItemLongClick: (HomeItem) -> Unit,
    private val onDeleteClick: (HomeItem) -> Unit
) : ListAdapter<HomeItem, ListItemViewHolder>(HomeDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_NOTE = 1
        private const val VIEW_TYPE_FOLDER = 2
        private const val VIEW_TYPE_RESOURCE = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.NoteItem -> VIEW_TYPE_NOTE
            is HomeItem.FolderItem -> VIEW_TYPE_FOLDER
            is HomeItem.ResourceItem -> VIEW_TYPE_RESOURCE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_NOTE -> {
                val binding = ItemNoteBinding.inflate(inflater, parent, false)
                NoteViewHolder(binding)
            }
            VIEW_TYPE_FOLDER -> {
                val binding = ItemFolderBinding.inflate(inflater, parent, false)
                FolderViewHolder(binding)
            }
            else -> {
                val binding = ItemResourceBinding.inflate(inflater, parent, false)
                ResourceViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is NoteViewHolder -> holder.bind((item as HomeItem.NoteItem).note)
            is FolderViewHolder -> holder.bind((item as HomeItem.FolderItem).folder)
            is ResourceViewHolder -> holder.bind((item as HomeItem.ResourceItem).resource)
        }
        holder.bind(position, itemCount)
    }

    // --- ViewHolders ---

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : ListItemViewHolder(binding.root) {
        private lateinit var note: Note

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
                        onDeleteClick(HomeItem.NoteItem(note))
                    }
                }
            })
            binding.cardView.setOnClickListener {
                onItemClick(HomeItem.NoteItem(note))
            }
            binding.cardView.setOnLongClickListener {
                onItemLongClick(HomeItem.NoteItem(note))
                true
            }
            binding.buttonStart.setOnClickListener {
                onDeleteClick(HomeItem.NoteItem(note))
            }
            binding.buttonEnd.setOnClickListener {
                onDeleteClick(HomeItem.NoteItem(note))
            }
        }

        fun bind(currentNote: Note) {
            listOf(Gravity.START, Gravity.END).forEach {
                binding.root.setSwipeState(SwipeableListItem.STATE_CLOSED, it)
            }
            note = currentNote
            val isTitleVisible = PreferenceManager
                .getDefaultSharedPreferences(binding.root.context)
                .getBoolean(PreferenceAppearanceKeys.IS_SHOW_TITLE, true)

            binding.noteTitle.text = HtmlCompat.fromHtml(
                (if (isTitleVisible && !note.title.isNullOrBlank()) note.title
                else if (note.content.isNullOrBlank()) note.title
                else note.content) ?: "",
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )

            if (note.isPinned && !note.isDeleted) {
                binding.cardView.setCardBackgroundColor(
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            binding.cardView,
                            com.google.android.material.R.attr.colorSecondaryContainer
                        )
                    )
                )
                binding.noteTitle.setTextColor(
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            binding.noteTitle,
                            com.google.android.material.R.attr.colorOnSecondaryContainer
                        )
                    )
                )
            } else {
                binding.cardView.setCardBackgroundColor(
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            binding.cardView,
                            com.google.android.material.R.attr.colorSurfaceBright
                        )
                    )
                )
                binding.noteTitle.setTextColor(
                    ColorStateList.valueOf(
                        MaterialColors.getColor(
                            binding.noteTitle,
                            com.google.android.material.R.attr.colorOnSurface
                        )
                    )
                )
            }
        }
    }

    inner class FolderViewHolder(
        private val binding: ItemFolderBinding
    ) : ListItemViewHolder(binding.root) {
        private lateinit var folder: Folder

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
                        onDeleteClick(HomeItem.FolderItem(folder))
                    }
                }
            })
            binding.cardView.setOnClickListener {
                onItemClick(HomeItem.FolderItem(folder))
            }
            binding.cardView.setOnLongClickListener {
                onItemLongClick(HomeItem.FolderItem(folder))
                true
            }
            binding.buttonStart.setOnClickListener {
                onDeleteClick(HomeItem.FolderItem(folder))
            }
            binding.buttonEnd.setOnClickListener {
                onDeleteClick(HomeItem.FolderItem(folder))
            }
        }

        fun bind(currentFolder: Folder) {
            listOf(Gravity.START, Gravity.END).forEach {
                binding.root.setSwipeState(SwipeableListItem.STATE_CLOSED, it)
            }
            folder = currentFolder
            binding.folderName.text = folder.name

            binding.cardView.setCardBackgroundColor(
                ColorStateList.valueOf(
                    MaterialColors.getColor(
                        binding.cardView,
                        com.google.android.material.R.attr.colorSurfaceBright
                    )
                )
            )
            binding.folderName.setTextColor(
                ColorStateList.valueOf(
                    MaterialColors.getColor(
                        binding.folderName,
                        com.google.android.material.R.attr.colorOnSurface
                    )
                )
            )
            binding.folderIcon.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(
                    binding.folderIcon,
                    com.google.android.material.R.attr.colorTertiary
                )
            )
        }
    }

    inner class ResourceViewHolder(
        private val binding: ItemResourceBinding
    ) : ListItemViewHolder(binding.root) {
        private lateinit var resource: LibraryResource

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
                        onDeleteClick(HomeItem.ResourceItem(resource))
                    }
                }
            })
            binding.cardView.setOnClickListener {
                onItemClick(HomeItem.ResourceItem(resource))
            }
            binding.cardView.setOnLongClickListener {
                onItemLongClick(HomeItem.ResourceItem(resource))
                true
            }
            binding.buttonStart.setOnClickListener {
                onDeleteClick(HomeItem.ResourceItem(resource))
            }
            binding.buttonEnd.setOnClickListener {
                onDeleteClick(HomeItem.ResourceItem(resource))
            }
        }

        fun bind(currentResource: LibraryResource) {
            listOf(Gravity.START, Gravity.END).forEach {
                binding.root.setSwipeState(SwipeableListItem.STATE_CLOSED, it)
            }
            resource = currentResource
            binding.itemTitle.text = resource.displayName

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

            val iconRes = when (resource.resourceType) {
                "LINK" -> R.drawable.link_24px
                else -> getIconForMime(resource.mimeType, resource.originalName)
            }
            binding.itemIcon.setImageResource(iconRes)
            binding.itemIcon.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(
                    binding.itemIcon,
                    com.google.android.material.R.attr.colorTertiary
                )
            )
        }

        private fun getIconForMime(mimeType: String?, originalName: String): Int {
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
}
