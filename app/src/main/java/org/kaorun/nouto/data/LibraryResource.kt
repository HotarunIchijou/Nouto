package org.kaorun.nouto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_resources")
data class LibraryResource(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long? = null,
    val displayName: String,
    val originalName: String,
    val resourceType: String, // "FILE" or "LINK"
    val mimeType: String? = null,
    val uriString: String,
    val time: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
