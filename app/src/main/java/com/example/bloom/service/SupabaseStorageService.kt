package com.example.bloom.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class SupabaseStorageService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val supabaseUrl = "https://xygckyyshsnottpmciil.supabase.co"
    private val anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh5Z2NreXlzaHNub3R0cG1jaWlsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ0ODA1NTUsImV4cCI6MjA4MDA1NjU1NX0.IVKl69PiM3ZjZdCaiutdCFhlssAMrI1kZRIPkQnJh1Q"

    suspend fun uploadImage(
        imageFile: File,
        userId: String,
        plantName: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d("SupabaseStorage", "Début upload pour user: $userId")

            // NETTOYER le nom de la plante pour le nom de fichier
            val cleanPlantName = plantName
                .replace("[^a-zA-Z0-9-_]".toRegex(), "_")
                .take(50)

            val fileName = "${cleanPlantName}_${System.currentTimeMillis()}.jpg"

            // ✅ CHANGER ICI : utiliser "plantimages" au lieu de "plants"
            val bucketName = "plantimages"  // ← BUCKET PUBLIC SANS RLS
            val filePath = "user_$userId/$fileName"

            Log.d("SupabaseStorage", "Bucket: $bucketName")
            Log.d("SupabaseStorage", "Chemin: $filePath")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    imageFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            // ✅ URL CORRECTE avec "plantimages"
            val request = Request.Builder()
                .url("$supabaseUrl/storage/v1/object/$bucketName/$filePath")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $anonKey")
                .addHeader("apikey", anonKey)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("SupabaseStorage", "Response code: ${response.code}")
            Log.d("SupabaseStorage", "Response: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("Upload failed: ${response.code} - $responseBody")
            }

            // ✅ URL PUBLIQUE CORRECTE
            val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$filePath"
            Log.d("SupabaseStorage", "Upload successful: $publicUrl")
            return@withContext publicUrl

        } catch (e: Exception) {
            Log.e("SupabaseStorage", "Erreur upload: ${e.message}", e)
            throw Exception("Upload failed: ${e.message}")
        }
    }
}