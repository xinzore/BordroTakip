package com.bordrotakip.ocr

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject

class PayrollImageStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun copyToInternalStorage(sourceUri: Uri): File {
        return withContext(Dispatchers.IO) {
            val imagesDir = File(context.filesDir, "payroll_images").apply { mkdirs() }
            val outFile = File(imagesDir, "payroll_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")

            val copied = runCatching {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    true
                } ?: false
            }.getOrElse { firstError ->
                Log.w(TAG, "openInputStream başarısız, fileDescriptor fallback deneniyor: $sourceUri", firstError)
                runCatching {
                    context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                        FileInputStream(pfd.fileDescriptor).use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        true
                    } ?: false
                }.getOrElse { secondError ->
                    outFile.delete()
                    throw IllegalArgumentException("Görsel okunamadı.", secondError)
                }
            }

            if (!copied) {
                outFile.delete()
                throw IllegalArgumentException("Görsel okunamadı.")
            }

            outFile
        }
    }

    companion object {
        private const val TAG = "PayrollImageStore"
    }
}
