package org.kaorun.nouto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val parentFolderId: Long? = null,
    val time: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
