package com.example.fluidcheck.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

object ImageUtils {
    /**
     * Processes an image from a Uri: crops to square, resizes to target size,
     * and returns the compressed JPEG as a ByteArray.
     */
    fun uriToCompressedByteArray(context: Context, uri: Uri, targetSize: Int = 256): ByteArray? {
        return try {
            val bitmap = decodeSampledBitmapFromUri(context, uri, targetSize) ?: return null
            
            // 1. Rotate based on Metadata (Phone orientation)
            val rotatedBitmap = rotateImageIfRequired(context, bitmap, uri)
            
            // 2. Crop to Square
            val dimension = Math.min(rotatedBitmap.width, rotatedBitmap.height)
            val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, (rotatedBitmap.width - dimension) / 2, (rotatedBitmap.height - dimension) / 2, dimension, dimension)
            
            // Clean up rotatedBitmap if it's different instance
            if (rotatedBitmap != bitmap) rotatedBitmap.recycle()
            
            // 3. Scale to Target Size
            val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true)
            
            // Clean up croppedBitmap if different
            if (croppedBitmap != scaledBitmap) croppedBitmap.recycle()
            bitmap.recycle() // Done with original sampled bitmap

            // 4. Compress to JPEG
            val byteArrayOutputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            scaledBitmap.recycle() // All done with bitmaps
            
            android.util.Log.d("ImageUtils", "Process complete. Byte array size: ${byteArray.size}")
            byteArray
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, targetSize: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        context.contentResolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it, null, options) 
        }

        options.inSampleSize = calculateInSampleSize(options, targetSize, targetSize)
        options.inJustDecodeBounds = false
        
        return context.contentResolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it, null, options) 
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
        val ei = context.contentResolver.openInputStream(selectedImage)?.use { input ->
            ExifInterface(input)
        } ?: return img
        
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
}
