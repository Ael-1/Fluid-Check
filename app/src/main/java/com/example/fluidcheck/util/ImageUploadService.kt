package com.example.fluidcheck.util

import android.util.Log
import com.example.fluidcheck.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Handles uploading images to ImgBB's free image hosting API.
 * Returns an HTTPS URL that can be stored in Firestore and loaded by Coil.
 */
object ImageUploadService {

    private const val TAG = "ImageUploadService"
    private const val IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload"

    /**
     * Uploads a Base64-encoded image to ImgBB.
     *
     * @param base64Image The Base64-encoded image string (without data URI prefix)
     * @return Result containing the HTTPS URL of the uploaded image, or a failure
     */
    suspend fun uploadToImgBB(base64Image: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.IMGBB_API_KEY
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("ImgBB API key is not configured"))
            }

            val url = URL(IMGBB_UPLOAD_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connectTimeout = 30_000
                readTimeout = 30_000
            }

            // Build form data
            val formData = buildString {
                append("key=")
                append(URLEncoder.encode(apiKey, "UTF-8"))
                append("&image=")
                append(URLEncoder.encode(base64Image, "UTF-8"))
            }

            // Write request body
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(formData)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "ImgBB upload failed with code $responseCode: $errorBody")
                return@withContext Result.failure(Exception("Upload failed (HTTP $responseCode)"))
            }

            // Parse JSON response
            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            if (!json.getBoolean("success")) {
                return@withContext Result.failure(Exception("ImgBB returned unsuccessful response"))
            }

            val imageUrl = json.getJSONObject("data").getString("display_url")
            Log.d(TAG, "Upload successful: $imageUrl")

            Result.success(imageUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}", e)
            Result.failure(Exception("Failed to upload image. Please try again."))
        }
    }
}
