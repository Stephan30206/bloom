package com.example.bloom.viewmodel

import android.graphics.Bitmap
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

    private val _error = MutableLiveData<String?>(null)
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
                _error.postValue("Failed to load plants: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    fun identifyAndSavePlant(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                val userId = auth.currentUser?.uid
                    ?: throw Exception("User not authenticated")

                // 1. Identifier avec Gemini AI
                val (name, summary) = geminiAIService.identifyPlantFromImage(bitmap)

                // 2. Upload l'image vers Firebase Storage
                val imageUrl = uploadImageToStorage(bitmap, userId)

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

            } catch (e: Exception) {
                _error.postValue("Failed to identify plant: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun uploadImageToStorage(bitmap: Bitmap, userId: String): String {
        val imageId = UUID.randomUUID().toString()
        val storageRef = storage.reference
            .child("users/$userId/plants/$imageId.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data).await()
        return storageRef.downloadUrl.await().toString()
    }

    fun getPlantById(plantId: String) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                _currentPlant.postValue(plantRepository.getPlantById(plantId))
                _isLoading.postValue(false)
            } catch (e: Exception) {
                _error.postValue("Failed to load plant: ${e.message}")
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
                    val storageRef = storage.getReferenceFromUrl(plant.imageUrl)
                    storageRef.delete().await()
                } catch (e: Exception) {
                    // L'image n'existe peut-être plus, continuer quand même
                    e.printStackTrace()
                }

                // Recharger la liste
                plant.userId.let { userId ->
                    loadPlants(userId)
                }

            } catch (e: Exception) {
                _error.postValue("Failed to delete plant: ${e.message}")
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