package org.kaorun.nouto.ui.utils

import android.content.Context
import android.net.Uri
import org.json.JSONObject

object FileUtils {
    fun writeNoteToFile(
        context: Context,
        uri: Uri,
        title: String?,
        content: String?
    ) {
        val jsonObject = JSONObject().apply {
            put("title", title ?: "")
            put("content", content ?: "")
        }
        val data = jsonObject.toString(4)

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { writer ->
                    writer.write(data)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}