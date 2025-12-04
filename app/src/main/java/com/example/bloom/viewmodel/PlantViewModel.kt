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

    // Dans PlantViewModel.kt, ajoutez cette fonction
    fun syncPlants(userId: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                // Synchroniser avec Supabase
                // Cela va: 1) Télécharger les plantes d'autres appareils
                //          2) Envoyer les plantes locales non synchronisées
                plantRepository.syncWithSupabase(userId)

                // Recharger les plantes (depuis Room, qui contient maintenant les plantes fusionnées)
                loadPlants(userId)
            } catch (e: Exception) {
                _error.postValue("Sync error: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadPlants(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                plantRepository.getPlantsByUser(userId).collect { plantsList ->
                    _plants.postValue(plantsList)
                    _isLoading.postValue(false)
                }
            } catch (e: Exception) {
                _error.postValue("Plant loading error: ${e.message}")
                _isLoading.postValue(false)
            }
        }
    }

    fun identifyAndSavePlant(bitmap: Bitmap) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                Log.d("PlantViewModel", "Start of plant identification...")

                val userId = auth.currentUser?.uid
                    ?: throw Exception("You must be logged in")

                val imageFile = bitmapToFile(bitmap, userId)
                Log.d("PlantViewModel", "File created: ${imageFile.absolutePath}")

                val (name, summary) = geminiAIService.identifyPlantFromImage(bitmap)
                Log.d("PlantViewModel", "Identification result: $name")

                // VÉRIFICATION: Si ce n'est pas une plante, arrêter ici
                if (name == "NOT_A_PLANT") {
                    Log.d("PlantViewModel", "Image is not a plant, stopping process")
                    _error.postValue("This image does not appear to contain a plant.\nPlease take a photo of a real plant or flower..")

                    // Supprimer le fichier temporaire
                    imageFile.delete()

                    return@launch
                }

                Log.d("PlantViewModel", "Successful identification: $name")

                val imageUrl = supabaseStorageService.uploadImage(
                    imageFile = imageFile,
                    userId = userId,
                    plantName = name
                )
                Log.d("PlantViewModel", "Image uploaded: $imageUrl")

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

                loadPlants(userId)

                Log.d("PlantViewModel", "Saved plant: ${plant.id}")

                imageFile.delete()

            } catch (e: Exception) {
                Log.e("PlantViewModel", "Failure: ${e.message}", e)
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

                loadPlants(plant.userId)

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