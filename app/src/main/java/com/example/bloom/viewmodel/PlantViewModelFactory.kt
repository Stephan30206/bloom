package com.example.bloom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bloom.repository.PlantRepository
import com.example.bloom.service.GeminiAIService

class PlantViewModelFactory(
    private val plantRepository: PlantRepository,
    private val geminiAIService: GeminiAIService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlantViewModel::class.java)) {
            return PlantViewModel(plantRepository, geminiAIService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}