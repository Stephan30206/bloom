package com.example.bloom.service

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAIService {

    // ✅ CORRECTION: Utilise Firebase.vertexAI au lieu de FirebaseVertexAI.getInstance()
    private val vertexAI = Firebase.vertexAI
    private val model = vertexAI.generativeModel("gemini-1.5-flash")

    suspend fun identifyPlantFromImage(bitmap: Bitmap): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Identify this plant, flower, or insect in the image.
                    Write a fun, interesting two-sentence fact about it.
                    
                    Please respond in this EXACT format:
                    Name: [the common or scientific name]
                    Fact: [exactly two sentences with interesting facts]
                    
                    Make it educational and engaging!
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = model.generateContent(inputContent)
                val responseText = response.text ?: throw Exception("Empty response from AI")

                parseAIResponse(responseText)

            } catch (e: Exception) {
                e.printStackTrace()
                Pair(
                    "Identification Error",
                    "Sorry, we couldn't identify this plant. Please try again with a clearer photo."
                )
            }
        }
    }

    private fun parseAIResponse(response: String): Pair<String, String> {
        try {
            val lines = response.trim().lines()
            var name = "Unknown Plant"
            var fact = "No information available."

            for (line in lines) {
                val trimmedLine = line.trim()
                when {
                    trimmedLine.startsWith("Name:", ignoreCase = true) -> {
                        name = trimmedLine.substringAfter("Name:").trim()
                    }
                    trimmedLine.startsWith("Fact:", ignoreCase = true) -> {
                        fact = trimmedLine.substringAfter("Fact:").trim()
                    }
                }
            }

            // Validation
            if (name.isBlank() || name == "Unknown Plant") {
                name = "Unidentified Plant"
            }
            if (fact.isBlank() || fact == "No information available.") {
                fact = "This plant couldn't be identified with confidence. Try taking a clearer photo with better lighting."
            }

            return Pair(name, fact)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair("Unknown Plant", "Unable to parse identification results.")
        }
    }
}