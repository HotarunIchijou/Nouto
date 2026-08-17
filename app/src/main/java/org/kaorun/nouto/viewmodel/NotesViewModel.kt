package org.kaorun.nouto.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import org.kaorun.nouto.data.Note
import org.kaorun.nouto.repository.NoteRepository
import kotlinx.coroutines.launch
import org.kaorun.nouto.data.NoteDatabase
import org.kaorun.nouto.repository.PreferenceRepository
import org.kaorun.nouto.ui.model.LayoutMode

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository: NoteRepository
    private val preferenceRepository = PreferenceRepository(application)

    private val _searchQuery = MutableLiveData<String?>(null)
    private val _layoutMode = MutableLiveData(preferenceRepository.getLayoutMode())
    private val _allNotes: LiveData<List<Note>>
    private val _pendingDelete = MutableLiveData<Note?>()
    val deletedNotes: LiveData<List<Note>>
    val displayedNotes: LiveData<List<Note>>
    val searchQuery: LiveData<String?> = _searchQuery
    val layoutMode: LiveData<LayoutMode> = _layoutMode
    val pendingDelete: LiveData<Note?> = _pendingDelete


    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        noteRepository = NoteRepository(noteDao)
        _allNotes = noteRepository.allNotes
        deletedNotes = noteRepository.deletedNotes
        displayedNotes = searchQuery.switchMap { query ->
            if (query.isNullOrBlank()) _allNotes
            else noteRepository.searchNotes(query)
        }
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
        isPinned: Boolean = false
    ) {
        if (title.isNullOrBlank() && content.isNullOrBlank()) return
        viewModelScope.launch {
            noteRepository.insert(
                Note(
                    title = title,
                    content = content,
                    time = time,
                    isPinned = isPinned
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
}