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

    // 🔧 CORRECTION 1: Toujours synchroniser avant de charger
    fun loadPlants(userId: String, forceSync: Boolean = false) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)

                // ✅ Synchroniser avec Supabase D'ABORD
                if (forceSync) {
                    Log.d("PlantViewModel", "🔄 Force sync with Supabase...")
                    plantRepository.syncWithSupabase(userId)
                }

                // ✅ Ensuite charger depuis Room (qui contient maintenant toutes les données)
                plantRepository.getPlantsByUser(userId).collect { plantsList ->
                    Log.d("PlantViewModel", "📦 Loaded ${plantsList.size} plants from Room")
                    _plants.postValue(plantsList)
                    _isLoading.postValue(false)
                }
            } catch (e: Exception) {
                Log.e("PlantViewModel", "❌ Plant loading error", e)
                _error.postValue("Plant loading error: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    // 🔧 CORRECTION 2: Fonction de sync explicite avec feedback
    fun syncPlants(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("PlantViewModel", "🔄 Starting manual sync...")
                _isLoading.postValue(true)

                val success = plantRepository.syncWithSupabase(userId)

                if (success) {
                    Log.d("PlantViewModel", "✅ Sync successful")
                    // Recharger les plantes après sync
                    loadPlants(userId, forceSync = false)
                } else {
                    Log.e("PlantViewModel", "❌ Sync failed")
                    _error.postValue("Sync failed. Check your internet connection.")
                }
            } catch (e: Exception) {
                Log.e("PlantViewModel", "❌ Sync error", e)
                _error.postValue("Sync error: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun identifyAndSavePlant(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                Log.d("PlantViewModel", "🔍 Start of plant identification...")

                val userId = auth.currentUser?.uid
                    ?: throw Exception("You must be logged in")

                val imageFile = bitmapToFile(bitmap, userId)
                Log.d("PlantViewModel", "📁 File created: ${imageFile.absolutePath}")

                val (name, summary) = geminiAIService.identifyPlantFromImage(bitmap)
                Log.d("PlantViewModel", "🌿 Identification result: $name")

                if (name == "NOT_A_PLANT") {
                    Log.d("PlantViewModel", "⚠️ Image is not a plant, stopping process")
                    _error.postValue("This image does not appear to contain a plant.\nPlease take a photo of a real plant or flower.")
                    imageFile.delete()
                    return@launch
                }

                Log.d("PlantViewModel", "✅ Successful identification: $name")

                val imageUrl = supabaseStorageService.uploadImage(
                    imageFile = imageFile,
                    userId = userId,
                    plantName = name
                )
                Log.d("PlantViewModel", "☁️ Image uploaded: $imageUrl")

                val plant = Plant(
                    id = Plant.generateId(),
                    name = name,
                    summary = summary,
                    imageUrl = imageUrl,
                    timestamp = System.currentTimeMillis(),
                    userId = userId
                )

                // ✅ Sauvegarder et synchroniser
                plantRepository.insertPlant(plant)
                _currentPlant.postValue(plant)

                // ✅ Recharger les plantes
                loadPlants(userId, forceSync = false)

                Log.d("PlantViewModel", "💾 Saved plant: ${plant.id}")

                imageFile.delete()

            } catch (e: Exception) {
                Log.e("PlantViewModel", "❌ Failure: ${e.message}", e)
                _error.postValue("Error: ${e.localizedMessage}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun bitmapToFile(bitmap: Bitmap, userId: String): File {
        val file = File.createTempFile("plant_${userId}_", ".jpg")
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.flush()
            stream.close()
        } catch (e: Exception) {
            Log.e("PlantViewModel", "Bitmap conversion error: ${e.message}")
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
                _error.postValue("Loading error: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    fun deletePlant(plant: Plant) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                plantRepository.deletePlant(plant)
                loadPlants(plant.userId, forceSync = false)
            } catch (e: Exception) {
                _error.postValue("Deletion error: ${e.message}")
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