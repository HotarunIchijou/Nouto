package org.kaorun.nouto.data

sealed class LibraryItem {
    data class FolderItem(val folder: Folder) : LibraryItem()
    data class ResourceItem(val resource: LibraryResource) : LibraryItem()

    val id: Long
        get() = when (this) {
            is FolderItem -> folder.id
            is ResourceItem -> resource.id
        }

    val name: String
        get() = when (this) {
            is FolderItem -> folder.name
            is ResourceItem -> resource.displayName
        }
}
