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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.UUID

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

    private val storage = FirebaseStorage.getInstance()
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
                    ?: throw Exception("Utilisateur non authentifié")

                // 1. Identifier avec Gemini AI
                val (name, summary) = geminiAIService.identifyPlantFromImage(bitmap)
                Log.d("PlantViewModel", "Identification réussie: $name")

                // 2. Upload l'image vers Firebase Storage
                val imageUrl = uploadImageToStorage(bitmap, userId)
                Log.d("PlantViewModel", "Image uploadée: $imageUrl")

                // 3. Créer et sauvegarder la plante
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

                // 4. Recharger la liste des plantes
                loadPlants(userId)

                Log.d("PlantViewModel", "Plante sauvegardée et liste rechargée: ${plant.id}")

            } catch (e: Exception) {
                Log.e("PlantViewModel", "Échec identification: ${e.message}", e)
                _error.postValue("Échec de l'identification: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun uploadImageToStorage(bitmap: Bitmap, userId: String): String {
        return try {
            val imageId = UUID.randomUUID().toString()
            val storageRef = storage.reference
                .child("users/$userId/plants/$imageId.jpg")

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            // Upload l'image
            storageRef.putBytes(data).await()

            // Récupérer l'URL de téléchargement
            val downloadUrl = storageRef.downloadUrl.await()
            Log.d("PlantViewModel", "URL image générée: $downloadUrl")
            downloadUrl.toString()

        } catch (e: Exception) {
            Log.e("PlantViewModel", "Erreur upload Firebase Storage: ${e.message}", e)
            // Fallback: utiliser une image placeholder
            "https://via.placeholder.com/400x300/4CAF50/FFFFFF?text=Plante+Identifi%C3%A9e"
        }
    }

    fun getPlantById(plantId: String) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                _currentPlant.postValue(plantRepository.getPlantById(plantId))
                _isLoading.postValue(false)
            } catch (e: Exception) {
                _error.postValue("Erreur de chargement de la plante: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)

                // Supprimer de la base de données
                plantRepository.deletePlant(plant)

                // Supprimer l'image de Firebase Storage
                try {
                    if (plant.imageUrl.startsWith("https://firebasestorage.googleapis.com/")) {
                        val storageRef = storage.getReferenceFromUrl(plant.imageUrl)
                        storageRef.delete().await()
                    }
                } catch (e: Exception) {
                    // L'image n'existe peut-être plus, continuer quand même
                    Log.w("PlantViewModel", "Impossible de supprimer l'image: ${e.message}")
                }

                // Recharger la liste
                plant.userId.let { userId ->
                    loadPlants(userId)
                }

            } catch (e: Exception) {
                _error.postValue("Erreur de suppression: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearError() {
        _error.postValue(null)
    }

    fun clearCurrentPlant() {
        _currentPlant.postValue(null)
    }
}