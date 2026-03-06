package com.bordrotakip.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.bordrotakip.util.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MlKitTextRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun recognizeText(uri: Uri): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            return runCatching {
                recognizeFromFilePath(recognizer, uri)
            }.getOrElse { primaryError ->
                Log.w(TAG, "OCR fromFilePath başarısız, bitmap fallback deneniyor", primaryError)
                recognizeFromUriBitmap(recognizer, uri, primaryError)
            }
        } finally {
            recognizer.close()
        }
    }

    suspend fun recognizeTaxRegionText(uri: Uri): String? {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val bitmap = decodeBitmap(uri) ?: return null
            val crops = listOf(
                cropByRatio(bitmap, xRatio = 0.42f, yRatio = 0.22f, wRatio = 0.42f, hRatio = 0.50f),
                cropByRatio(bitmap, xRatio = 0.48f, yRatio = 0.22f, wRatio = 0.36f, hRatio = 0.50f)
            )
            val cropTexts = crops.mapNotNull { crop ->
                runCatching {
                    val prepared = scaleUpForOcrIfNeeded(crop)
                    recognizeFromBitmap(recognizer, prepared)
                }.onFailure {
                    Log.w(TAG, "OCR tax bölge taraması başarısız", it)
                }.getOrNull()?.takeIf { it.isNotBlank() }
            }
            return cropTexts.joinToString("\n").ifBlank { null }
        } finally {
            recognizer.close()
        }
    }

    private suspend fun recognizeFromFilePath(recognizer: TextRecognizer, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image).await()
        return result.text
    }

    private suspend fun recognizeFromUriBitmap(
        recognizer: TextRecognizer,
        uri: Uri,
        primaryError: Throwable
    ): String {
        val bitmap = decodeBitmap(uri)
            ?: throw IllegalStateException("Görsel çözümlenemedi.", primaryError)
        return recognizeFromBitmap(recognizer, bitmap)
    }

    private suspend fun recognizeFromBitmap(recognizer: TextRecognizer, bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return result.text
    }

    private fun cropByRatio(
        bitmap: Bitmap,
        xRatio: Float,
        yRatio: Float,
        wRatio: Float,
        hRatio: Float
    ): Bitmap {
        val x = (bitmap.width * xRatio).toInt().coerceIn(0, bitmap.width - 1)
        val y = (bitmap.height * yRatio).toInt().coerceIn(0, bitmap.height - 1)
        val width = (bitmap.width * wRatio).toInt().coerceIn(1, bitmap.width - x)
        val height = (bitmap.height * hRatio).toInt().coerceIn(1, bitmap.height - y)
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    private fun scaleUpForOcrIfNeeded(bitmap: Bitmap): Bitmap {
        val minWidthForText = 1500
        if (bitmap.width >= minWidthForText) return bitmap
        val scale = minWidthForText.toFloat() / bitmap.width
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, minWidthForText, targetHeight, true)
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }.getOrNull()
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    }

    companion object {
        private const val TAG = "MlKitTextRecognizer"
    }
}
