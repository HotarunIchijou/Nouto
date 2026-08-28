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

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository: NoteRepository
    private val libraryRepository: LibraryRepository
    private val preferenceRepository = PreferenceRepository(application)

    private val _searchQuery = MutableLiveData<String?>(null)
    private val _layoutMode = MutableLiveData(preferenceRepository.getLayoutMode())
    private val _pendingDelete = MutableLiveData<Note?>()

    val deletedNotes: LiveData<List<Note>>
    val allNotes: LiveData<List<Note>>
    val displayedNotes: LiveData<List<Note>>
    val searchQuery: LiveData<String?> = _searchQuery
    val layoutMode: LiveData<LayoutMode> = _layoutMode
    val pendingDelete: LiveData<Note?> = _pendingDelete

    val displayedHomeItems = MediatorLiveData<List<HomeItem>>()

    private var latestNotes: List<Note> = emptyList()
    private var latestFolders: List<Folder> = emptyList()
    private var latestResources: List<LibraryResource> = emptyList()

    private val rootNotesSource: LiveData<List<Note>>
    private val rootFoldersSource: LiveData<List<Folder>>
    private val rootResourcesSource: LiveData<List<LibraryResource>>
    private val allNotesSource: LiveData<List<Note>>
    private val allFoldersSource: LiveData<List<Folder>>
    private val allResourcesSource: LiveData<List<LibraryResource>>

    private var isInitialLoadDone = false

    init {
        val db = NoteDatabase.getDatabase(application)
        noteRepository = NoteRepository(db.noteDao())
        libraryRepository = LibraryRepository(db.libraryDao())

        deletedNotes = noteRepository.deletedNotes
        allNotes = noteRepository.allNotes
        displayedNotes = noteRepository.allNotes

        rootNotesSource = noteRepository.rootNotes
        rootFoldersSource = libraryRepository.getFoldersInFolder(null)
        rootResourcesSource = libraryRepository.getResourcesInFolder(null)
        allNotesSource = noteRepository.allNotes
        allFoldersSource = libraryRepository.getAllFolders()
        allResourcesSource = libraryRepository.getAllResources()

        displayedHomeItems.addSource(rootNotesSource) { notes ->
            if (_searchQuery.value.isNullOrBlank()) {
                latestNotes = notes ?: emptyList()
                combineHomeItems()
            }
        }
        displayedHomeItems.addSource(rootFoldersSource) { folders ->
            if (_searchQuery.value.isNullOrBlank()) {
                latestFolders = folders ?: emptyList()
                combineHomeItems()
            }
        }
        displayedHomeItems.addSource(rootResourcesSource) { resources ->
            if (_searchQuery.value.isNullOrBlank()) {
                latestResources = resources ?: emptyList()
                combineHomeItems()
            }
        }
        
        displayedHomeItems.addSource(allNotesSource) { notes ->
            if (!_searchQuery.value.isNullOrBlank()) {
                latestNotes = notes ?: emptyList()
                combineHomeItems()
            }
        }
        displayedHomeItems.addSource(allFoldersSource) { folders ->
            if (!_searchQuery.value.isNullOrBlank()) {
                latestFolders = folders ?: emptyList()
                combineHomeItems()
            }
        }
        displayedHomeItems.addSource(allResourcesSource) { resources ->
            if (!_searchQuery.value.isNullOrBlank()) {
                latestResources = resources ?: emptyList()
                combineHomeItems()
            }
        }

        displayedHomeItems.addSource(_searchQuery) { query ->
            // Update lists based on query state before combining
            if (query.isNullOrBlank()) {
                latestNotes = rootNotesSource.value ?: emptyList()
                latestFolders = rootFoldersSource.value ?: emptyList()
                latestResources = rootResourcesSource.value ?: emptyList()
            } else {
                latestNotes = allNotesSource.value ?: emptyList()
                latestFolders = allFoldersSource.value ?: emptyList()
                latestResources = allResourcesSource.value ?: emptyList()
            }
            combineHomeItems()
        }
    }

    private fun combineHomeItems() {
        val query = _searchQuery.value?.trim()?.lowercase()

        if (!isInitialLoadDone) {
            if (query.isNullOrBlank()) {
                if (rootNotesSource.value == null || rootFoldersSource.value == null || rootResourcesSource.value == null) {
                    return
                }
            } else {
                if (allNotesSource.value == null || allFoldersSource.value == null || allResourcesSource.value == null) {
                    return
                }
            }
            isInitialLoadDone = true
        }

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

        // Pinned notes first, then folders, then other notes and resources
        val pinnedNotes = noteItems.filter { it.note.isPinned }
        val unpinnedNotes = noteItems.filter { !it.note.isPinned }

        displayedHomeItems.value = pinnedNotes + folderItems + unpinnedNotes + resourceItems
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

    fun getNote(id: Int): LiveData<Note> = noteRepository.getNoteById(id)

    fun addNote(
        title: String?,
        content: String?,
        time: Long = System.currentTimeMillis(),
        isPinned: Boolean = false,
        folderId: Long? = null
    ) {
        if (title.isNullOrBlank() && content.isNullOrBlank()) return
        viewModelScope.launch {
            noteRepository.insert(
                Note(
                    title = title,
                    content = content,
                    time = time,
                    isPinned = isPinned,
                    folderId = folderId
                )
            )
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            if (note.title.isNullOrBlank() && note.content.isNullOrBlank()) {
                noteRepository.delete(note)
            } else {
                noteRepository.update(note)
            }
        }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch {
            noteRepository.update(note.copy(isPinned = !note.isPinned))
        }
    }

    fun moveNote(note: Note, targetFolderId: Long?) {
        viewModelScope.launch {
            noteRepository.update(note.copy(folderId = targetFolderId))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { noteRepository.delete(note) }
    }

    fun deleteNotes(notes: List<Note>) {
        viewModelScope.launch { noteRepository.delete(notes) }
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

    fun setPendingDelete(note: Note) {
        markDeleted(note)
        _pendingDelete.value = note
    }

    fun clearPendingDelete() {
        _pendingDelete.value = null
    }

    // --- Folder operations ---
    fun createFolder(name: String, parentFolderId: Long? = null, onComplete: ((Long) -> Unit)? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = libraryRepository.insertFolder(name, parentFolderId)
            onComplete?.invoke(id)
        }
    }

    fun renameFolder(folder: Folder, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            libraryRepository.renameFolder(folder, newName)
        }
    }

    fun moveFolder(folder: Folder, targetFolderId: Long?) {
        viewModelScope.launch {
            libraryRepository.moveFolder(folder, targetFolderId)
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            libraryRepository.deleteFolderRecursively(folder)
        }
    }

    fun restoreFolder(folder: Folder) {
        viewModelScope.launch {
            libraryRepository.restoreFolder(folder)
        }
    }

    // --- Resource operations ---
    fun addFiles(uris: List<Uri>, folderId: Long? = null, context: Context, onComplete: (() -> Unit)? = null) {
        if (uris.isEmpty()) return
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
                            folderId = folderId,
                            displayName = name,
                            originalName = name,
                            resourceType = "FILE",
                            mimeType = mimeType,
                            uriString = Uri.fromFile(localFile).toString()
                        )
                    )
                }
            }
            libraryRepository.insertResources(resources)
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun addLink(url: String, displayName: String?, folderId: Long? = null, onComplete: (() -> Unit)? = null) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        val finalUrl = if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)) {
            "https://$cleanUrl"
        } else {
            cleanUrl
        }
        val name = if (!displayName.isNullOrBlank()) displayName.trim() else cleanUrl
        viewModelScope.launch {
            libraryRepository.insertResource(
                folderId = folderId,
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
            libraryRepository.renameResource(resource, newDisplayName)
        }
    }

    fun moveResource(resource: LibraryResource, targetFolderId: Long?) {
        viewModelScope.launch {
            libraryRepository.moveResource(resource, targetFolderId)
        }
    }

    fun deleteResource(resource: LibraryResource) {
        viewModelScope.launch {
            libraryRepository.deleteResource(resource)
        }
    }

    fun restoreResource(resource: LibraryResource) {
        viewModelScope.launch {
            libraryRepository.restoreResource(resource)
        }
    }

    suspend fun getAllFolders(): List<Folder> = withContext(Dispatchers.IO) {
        libraryRepository.getAllFoldersSync()
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