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
    ): Result<Unit> {
        return runCatching {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw NullPointerException()

            outputStream.use { stream ->
                stream.bufferedWriter().use { writer ->
                    val jsonObject = JSONObject().apply {
                        put("title", title ?: "")
                        put("content", content ?: "")
                    }
                    writer.write(jsonObject.toString())
                }
            }
        }
    }

    fun readFromFile(
        context: Context,
        uri: Uri
    ): Result<Pair<String, String>> {
        return runCatching {
           val inputStream = context.contentResolver.openInputStream(uri)
               ?: throw NullPointerException()

           inputStream.use { stream ->
               stream.bufferedReader().use { reader ->
                   val jsonString = reader.readText()
                   val jsonObject = JSONObject(jsonString)

                   val title = jsonObject.optString("title", "")
                   val content = jsonObject.optString("content", "")

                   Pair(title, content)
               }
           }
        }
    }
}