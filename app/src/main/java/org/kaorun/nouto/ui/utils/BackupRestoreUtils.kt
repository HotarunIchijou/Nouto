package org.kaorun.nouto.ui.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kaorun.nouto.data.NoteDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupRestoreUtils {
    
    suspend fun createFullBackup(context: Context, backupUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbPath = context.getDatabasePath("note_database")
            val walPath = context.getDatabasePath("note_database-wal")
            val shmPath = context.getDatabasePath("note_database-shm")
            
            // NoteDatabase instance must be checkpointed and closed to ensure all data is in the main db
            val db = NoteDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            
            val resourcesDir = File(context.filesDir, "library_resources")
            
            context.contentResolver.openOutputStream(backupUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    
                    // Backup DB
                    if (dbPath.exists()) addFileToZip(dbPath, "note_database", zipOut)
                    if (walPath.exists()) addFileToZip(walPath, "note_database-wal", zipOut)
                    if (shmPath.exists()) addFileToZip(shmPath, "note_database-shm", zipOut)
                    
                    // Backup Resources
                    if (resourcesDir.exists() && resourcesDir.isDirectory) {
                        resourcesDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                addFileToZip(file, "library_resources/${file.name}", zipOut)
                            }
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun restoreFullBackup(context: Context, backupUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbPath = context.getDatabasePath("note_database")
            val walPath = context.getDatabasePath("note_database-wal")
            val shmPath = context.getDatabasePath("note_database-shm")
            val resourcesDir = File(context.filesDir, "library_resources")
            
            // Ensure DB is fully closed before overriding
            NoteDatabase.getDatabase(context).close()
            
            // Delete old db files to prevent conflicts
            if (dbPath.exists()) dbPath.delete()
            if (walPath.exists()) walPath.delete()
            if (shmPath.exists()) shmPath.delete()
            
            context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "note_database" -> extractFile(zipIn, dbPath)
                            entry.name == "note_database-wal" -> extractFile(zipIn, walPath)
                            entry.name == "note_database-shm" -> extractFile(zipIn, shmPath)
                            entry.name.startsWith("library_resources/") -> {
                                if (!resourcesDir.exists()) resourcesDir.mkdirs()
                                val fileName = entry.name.removePrefix("library_resources/")
                                val file = File(resourcesDir, fileName)
                                extractFile(zipIn, file)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun addFileToZip(file: File, zipPath: String, zipOut: ZipOutputStream) {
        FileInputStream(file).use { fis ->
            val zipEntry = ZipEntry(zipPath)
            zipOut.putNextEntry(zipEntry)
            fis.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
    
    private fun extractFile(zipIn: ZipInputStream, targetFile: File) {
        FileOutputStream(targetFile).use { fos ->
            zipIn.copyTo(fos)
        }
    }
}
