package org.kaorun.nouto.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LibraryDao {

    // --- Folders ---
    @Query("SELECT * FROM folders WHERE (parentFolderId = :parentId OR (parentFolderId IS NULL AND :parentId IS NULL)) AND isDeleted = 0 ORDER BY time DESC")
    fun getFoldersInFolder(parentId: Long?): LiveData<List<Folder>>

    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY time DESC")
    fun getAllFolders(): LiveData<List<Folder>>

    @Query("SELECT * FROM folders WHERE isDeleted = 0")
    suspend fun getAllFoldersSync(): List<Folder>

    @Query("SELECT * FROM folders WHERE isDeleted = 1 ORDER BY time DESC")
    fun getDeletedFolders(): LiveData<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id AND isDeleted = 0")
    fun getFolderById(id: Long): LiveData<Folder?>

    @Query("SELECT * FROM folders WHERE id = :id AND isDeleted = 0")
    suspend fun getFolderByIdSync(id: Long): Folder?

    @Query("SELECT * FROM folders WHERE parentFolderId = :folderId AND isDeleted = 0")
    suspend fun getChildFoldersSync(folderId: Long): List<Folder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Delete
    suspend fun deleteFolders(folders: List<Folder>)

    // --- Resources ---
    @Query("SELECT * FROM library_resources WHERE (folderId = :folderId OR (folderId IS NULL AND :folderId IS NULL)) AND isDeleted = 0 ORDER BY time DESC")
    fun getResourcesInFolder(folderId: Long?): LiveData<List<LibraryResource>>

    @Query("SELECT * FROM library_resources WHERE isDeleted = 0 ORDER BY time DESC")
    fun getAllResources(): LiveData<List<LibraryResource>>

    @Query("SELECT * FROM library_resources WHERE isDeleted = 1 ORDER BY time DESC")
    fun getDeletedResources(): LiveData<List<LibraryResource>>

    @Query("SELECT * FROM library_resources WHERE (folderId = :folderId OR (folderId IS NULL AND :folderId IS NULL)) AND isDeleted = 0")
    suspend fun getResourcesInFolderSync(folderId: Long?): List<LibraryResource>

    @Query("SELECT * FROM library_resources WHERE id = :id AND isDeleted = 0")
    fun getResourceById(id: Long): LiveData<LibraryResource?>

    @Query("SELECT * FROM library_resources WHERE id = :id AND isDeleted = 0")
    suspend fun getResourceByIdSync(id: Long): LibraryResource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: LibraryResource): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<LibraryResource>)

    @Update
    suspend fun updateResource(resource: LibraryResource)

    @Delete
    suspend fun deleteResource(resource: LibraryResource)

    @Delete
    suspend fun deleteResources(resources: List<LibraryResource>)

    @Query("DELETE FROM library_resources WHERE folderId = :folderId")
    suspend fun deleteResourcesInFolder(folderId: Long)
}
