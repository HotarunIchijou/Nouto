package org.kaorun.nouto.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.kaorun.nouto.R
import org.kaorun.nouto.data.HomeItem
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.databinding.BottomSheetItemOptionsBinding

class ItemOptionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetItemOptionsBinding? = null
    private val binding get() = _binding!!

    var item: HomeItem? = null
    var onPinToggle: ((Note) -> Unit)? = null
    var onRename: ((HomeItem) -> Unit)? = null
    var onMove: ((HomeItem) -> Unit)? = null
    var onDelete: ((HomeItem) -> Unit)? = null

    companion object {
        fun newInstance(item: HomeItem): ItemOptionsBottomSheet {
            return ItemOptionsBottomSheet().apply {
                this.item = item
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetItemOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.parent as? View)?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        val currentItem = item ?: run {
            dismiss()
            return
        }

        setupItemHeader(currentItem)
        setupActions(currentItem)
    }

    private fun setupItemHeader(item: HomeItem) {
        when (item) {
            is HomeItem.FolderItem -> {
                binding.headerIcon.setImageResource(R.drawable.folder_24px)
                binding.headerTitle.text = item.folder.name
                binding.optionPin.isVisible = false
                binding.optionRename.isVisible = true
            }
            is HomeItem.NoteItem -> {
                binding.headerIcon.setImageResource(R.drawable.notes_24px)
                val title = if (!item.note.title.isNullOrBlank()) {
                    item.note.title
                } else if (!item.note.content.isNullOrBlank()) {
                    item.note.content
                } else {
                    getString(R.string.empty_title)
                }
                binding.headerTitle.text = title
                binding.optionPin.isVisible = true
                binding.optionRename.isVisible = false

                if (item.note.isPinned) {
                    binding.pinIcon.setImageResource(R.drawable.keep_off_24px)
                    binding.pinText.setText(R.string.unpin)
                } else {
                    binding.pinIcon.setImageResource(R.drawable.keep_24px)
                    binding.pinText.setText(R.string.pin)
                }
            }
            is HomeItem.ResourceItem -> {
                val res = item.resource
                val iconRes = when (res.resourceType) {
                    "LINK" -> R.drawable.link_24px
                    else -> getIconForMime(res.mimeType, res.originalName)
                }
                binding.headerIcon.setImageResource(iconRes)
                binding.headerTitle.text = res.displayName
                binding.optionPin.isVisible = false
                binding.optionRename.isVisible = true
            }
        }
    }

    private fun setupActions(item: HomeItem) {
        binding.optionPin.setOnClickListener {
            dismiss()
            if (item is HomeItem.NoteItem) {
                onPinToggle?.invoke(item.note)
            }
        }

        binding.optionRename.setOnClickListener {
            dismiss()
            onRename?.invoke(item)
        }

        binding.optionMove.setOnClickListener {
            dismiss()
            onMove?.invoke(item)
        }

        binding.optionDelete.setOnClickListener {
            dismiss()
            onDelete?.invoke(item)
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
