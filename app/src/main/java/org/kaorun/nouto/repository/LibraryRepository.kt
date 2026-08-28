package org.kaorun.nouto.repository

import androidx.lifecycle.LiveData
import org.kaorun.nouto.data.Folder
import org.kaorun.nouto.data.LibraryDao
import org.kaorun.nouto.data.LibraryResource

class LibraryRepository(private val libraryDao: LibraryDao) {

    fun getFoldersInFolder(parentId: Long?): LiveData<List<Folder>> =
        libraryDao.getFoldersInFolder(parentId)

    fun getAllFolders(): LiveData<List<Folder>> =
        libraryDao.getAllFolders()

    suspend fun getAllFoldersSync(): List<Folder> =
        libraryDao.getAllFoldersSync()

    fun getFolderById(id: Long): LiveData<Folder?> =
        libraryDao.getFolderById(id)

    suspend fun getFolderByIdSync(id: Long): Folder? =
        libraryDao.getFolderByIdSync(id)

    fun getResourcesInFolder(folderId: Long?): LiveData<List<LibraryResource>> =
        libraryDao.getResourcesInFolder(folderId)

    fun getAllResources(): LiveData<List<LibraryResource>> =
        libraryDao.getAllResources()

    fun getResourceById(id: Long): LiveData<LibraryResource?> =
        libraryDao.getResourceById(id)

    suspend fun insertFolder(name: String, parentFolderId: Long?): Long {
        return libraryDao.insertFolder(
            Folder(
                name = name.trim(),
                parentFolderId = parentFolderId
            )
        )
    }

    suspend fun updateFolder(folder: Folder) {
        libraryDao.updateFolder(folder)
    }

    suspend fun renameFolder(folder: Folder, newName: String) {
        libraryDao.updateFolder(folder.copy(name = newName.trim()))
    }

    suspend fun moveFolder(folder: Folder, targetFolderId: Long?) {
        if (folder.id != targetFolderId) {
            libraryDao.updateFolder(folder.copy(parentFolderId = targetFolderId))
        }
    }

    suspend fun deleteFolderRecursively(folder: Folder) {
        // Just mark it as deleted (and we should probably mark its children as well, but typical trash logic might just hide the parent, or we mark all descendants)
        // Let's mark all descendants so they appear in trash
        val foldersToDelete = mutableListOf<Folder>()
        val resourcesToDelete = mutableListOf<LibraryResource>()

        suspend fun collectDescendants(currentFolder: Folder) {
            foldersToDelete.add(currentFolder.copy(isDeleted = true))
            val resources = libraryDao.getResourcesInFolderSync(currentFolder.id)
            resourcesToDelete.addAll(resources.map { it.copy(isDeleted = true) })
            val children = libraryDao.getChildFoldersSync(currentFolder.id)
            for (child in children) {
                collectDescendants(child)
            }
        }

        collectDescendants(folder)
        for (res in resourcesToDelete) libraryDao.updateResource(res)
        for (f in foldersToDelete) libraryDao.updateFolder(f)
    }

    suspend fun insertResource(
        folderId: Long?,
        displayName: String,
        originalName: String,
        resourceType: String,
        mimeType: String?,
        uriString: String
    ): Long {
        return libraryDao.insertResource(
            LibraryResource(
                folderId = folderId,
                displayName = displayName.trim().ifEmpty { originalName },
                originalName = originalName,
                resourceType = resourceType,
                mimeType = mimeType,
                uriString = uriString
            )
        )
    }

    suspend fun insertResources(resources: List<LibraryResource>) {
        libraryDao.insertResources(resources)
    }

    suspend fun updateResource(resource: LibraryResource) {
        libraryDao.updateResource(resource)
    }

    suspend fun renameResource(resource: LibraryResource, newDisplayName: String) {
        libraryDao.updateResource(resource.copy(displayName = newDisplayName.trim().ifEmpty { resource.originalName }))
    }

    suspend fun moveResource(resource: LibraryResource, targetFolderId: Long?) {
        libraryDao.updateResource(resource.copy(folderId = targetFolderId))
    }

    suspend fun deleteResource(resource: LibraryResource) {
        libraryDao.updateResource(resource.copy(isDeleted = true))
    }

    suspend fun restoreResource(resource: LibraryResource) {
        libraryDao.updateResource(resource.copy(isDeleted = false))
    }

    suspend fun restoreFolder(folder: Folder) {
        libraryDao.updateFolder(folder.copy(isDeleted = false))
        // Note: For a robust implementation we might want to restore children too, but for now restoring the folder restores its visibility if we don't query children's isDeleted flag recursively, or we just restore this folder.
    }

    suspend fun deleteResources(resources: List<LibraryResource>) {
        libraryDao.deleteResources(resources)
    }
}
