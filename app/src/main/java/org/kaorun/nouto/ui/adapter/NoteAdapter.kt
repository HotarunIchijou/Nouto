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
import com.google.android.material.listitem.ListItemViewHolder
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.ItemNoteBinding
import com.google.android.material.listitem.ListItemCardView.SwipeCallback
import com.google.android.material.listitem.RevealableListItem
import com.google.android.material.listitem.SwipeableListItem
import com.google.android.material.listitem.SwipeableListItem.STATE_SWIPE_PRIMARY_ACTION
import org.kaorun.nouto.R
import org.kaorun.nouto.data.PreferenceAppearanceKeys

class NoteAdapter(
    private val onItemClick: (Note) -> Unit,
    private val onDeleteClick: (Note) -> Unit,
    private val onRestoreClick: ((Note) -> Unit)? = null
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {
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
                        bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        if (onRestoreClick != null && gravity == Gravity.END) {
                            onRestoreClick(note)
                        } else {
                            onDeleteClick(note)
                        }
                    }
                }
            })
            binding.cardView.setOnClickListener { onItemClick(note) }
            binding.buttonStart.setOnClickListener { onDeleteClick(note) }
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
                /* source = */ (
                    if (isTitleVisible && !note.title.isNullOrBlank()) note.title
                    else if (note.content.isNullOrBlank()) note.title
                    else note.content
                )!!,
                /* flags = */ HtmlCompat.FROM_HTML_MODE_COMPACT
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

            if (onRestoreClick != null) {
                binding.buttonStart.setIconResource(R.drawable.delete_forever_24px)
                binding.buttonEnd.setIconResource(R.drawable.restore_from_trash_24px)

                binding.buttonEnd.backgroundTintList = ColorStateList.valueOf(
                    MaterialColors.getColor(
                        binding.buttonEnd,
                        androidx.appcompat.R.attr.colorPrimary
                    )
                )
                binding.buttonEnd.iconTint = ColorStateList.valueOf(
                    MaterialColors.getColor(
                        binding.buttonEnd,
                        com.google.android.material.R.attr.colorOnPrimary
                    )
                )
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NoteViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
        holder.bind(position, itemCount)
    }
}