package com.example.communitys

import android.content.Context
import android.net.Uri
import com.example.communitys.CommUnityApplication
import io.github.jan.supabase.storage.storage
import java.util.UUID

class SupabaseStorageHelper {

    private val supabase = CommUnityApplication.supabase

    /**
     * Uploads an image to Supabase Storage using ContentResolver.
     * This correctly handles URIs from Android's photo picker.
     * Returns the public URL of the uploaded image on success.
     */
    suspend fun uploadImage(
        bucketName: String,
        fileUri: Uri,
        userId: String,
        context: Context
    ): Result<String> {
        return try {
            // ✅ Use contentResolver — works with photo picker URIs
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return Result.failure(Exception("Could not open image"))

            val fileBytes = inputStream.readBytes()
            inputStream.close()

            // Unique file path: userId/uuid.jpg
            val fileName = "$userId/${UUID.randomUUID()}.jpg"

            supabase.storage[bucketName].upload(
                path = fileName,
                data = fileBytes,
                upsert = false
            )

            // Return the public URL
            val publicUrl = supabase.storage[bucketName].publicUrl(fileName)
            Result.success(publicUrl)

        } catch (e: Exception) {
            Result.failure(Exception("Failed to upload image: ${e.message}"))
        }
    }
}