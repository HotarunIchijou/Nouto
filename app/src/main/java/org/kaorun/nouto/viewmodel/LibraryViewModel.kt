package org.kaorun.nouto.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kaorun.nouto.data.Folder
import org.kaorun.nouto.data.HomeItem
import org.kaorun.nouto.data.LibraryResource
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.data.NoteDatabase
import org.kaorun.nouto.repository.LibraryRepository
import org.kaorun.nouto.repository.NoteRepository
import org.kaorun.nouto.repository.PreferenceRepository
import org.kaorun.nouto.ui.model.LayoutMode

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LibraryRepository
    private val noteRepository: NoteRepository
    private val preferenceRepository = PreferenceRepository(application)

    private val _currentFolderId = MutableLiveData<Long?>(null)
    val currentFolderId: LiveData<Long?> = _currentFolderId

    private val _layoutMode = MutableLiveData(preferenceRepository.getLayoutMode())
    val layoutMode: LiveData<LayoutMode> = _layoutMode

    private val _searchQuery = MutableLiveData<String?>(null)
    val searchQuery: LiveData<String?> = _searchQuery

    val currentFolder: LiveData<Folder?> = _currentFolderId.switchMap { folderId ->
        if (folderId == null || folderId == -1L) {
            MutableLiveData<Folder?>(null)
        } else {
            repository.getFolderById(folderId)
        }
    }

    val libraryItems = MediatorLiveData<List<HomeItem>>()

    private var currentFoldersSource: LiveData<List<Folder>>? = null
    private var currentNotesSource: LiveData<List<Note>>? = null
    private var currentResourcesSource: LiveData<List<LibraryResource>>? = null
    private var latestFolders: List<Folder> = emptyList()
    private var latestNotes: List<Note> = emptyList()
    private var latestResources: List<LibraryResource> = emptyList()

    init {
        val db = NoteDatabase.getDatabase(application)
        repository = LibraryRepository(db.libraryDao())
        noteRepository = NoteRepository(db.noteDao())

        libraryItems.addSource(_currentFolderId) { folderId ->
            updateSources(folderId)
        }
        libraryItems.addSource(_searchQuery) {
            combineItems()
        }
        updateSources(null)
    }

    private fun updateSources(folderId: Long?) {
        val targetFolderId = if (folderId == -1L) null else folderId

        currentFoldersSource?.let { libraryItems.removeSource(it) }
        currentNotesSource?.let { libraryItems.removeSource(it) }
        currentResourcesSource?.let { libraryItems.removeSource(it) }

        val foldersSource = repository.getFoldersInFolder(targetFolderId)
        val resourcesSource = repository.getResourcesInFolder(targetFolderId)
        val notesSource = if (targetFolderId != null) {
            noteRepository.getNotesInFolder(targetFolderId)
        } else {
            noteRepository.rootNotes
        }

        currentFoldersSource = foldersSource
        currentResourcesSource = resourcesSource
        currentNotesSource = notesSource

        libraryItems.addSource(foldersSource) { folders ->
            latestFolders = folders ?: emptyList()
            combineItems()
        }
        libraryItems.addSource(notesSource) { notes ->
            latestNotes = notes ?: emptyList()
            combineItems()
        }
        libraryItems.addSource(resourcesSource) { resources ->
            latestResources = resources ?: emptyList()
            combineItems()
        }
    }

    private fun combineItems() {
        val query = _searchQuery.value?.trim()?.lowercase()

        val folderItems = latestFolders
            .filter { query.isNullOrEmpty() || it.name.lowercase().contains(query) }
            .map { HomeItem.FolderItem(it) }

        val noteItems = latestNotes
            .filter {
                query.isNullOrEmpty() ||
                        (!it.title.isNullOrBlank() && it.title.lowercase().contains(query)) ||
                        (!it.content.isNullOrBlank() && it.content.lowercase().contains(query))
            }
            .map { HomeItem.NoteItem(it) }

        val resourceItems = latestResources
            .filter {
                query.isNullOrEmpty() ||
                        it.displayName.lowercase().contains(query) ||
                        it.originalName.lowercase().contains(query)
            }
            .map { HomeItem.ResourceItem(it) }

        val pinnedNotes = noteItems.filter { it.note.isPinned }
        val unpinnedNotes = noteItems.filter { !it.note.isPinned }

        libraryItems.value = pinnedNotes + folderItems + unpinnedNotes + resourceItems
    }

    fun setCurrentFolderId(folderId: Long?) {
        _currentFolderId.value = if (folderId == -1L) null else folderId
    }

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
    }

    fun toggleLayoutMode() {
        val layoutMode = if (_layoutMode.value == LayoutMode.LINEAR) LayoutMode.GRID
        else LayoutMode.LINEAR

        preferenceRepository.saveLayoutMode(layoutMode)
        _layoutMode.value = layoutMode
    }

    fun createFolder(name: String, parentFolderId: Long? = _currentFolderId.value, onComplete: ((Long) -> Unit)? = null) {
        if (name.isBlank()) return
        val targetParent = if (parentFolderId == -1L) null else parentFolderId
        viewModelScope.launch {
            val id = repository.insertFolder(name, targetParent)
            onComplete?.invoke(id)
        }
    }

    fun renameFolder(folder: Folder, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameFolder(folder, newName)
        }
    }

    fun moveFolder(folder: Folder, targetFolderId: Long?) {
        val target = if (targetFolderId == -1L) null else targetFolderId
        viewModelScope.launch {
            repository.moveFolder(folder, target)
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolderRecursively(folder)
        }
    }

    fun restoreFolder(folder: Folder) {
        viewModelScope.launch {
            repository.restoreFolder(folder)
        }
    }

    fun addFiles(uris: List<Uri>, folderId: Long? = _currentFolderId.value, context: Context, onComplete: (() -> Unit)? = null) {
        if (uris.isEmpty()) return
        val targetFolder = if (folderId == -1L) null else folderId
        viewModelScope.launch(Dispatchers.IO) {
            val resources = mutableListOf<LibraryResource>()
            val resourcesDir = java.io.File(context.filesDir, "library_resources")
            if (!resourcesDir.exists()) resourcesDir.mkdirs()

            for (uri in uris) {
                val (name, mimeType) = getFileInfo(context, uri)
                
                val uniqueFileName = java.util.UUID.randomUUID().toString() + "_" + name
                val localFile = java.io.File(resourcesDir, uniqueFileName)
                
                var success = false
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    success = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (success) {
                    resources.add(
                        LibraryResource(
                            folderId = targetFolder,
                            displayName = name,
                            originalName = name,
                            resourceType = "FILE",
                            mimeType = mimeType,
                            uriString = Uri.fromFile(localFile).toString()
                        )
                    )
                }
            }
            repository.insertResources(resources)
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun addLink(url: String, displayName: String?, folderId: Long? = _currentFolderId.value, onComplete: (() -> Unit)? = null) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        val targetFolder = if (folderId == -1L) null else folderId
        val finalUrl = if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)) {
            "https://$cleanUrl"
        } else {
            cleanUrl
        }
        val name = if (!displayName.isNullOrBlank()) displayName.trim() else cleanUrl
        viewModelScope.launch {
            repository.insertResource(
                folderId = targetFolder,
                displayName = name,
                originalName = finalUrl,
                resourceType = "LINK",
                mimeType = "text/uri-list",
                uriString = finalUrl
            )
            onComplete?.invoke()
        }
    }

    fun renameResource(resource: LibraryResource, newDisplayName: String) {
        if (newDisplayName.isBlank()) return
        viewModelScope.launch {
            repository.renameResource(resource, newDisplayName)
        }
    }

    fun moveResource(resource: LibraryResource, targetFolderId: Long?) {
        val target = if (targetFolderId == -1L) null else targetFolderId
        viewModelScope.launch {
            repository.moveResource(resource, target)
        }
    }

    fun deleteResource(resource: LibraryResource) {
        viewModelScope.launch {
            repository.deleteResource(resource)
        }
    }

    fun restoreResource(resource: LibraryResource) {
        viewModelScope.launch {
            repository.restoreResource(resource)
        }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch {
            noteRepository.update(note.copy(isPinned = !note.isPinned))
        }
    }

    fun moveNote(note: Note, targetFolderId: Long?) {
        val target = if (targetFolderId == -1L) null else targetFolderId
        viewModelScope.launch {
            noteRepository.update(note.copy(folderId = target))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.delete(note)
        }
    }

    fun markDeleted(note: Note) {
        viewModelScope.launch {
            noteRepository.update(note.copy(isDeleted = true))
        }
    }

    fun unmarkDeleted(note: Note) {
        viewModelScope.launch {
            noteRepository.update(note.copy(isDeleted = false))
        }
    }

    suspend fun getAllFolders(): List<Folder> = withContext(Dispatchers.IO) {
        repository.getAllFoldersSync()
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
}
