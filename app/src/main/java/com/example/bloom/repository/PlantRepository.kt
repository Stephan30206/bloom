package com.example.bloom.repository

import com.example.bloom.database.PlantDao
import com.example.bloom.model.Plant
import kotlinx.coroutines.flow.Flow

class PlantRepository(
    private val plantDao: PlantDao
) {
    fun getPlantsByUser(userId: String): Flow<List<Plant>> {
        return plantDao.getPlantsByUser(userId)
    }

    suspend fun insertPlant(plant: Plant) {
        plantDao.insertPlant(plant)
    }

    suspend fun deletePlant(plant: Plant) {
        plantDao.deletePlant(plant)
    }

    suspend fun getPlantById(plantId: String): Plant? {
        return plantDao.getPlantById(plantId)
    }

    suspend fun deleteAllPlantsForUser(userId: String) {
        plantDao.deleteAllPlantsForUser(userId)
    }
}