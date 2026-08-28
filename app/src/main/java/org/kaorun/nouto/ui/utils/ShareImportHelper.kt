package org.kaorun.nouto.ui.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kaorun.nouto.data.LibraryResource
import org.kaorun.nouto.repository.LibraryRepository

object ShareImportHelper {

    suspend fun importToFolder(
        context: Context,
        uris: List<Uri>?,
        text: String?,
        repository: LibraryRepository,
        targetFolderId: Long?
    ) = withContext(Dispatchers.IO) {
        if (!uris.isNullOrEmpty()) {
            val resources = mutableListOf<LibraryResource>()
            val resourcesDir = java.io.File(context.filesDir, "library_resources")
            if (!resourcesDir.exists()) resourcesDir.mkdirs()

            for (uri in uris) {
                val (name, mime) = getFileInfo(context, uri)
                
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
                            folderId = targetFolderId,
                            displayName = name,
                            originalName = name,
                            resourceType = "FILE",
                            mimeType = mime,
                            uriString = Uri.fromFile(localFile).toString()
                        )
                    )
                }
            }
            repository.insertResources(resources)
        } else if (!text.isNullOrBlank()) {
            val t = text.trim()
            val isUrl = t.startsWith("http://", ignoreCase = true) ||
                    t.startsWith("https://", ignoreCase = true) ||
                    t.contains(".")
            val finalUrl = if (isUrl && !t.startsWith("http://", ignoreCase = true) &&
                !t.startsWith("https://", ignoreCase = true)) {
                "https://$t"
            } else {
                t
            }
            repository.insertResource(
                folderId = targetFolderId,
                displayName = t,
                originalName = finalUrl,
                resourceType = if (isUrl) "LINK" else "FILE",
                mimeType = if (isUrl) "text/uri-list" else "text/plain",
                uriString = finalUrl
            )
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
}
