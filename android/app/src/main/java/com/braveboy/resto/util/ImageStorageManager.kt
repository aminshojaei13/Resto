package com.braveboy.resto.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Offline-first local image storage manager.
 * Saves user profile photos and product images directly into app internal storage
 * (`context.filesDir`), returning local file paths (`/data/user/0/...`) for instant
 * offline rendering and local persistence.
 */
object ImageStorageManager {

    fun saveImageLocally(
        context: Context,
        uri: Uri,
        folderName: String,
        fileName: String
    ): String? {
        return try {
            val dir = File(context.filesDir, folderName)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "${fileName.replace(" ", "_")}.jpg")
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
