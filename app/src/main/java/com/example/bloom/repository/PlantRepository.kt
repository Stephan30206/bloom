package com.example.bloom.repository

import com.example.bloom.database.PlantDao
import com.example.bloom.model.Plant
import com.example.bloom.service.SupabasePlantService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlantRepository(
    private val plantDao: PlantDao,
    private val supabasePlantService: SupabasePlantService
) {

    // Cette fonction retourne les plantes DE ROOM (toujours)
    fun getPlantsByUser(userId: String): Flow<List<Plant>> {
        return plantDao.getPlantsByUser(userId).map { plants ->
            plants.sortedByDescending { it.timestamp }
        }
    }

    suspend fun insertPlant(plant: Plant) {
        // 1. TOUJOURS insérer dans Room d'abord
        plantDao.insertPlant(plant.copy(synced = false))

        // 2. Ensuite, tenter la synchronisation avec Supabase (en arrière-plan)
        syncToSupabase(plant)
    }

    suspend fun deletePlant(plant: Plant) {
        // 1. TOUJOURS supprimer de Room d'abord
        plantDao.deletePlant(plant)

        // 2. Ensuite, tenter la suppression sur Supabase
        try {
            supabasePlantService.deletePlant(plant.id)
        } catch (e: Exception) {
            // Même si Supabase échoue, la suppression locale est faite
        }
    }

    suspend fun getPlantById(plantId: String): Plant? {
        // TOUJOURS depuis Room
        return plantDao.getPlantById(plantId)
    }

    suspend fun deleteAllPlantsForUser(userId: String) {
        // 1. TOUJOURS supprimer de Room d'abord
        plantDao.deleteAllPlantsForUser(userId)

        // 2. Ensuite, tenter sur Supabase
        try {
            supabasePlantService.deleteAllPlantsForUser(userId)
        } catch (e: Exception) {
            // Ignorer si Supabase échoue
        }
    }

    // Fonction de synchronisation MANUELLE (quand l'utilisateur se connecte sur un nouvel appareil)
    suspend fun syncWithSupabase(userId: String): Boolean {
        return try {
            // Étape 1: Récupérer les plantes de Supabase (autres appareils)
            val remotePlants = supabasePlantService.getPlantsByUser(userId)

            // Étape 2: Récupérer les plantes locales
            val localPlants = plantDao.getPlantsSync(userId)

            // Étape 3: Fusionner - ajouter les plantes distantes qui n'existent pas localement
            remotePlants.forEach { remotePlant ->
                val existsLocally = localPlants.any { it.id == remotePlant.id }
                if (!existsLocally) {
                    // Cette plante vient d'un autre appareil, l'ajouter localement
                    plantDao.insertPlant(remotePlant.copy(synced = true))
                }
            }

            // Étape 4: Pousser les plantes locales non synchronisées vers Supabase
            localPlants.filter { !it.synced }.forEach { localPlant ->
                if (supabasePlantService.insertPlant(localPlant)) {
                    plantDao.updateSyncedStatus(localPlant.id, true)
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    // Synchronisation automatique en arrière-plan (pour une nouvelle plante)
    private suspend fun syncToSupabase(plant: Plant) {
        try {
            if (supabasePlantService.insertPlant(plant)) {
                plantDao.updateSyncedStatus(plant.id, true)
            }
        } catch (e: Exception) {
            // Échec de synchronisation, la plante reste avec synced = false
            // Elle sera synchronisée plus tard
        }
    }
}