package org.kaorun.nouto.ui.components

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kaorun.nouto.R
import org.kaorun.nouto.data.Folder
import org.kaorun.nouto.data.LibraryResource
import org.kaorun.nouto.data.NoteDatabase
import org.kaorun.nouto.databinding.BottomSheetShareImportBinding
import org.kaorun.nouto.databinding.ItemFolderSelectBinding
import org.kaorun.nouto.repository.LibraryRepository

class ShareImportBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetShareImportBinding? = null
    private val binding get() = _binding!!

    private var sharedUris: List<Uri> = emptyList()
    private var sharedText: String? = null
    private var selectedFolderId: Long? = null
    private var allFolders: List<Folder> = emptyList()
    private lateinit var folderAdapter: FolderSelectAdapter
    private lateinit var repository: LibraryRepository
    var onImportComplete: ((targetFolderId: Long?) -> Unit)? = null

    companion object {
        private const val ARG_URIS = "arg_uris"
        private const val ARG_TEXT = "arg_text"

        fun newInstance(uris: ArrayList<Uri>?, text: String?): ShareImportBottomSheet {
            return ShareImportBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_URIS, uris)
                    putString(ARG_TEXT, text)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_URIS, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList<Uri>(ARG_URIS)
        }
        sharedUris = uris ?: emptyList()
        sharedText = arguments?.getString(ARG_TEXT)
        val dao = NoteDatabase.getDatabase(requireContext().applicationContext).libraryDao()
        repository = LibraryRepository(dao)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetShareImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupPreview()
        setupFolderList()
        setupListeners()
        loadFolders()
    }

    private fun setupPreview() {
        if (sharedUris.isNotEmpty()) {
            if (sharedUris.size == 1) {
                val (name, mime) = getFileInfo(requireContext(), sharedUris[0])
                binding.previewText.text = name
                binding.previewIcon.setImageResource(getIconForMime(mime, name))
            } else {
                binding.previewText.text = getString(R.string.files_count, sharedUris.size)
                binding.previewIcon.setImageResource(R.drawable.description_24px)
            }
        } else if (!sharedText.isNullOrBlank()) {
            binding.previewText.text = sharedText
            binding.previewIcon.setImageResource(R.drawable.link_24px)
        }
    }

    private fun setupFolderList() {
        folderAdapter = FolderSelectAdapter(
            selectedFolderId = selectedFolderId,
            onFolderSelected = { folderId ->
                selectedFolderId = folderId
                folderAdapter.setSelectedId(folderId)
            }
        )
        binding.folderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.folderRecyclerView.adapter = folderAdapter
    }

    private fun loadFolders() {
        lifecycleScope.launch {
            allFolders = withContext(Dispatchers.IO) {
                repository.getAllFoldersSync()
            }
            folderAdapter.submitList(allFolders)
        }
    }

    private fun setupListeners() {
        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        binding.buttonNewFolder.setOnClickListener {
            LibraryDialogs.showCreateFolderDialog(requireContext(), resources) { folderName ->
                lifecycleScope.launch {
                    val newId = withContext(Dispatchers.IO) {
                        repository.insertFolder(folderName, selectedFolderId)
                    }
                    selectedFolderId = newId
                    loadFolders()
                }
            }
        }

        binding.buttonAdd.setOnClickListener {
            performImport()
        }
    }

    private fun performImport() {
        binding.buttonAdd.isEnabled = false
        val context = requireContext().applicationContext
        val folderId = selectedFolderId

        lifecycleScope.launch(Dispatchers.IO) {
            if (sharedUris.isNotEmpty()) {
                val resources = mutableListOf<LibraryResource>()
                for (uri in sharedUris) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {}

                    val (name, mime) = getFileInfo(context, uri)
                    resources.add(
                        LibraryResource(
                            folderId = folderId,
                            displayName = name,
                            originalName = name,
                            resourceType = "FILE",
                            mimeType = mime,
                            uriString = uri.toString()
                        )
                    )
                }
                repository.insertResources(resources)
            } else if (!sharedText.isNullOrBlank()) {
                val text = sharedText!!.trim()
                val isUrl = text.startsWith("http://", ignoreCase = true) ||
                        text.startsWith("https://", ignoreCase = true) ||
                        text.contains(".")
                val finalUrl = if (isUrl && !text.startsWith("http://", ignoreCase = true) &&
                    !text.startsWith("https://", ignoreCase = true)) {
                    "https://$text"
                } else {
                    text
                }
                repository.insertResource(
                    folderId = folderId,
                    displayName = text,
                    originalName = finalUrl,
                    resourceType = if (isUrl) "LINK" else "FILE",
                    mimeType = if (isUrl) "text/uri-list" else "text/plain",
                    uriString = finalUrl
                )
            }

            withContext(Dispatchers.Main) {
                dismiss()
                onImportComplete?.invoke(folderId)
            }
        }
    }

    private fun getFileInfo(context: Context, uri: Uri): Pair<String, String?> {
        var name = "Document"
        var mimeType = context.contentResolver.getType(uri)

        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val str = it.getString(nameIndex)
                        if (!str.isNullOrBlank()) {
                            name = str
                        }
                    }
                }
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            uri.lastPathSegment?.let { name = it }
        }

        if (mimeType == null) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (!extension.isNullOrBlank()) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            }
        }
        return Pair(name, mimeType)
    }

    private fun getIconForMime(mimeType: String?, name: String): Int {
        val lowerMime = mimeType?.lowercase() ?: ""
        val lowerName = name.lowercase()
        return when {
            lowerMime.contains("pdf") || lowerName.endsWith(".pdf") -> R.drawable.picture_as_pdf_24px
            lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".png") -> R.drawable.image_24px
            lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") -> R.drawable.video_file_24px
            lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") -> R.drawable.audio_file_24px
            lowerMime.contains("presentation") || lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") -> R.drawable.slideshow_24px
            else -> R.drawable.description_24px
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class FolderSelectAdapter(
        private var selectedFolderId: Long?,
        private val onFolderSelected: (Long?) -> Unit
    ) : RecyclerView.Adapter<FolderSelectAdapter.FolderViewHolder>() {

        private val items = mutableListOf<FolderOption>()

        init {
            rebuildItems()
        }

        fun setSelectedId(id: Long?) {
            selectedFolderId = id
            notifyDataSetChanged()
        }

        fun submitList(folders: List<Folder>) {
            allFolders = folders
            rebuildItems()
            notifyDataSetChanged()
        }

        private fun rebuildItems() {
            items.clear()
            items.add(FolderOption(id = null, name = getString(R.string.library_root)))
            allFolders.forEach { folder ->
                items.add(FolderOption(id = folder.id, name = folder.name))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
            val binding = ItemFolderSelectBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return FolderViewHolder(binding)
        }

        override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class FolderViewHolder(
            private val binding: ItemFolderSelectBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(option: FolderOption) {
                binding.folderName.text = option.name
                val isSelected = option.id == selectedFolderId
                binding.radioButton.isChecked = isSelected
                binding.folderCard.isChecked = isSelected

                val clickListener = View.OnClickListener {
                    onFolderSelected(option.id)
                }
                binding.folderCard.setOnClickListener(clickListener)
                binding.radioButton.setOnClickListener(clickListener)
            }
        }
    }

    data class FolderOption(val id: Long?, val name: String)
}
