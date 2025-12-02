package com.example.bloom.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloom.model.Plant
import com.example.bloom.repository.PlantRepository
import com.example.bloom.service.GeminiAIService
import com.example.bloom.service.SupabaseStorageService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PlantViewModel(
    private val plantRepository: PlantRepository,
    private val geminiAIService: GeminiAIService
) : ViewModel() {

    private val _plants = MutableLiveData<List<Plant>>(emptyList())
    val plants: LiveData<List<Plant>> = _plants

    private val _currentPlant = MutableLiveData<Plant?>(null)
    val currentPlant: LiveData<Plant?> = _currentPlant

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val supabaseStorageService = SupabaseStorageService()
    private val auth = FirebaseAuth.getInstance()

    fun loadPlants(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                plantRepository.getPlantsByUser(userId).collect { plantsList ->
                    _plants.postValue(plantsList)
                    _isLoading.postValue(false)
                }
            } catch (e: Exception) {
                _error.postValue("Erreur de chargement des plantes: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    fun identifyAndSavePlant(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                Log.d("PlantViewModel", "Début identification plante...")

                val userId = auth.currentUser?.uid
                    ?: throw Exception("Vous devez être connecté")

                // 1. Convertir Bitmap en File
                val imageFile = bitmapToFile(bitmap, userId)
                Log.d("PlantViewModel", "Fichier créé: ${imageFile.absolutePath}")

                // 2. Identifier avec Gemini AI
                val (name, summary) = geminiAIService.identifyPlantFromImage(bitmap)
                Log.d("PlantViewModel", "Identification réussie: $name")

                // 3. Upload vers Supabase (CORRIGÉ: passer le File et plantName)
                val imageUrl = supabaseStorageService.uploadImage(
                    imageFile = imageFile,
                    userId = userId,
                    plantName = name
                )
                Log.d("PlantViewModel", "Image uploadée: $imageUrl")

                // 4. Créer et sauvegarder la plante
                val plant = Plant(
                    id = Plant.generateId(),
                    name = name,
                    summary = summary,
                    imageUrl = imageUrl,
                    timestamp = System.currentTimeMillis(),
                    userId = userId
                )

                plantRepository.insertPlant(plant)
                _currentPlant.postValue(plant)

                // 5. Recharger la liste
                loadPlants(userId)

                Log.d("PlantViewModel", "Plante sauvegardée: ${plant.id}")

                // 6. Nettoyer le fichier temporaire
                imageFile.delete()

            } catch (e: Exception) {
                Log.e("PlantViewModel", "Échec: ${e.message}", e)
                _error.postValue("Erreur: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Fonction pour convertir Bitmap en File
    private fun bitmapToFile(bitmap: Bitmap, userId: String): File {
        val file = File.createTempFile("plant_${userId}_", ".jpg")
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.flush()
            stream.close()
        } catch (e: Exception) {
            Log.e("PlantViewModel", "Erreur conversion bitmap: ${e.message}")
        }
        return file
    }

    fun getPlantById(plantId: String) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                _currentPlant.postValue(plantRepository.getPlantById(plantId))
                _isLoading.postValue(false)
            } catch (e: Exception) {
                _error.postValue("Erreur de chargement: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)

                // 1. Supprimer de la base de données
                plantRepository.deletePlant(plant)

                // 2. Optionnel: Ajouter une fonction deleteImage dans SupabaseStorageService
                // si tu veux supprimer l'image de Supabase aussi
                // supabaseStorageService.deleteImage(plant.imageUrl)

                // 3. Recharger la liste
                loadPlants(plant.userId)

            } catch (e: Exception) {
                _error.postValue("Erreur de suppression: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Ajoute cette fonction si tu veux supprimer les images
    suspend fun deleteImageFromSupabase(imageUrl: String) {
        // Tu peux implémenter cette fonction dans SupabaseStorageService
        // puis l'appeler ici
    }

    fun clearError() {
        _error.postValue(null)
    }

    fun clearCurrentPlant() {
        _currentPlant.postValue(null)
    }
}