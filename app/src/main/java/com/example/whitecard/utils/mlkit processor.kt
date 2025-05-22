package com.example.whitecard.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MLKitCardProcessor(private val context: Context) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val TAG = "MLKitCardProcessor"

    /**
     * Process a card image from Uri
     */
    suspend fun processCard(uri: Uri): String {
        Log.d(TAG, "Processing card from URI: $uri")

        return withContext(Dispatchers.IO) {
            try {
                // Convert URI to Bitmap
                val bitmap = loadBitmapFromUri(uri)
                if (bitmap != null) {
                    // Create input image from Bitmap
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    // Extract text
                    val text = extractTextFromImage(inputImage)
                    Log.d(TAG, "Extracted text: $text")
                    text
                } else {
                    Log.e(TAG, "Failed to load bitmap from URI")
                    "Error: Could not load image"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing card", e)
                "Error: ${e.message}"
            }
        }
    }

    /**
     * Process a card image from Bitmap
     */
    suspend fun processCardBitmap(bitmap: Bitmap): String {
        return try {
            // Create input image from Bitmap
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            // Process with ML Kit
            extractTextFromImage(inputImage)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing card bitmap", e)
            "Error: ${e.message}"
        }
    }

    /**
     * Load a bitmap from URI
     */
    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading bitmap from URI", e)
            null
        }
    }

    /**
     * Extract text from an image using ML Kit
     */
    private suspend fun extractTextFromImage(image: InputImage): String = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                continuation.resume(text)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
                continuation.resumeWithException(e)
            }
    }
}
