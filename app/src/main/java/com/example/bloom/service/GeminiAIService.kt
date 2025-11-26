package com.example.bloom.service

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAIService {
    private val generativeModel = Firebase.ai.generativeModel("gemini-1.5-flash")

    suspend fun identifyPlantFromImage(bitmap: Bitmap): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("GeminiAI", "Starting plant identification with Firebase AI...")

                val prompt = """
                    Identify this plant, flower, or insect in the image.
                    Write a fun, interesting two-sentence fact about it.
                    
                    Please respond in this EXACT format:
                    Name: [the common or scientific name]
                    Fact: [exactly two sentences with interesting facts]
                    
                    Make it educational and engaging!
                """.trimIndent()

                // Correct way to use Firebase Gemini AI with image
                val inputContent = content {
                    text(prompt)
                    image(bitmap)
                }

                val response = generativeModel.generateContent(inputContent)

                val responseText = response.text ?: throw Exception("Réponse vide de l'IA")

                Log.d("GeminiAI", "Réponse AI: $responseText")

                parseAIResponse(responseText)
            } catch (e: Exception) {
                Log.e("GeminiAI", "Erreur identification plante: ${e.message}", e)
                Pair(
                    "Plante Mystère",
                    "Nous avons du mal à identifier cette plante. Essayez avec une photo plus nette et un meilleur éclairage."
                )
            }
        }
    }

    private fun parseAIResponse(response: String): Pair<String, String> {
        return try {
            var name = "Plante Mystère"
            var fact = "Aucune information disponible pour le moment."

            val lines = response.lines()
            for (line in lines) {
                when {
                    line.trim().startsWith("Name:", ignoreCase = true) -> {
                        name = line.substringAfter(":").trim()
                        if (name.isBlank()) name = "Plante Mystère"
                    }
                    line.trim().startsWith("Fact:", ignoreCase = true) -> {
                        fact = line.substringAfter(":").trim()
                        if (fact.isBlank()) fact = "Cette plante présente des caractéristiques uniques dignes d'exploration."
                    }
                }
            }

            Log.d("GeminiAI", "Parsé - Nom: $name, Fait: $fact")
            Pair(name, fact)
        } catch (e: Exception) {
            Log.e("GeminiAI", "Erreur parsing réponse AI", e)
            Pair("Plante Mystère", "Impossible d'analyser les résultats d'identification. Veuillez réessayer.")
        }
    }
}