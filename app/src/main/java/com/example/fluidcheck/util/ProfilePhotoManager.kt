package com.example.fluidcheck.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Manages local caching of profile photos for offline-first display.
 * Photos are stored in the app's internal storage (filesDir) so they persist
 * across app restarts but are automatically cleaned up on app uninstall.
 */
object ProfilePhotoManager {

    private const val TAG = "ProfilePhotoManager"
    private const val PHOTO_DIR = "profile_photos"
    private const val PHOTO_FILENAME = "profile_photo.jpg"

    /**
     * Saves compressed image bytes to local storage.
     * Returns the File on success, null on failure.
     */
    fun savePhotoLocally(context: Context, imageBytes: ByteArray): File? {
        return try {
            val dir = File(context.filesDir, PHOTO_DIR)
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, PHOTO_FILENAME)
            FileOutputStream(file).use { fos ->
                fos.write(imageBytes)
                fos.flush()
            }
            Log.d(TAG, "Photo saved locally: ${file.absolutePath} (${imageBytes.size} bytes)")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save photo locally: ${e.message}", e)
            null
        }
    }

    /**
     * Returns the local photo file if it exists, null otherwise.
     */
    fun getLocalPhotoFile(context: Context): File? {
        val file = File(context.filesDir, "$PHOTO_DIR/$PHOTO_FILENAME")
        return if (file.exists()) file else null
    }

    /**
     * Deletes the locally cached profile photo.
     */
    fun deleteLocalPhoto(context: Context) {
        try {
            val file = File(context.filesDir, "$PHOTO_DIR/$PHOTO_FILENAME")
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Local photo deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete local photo: ${e.message}", e)
        }
    }
}
