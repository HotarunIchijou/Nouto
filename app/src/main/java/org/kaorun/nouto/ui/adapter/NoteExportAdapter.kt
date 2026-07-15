package org.kaorun.nouto.ui.adapter

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.color.MaterialColors
import com.google.android.material.listitem.ListItemViewHolder
import com.google.android.material.listitem.SwipeableListItem
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.ItemNoteBinding

class NoteExportAdapter(
    private val onItemClick: (Note) -> Unit
) : ListAdapter<Note, NoteExportAdapter.NoteExportViewHolder>(NoteDiffCallback()) {
    inner class NoteExportViewHolder(
        private val binding: ItemNoteBinding
    ) : ListItemViewHolder(binding.root) {
        private lateinit var note: Note

        init {
            binding.cardView.setOnClickListener { onItemClick(note) }
        }

        fun bind(currentNote: Note) {
            listOf(Gravity.START, Gravity.END).forEach {
                binding.root.setSwipeState(SwipeableListItem.STATE_CLOSED, it)
            }
            note = currentNote
            binding.noteTitle.text = HtmlCompat.fromHtml(
                (if (note.title.isNullOrBlank()) note.content else note.title)!!,
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteExportViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteExportViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NoteExportViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
        holder.bind(position, itemCount)
    }
}