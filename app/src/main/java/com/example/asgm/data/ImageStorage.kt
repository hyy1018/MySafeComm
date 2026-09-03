// #member4
// Copies a picked image into the app's own files directory and returns a file:// URI string.
// The photo picker (GetContent) only grants one-time read access to its content URI, so a post
// that stored that raw URI showed no image once the app restarted -- keeping a private copy
// fixes that.
package com.example.asgm.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Returns the file:// URI string for the copy, or null if the source couldn't be read.
suspend fun copyImageToAppStorage(context: Context, source: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "post_images").apply { mkdirs() }
            val dest = File(dir, "post_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            Uri.fromFile(dest).toString()
        } catch (e: Exception) {
            null
        }
    }
