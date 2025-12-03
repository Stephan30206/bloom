package com.example.bloom.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class GeminiAIService {

    private val apiKey = "AIzaSyCEhnWck8XJhdV9ZKTqOuaWJbg2Zxqg35o"
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

    suspend fun identifyPlantFromImage(bitmap: Bitmap): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                Log.d("GeminiAI", "Starting identification using direct Gemini API...")

                val base64Image = bitmapToBase64(bitmap)
                Log.d("GeminiAI", "Base64 image size: ${base64Image.length} characters")

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", """
                                        Your task is to identify the plant or flower shown in the image. 
                                        You must ONLY identify plants or flowers.
                                        
                                        If the image does not contain a plant or flower, reply with:
                                        "Error: This image does not contain a plant or flower."
                                        
                                        Then provide an interesting fact about the identified plant in exactly two sentences.
                                        
                                        Follow this response format strictly (keep the important label in bold):
                                        
                                        [common or scientific name]  
                                        [two interesting sentences]
                                        
                                        Respond in English.

                                    """.trimIndent())
                                })
                                put(JSONObject().apply {
                                    put("inline_data", JSONObject().apply {
                                        put("mime_type", "image/jpeg")
                                        put("data", base64Image)
                                    })
                                })
                            })
                        })
                    })
                }

                val requestBodyString = requestBody.toString()
                Log.d("GeminiAI", "Request size: ${requestBodyString.length} characters")

                val url = URL(apiUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                    connectTimeout = 30000
                    readTimeout = 30000

                    outputStream.use { os ->
                        os.write(requestBodyString.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                }

                val responseCode = connection.responseCode
                Log.d("GeminiAI", "HTTP Response Code: $responseCode")

                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("GeminiAI", "Response received (${response.length} characters)")
                        parseResponse(response)
                    }

                    HttpURLConnection.HTTP_BAD_REQUEST -> {
                        val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("GeminiAI", "400 Bad Request: $error")
                        Pair(
                            "Invalid request",
                            "The request is malformed. Please verify the image format."
                        )
                    }

                    HttpURLConnection.HTTP_FORBIDDEN -> {
                        val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("GeminiAI", "403 Forbidden: $error")
                        Pair(
                            "Invalid API key",
                            "Your API key is invalid or does not have the required permissions."
                        )
                    }

                    429 -> {
                        val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("GeminiAI", "429 Quota exceeded: $error")
                        Pair(
                            "Quota exceeded",
                            "Request limit reached. Please try again in a few minutes."
                        )
                    }

                    else -> {
                        val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("GeminiAI", "HTTP Error $responseCode: $error")
                        Pair(
                            "API Error ($responseCode)",
                            "Server error: ${error?.take(200) ?: "No details available"}"
                        )
                    }
                }

            } catch (e: java.net.SocketTimeoutException) {
                Log.e("GeminiAI", "Timeout", e)
                Pair(
                    "Timeout",
                    "The server is not responding. Please try again with a smaller image."
                )
            } catch (e: java.net.UnknownHostException) {
                Log.e("GeminiAI", "No Internet connection", e)
                Pair(
                    "Connection Error",
                    "No Internet connection. Please check your network."
                )
            } catch (e: java.io.IOException) {
                Log.e("GeminiAI", "I/O Error: ${e.message}", e)
                Pair(
                    "Network Error",
                    "Connection problem: ${e.localizedMessage}"
                )
            } catch (e: Exception) {
                Log.e("GeminiAI", "Unexpected error: ${e.javaClass.simpleName}", e)
                e.printStackTrace()
                Pair(
                    "System Error",
                    "Error: ${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()

        val maxDimension = 1024
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = minOf(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height
            )
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Log.d("GeminiAI", "Resizing: ${bitmap.width}x${bitmap.height} -> ${newWidth}x${newHeight}")
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()

        Log.d("GeminiAI", "Compressed image size: ${byteArray.size / 1024} KB")

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun parseResponse(response: String): Pair<String, String> {
        return try {
            val jsonResponse = JSONObject(response)

            if (jsonResponse.has("error")) {
                val error = jsonResponse.getJSONObject("error")
                val errorMessage = error.optString("message", "Unknown error")
                Log.e("GeminiAI", "Error in response: $errorMessage")
                return Pair("API Error", errorMessage)
            }

            val candidates = jsonResponse.getJSONArray("candidates")
            if (candidates.length() == 0) {
                Log.e("GeminiAI", "No candidate found in the response")
                return Pair("No result", "The API could not identify the image")
            }

            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).getString("text")

            Log.d("GeminiAI", "🔍 Received text:\n$text")

            var name = ""
            var fact = ""

            text.lines().forEach { line ->
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("Name:", ignoreCase = true) -> {
                        name = trimmedLine.substringAfter(":").trim()
                    }
                    trimmedLine.startsWith("Fact:", ignoreCase = true) -> {
                        fact = trimmedLine.substringAfter(":").trim()
                    }
                }
            }

            if (name.isEmpty() && fact.isEmpty() && text.isNotBlank()) {
                val lines = text.trim().lines().filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    name = lines.firstOrNull() ?: "Plant identified"
                    fact = lines.drop(1).joinToString(" ").take(200)
                }
            }

            if (name.isEmpty() || fact.isEmpty()) {
                Log.w("GeminiAI", "Incomplete format - Name: '$name', Fact: '$fact'")
                Pair(
                    name.ifEmpty { "Partial identification" },
                    fact.ifEmpty { text.take(200) }
                )
            } else {
                Log.d("GeminiAI", "Name: $name")
                Log.d("GeminiAI", "Fact: ${fact.take(50)}...")
                Pair(name, fact)
            }

        } catch (e: org.json.JSONException) {
            Log.e("GeminiAI", "Parsing error", e)
            Pair(
                "Parsing Error",
                "Unable to parse JSON response: ${e.localizedMessage}"
            )
        } catch (e: Exception) {
            Log.e("GeminiAI", "Unexpected parsing error", e)
            Pair(
                "Parsing Error",
                "Error: ${e.localizedMessage}"
            )
        }
    }
}
