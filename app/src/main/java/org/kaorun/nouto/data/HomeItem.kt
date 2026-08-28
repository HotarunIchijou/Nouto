package org.kaorun.nouto.data

sealed class HomeItem {
    data class FolderItem(val folder: Folder) : HomeItem()
    data class NoteItem(val note: Note) : HomeItem()
    data class ResourceItem(val resource: LibraryResource) : HomeItem()

    val id: Long
        get() = when (this) {
            is FolderItem -> folder.id
            is NoteItem -> note.id.toLong()
            is ResourceItem -> resource.id
        }

    val name: String
        get() = when (this) {
            is FolderItem -> folder.name
            is NoteItem -> note.title.takeIf { !it.isNullOrBlank() } ?: note.content.orEmpty()
            is ResourceItem -> resource.displayName
        }

    val time: Long
        get() = when (this) {
            is FolderItem -> folder.time
            is NoteItem -> note.time
            is ResourceItem -> resource.time
        }
}
