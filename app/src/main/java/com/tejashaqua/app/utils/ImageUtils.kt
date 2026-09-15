package com.tejashaqua.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.io.IOException
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import com.tejashaqua.app.ui.theme.AquaBlue

object ImageUtils {
    fun createImageUri(context: Context): Uri? {
        val imageFolder = File(context.cacheDir, "images")
        if (!imageFolder.exists()) {
            imageFolder.mkdirs()
        }
        val file = File(imageFolder, "camera_photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun getCorrectlyOrientedBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var rotation = 0
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                
                rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }

            inputStream = context.contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (rotation != 0 && bitmap != null) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                bitmap = rotatedBitmap
            }
            
            if (bitmap != null) {
                val watermarked = addWatermark(context, bitmap)
                if (watermarked != bitmap) {
                    bitmap.recycle()
                    bitmap = watermarked
                }
            }
            
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun addWatermark(context: Context, src: Bitmap): Bitmap {
        // First, resize to a reasonable maximum dimension to save storage and bandwidth
        val maxDimension = 1280
        var processedSrc = src
        
        if (src.width > maxDimension || src.height > maxDimension) {
            val ratio = src.width.toFloat() / src.height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (ratio > 1) {
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }
            processedSrc = Bitmap.createScaledBitmap(src, newWidth, newHeight, true)
        }

        val width = processedSrc.width
        val height = processedSrc.height
        val result = Bitmap.createBitmap(width, height, processedSrc.config ?: Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(result)
        canvas.drawBitmap(processedSrc, 0f, 0f, null)
        
        // Recycle the intermediate resized bitmap if we created one
        if (processedSrc != src) {
            processedSrc.recycle()
        }

        val text = "Tejash Aqua"

        // 1. Logo at Bottom Right Corner
        try {
            val logo = BitmapFactory.decodeResource(context.resources, com.tejashaqua.app.R.drawable.tejas_aqua_watermark_logo)
            
            if (logo != null) {
                // Logo size (approx 12% of image width)
                val logoWidth = (width * 0.15f).toInt().coerceAtLeast(80)
                val logoHeight = (logo.height * (logoWidth.toFloat() / logo.width)).toInt()
                val scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, true)
                
                val paintLogo = Paint().apply {
                    alpha = 120 // Increased transparency (Lower value = more transparent)
                }

                val margin = (width * 0.02f).coerceAtLeast(20f)
                
                // Draw only at Bottom Right
                canvas.drawBitmap(
                    scaledLogo, 
                    width - logoWidth - margin, 
                    height - logoHeight - margin, 
                    paintLogo
                )

                scaledLogo.recycle()
                logo.recycle()
            }
        } catch (_: Exception) {
            // Fallback for bottom text if logo failsb
            val paintBr = Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = 140
                textSize = (width * 0.04f).coerceAtLeast(35f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                setShadowLayer(5f, 2f, 2f, android.graphics.Color.BLACK)
            }
            val boundsBr = Rect()
            paintBr.getTextBounds(text, 0, text.length, boundsBr)
            canvas.drawText(text, width - boundsBr.width() - 40f, height - 40f, paintBr)
        }
        
        return result
    }

    /**
     * Attempts to convert a Firebase Storage download URL for an original image
     * into a URL for a thumbnail generated by the "Resize Images" extension.
     * Assumes standard suffix configuration: _400x400
     */
    fun getThumbnailUrl(originalUrl: String?): String? {
        if (originalUrl.isNullOrBlank()) return originalUrl
        if (!originalUrl.contains("firebasestorage.googleapis.com")) return originalUrl
        
        // If it's already a thumbnail URL, return it
        if (originalUrl.contains("_400x400")) return originalUrl
        
        return try {
            val urlParts = originalUrl.split("?")
            val basePath = urlParts[0]
            val queryParams = if (urlParts.size > 1) "?" + urlParts[1] else ""
            
            val lastDotIndex = basePath.lastIndexOf(".")
            if (lastDotIndex != -1) {
                basePath.substring(0, lastDotIndex) + "_400x400" + basePath.substring(lastDotIndex) + queryParams
            } else {
                originalUrl
            }
        } catch (_: Exception) {
            originalUrl
        }
    }
}
