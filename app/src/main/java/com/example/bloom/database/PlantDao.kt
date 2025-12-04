package com.example.bloom.database

import androidx.room.*
import com.example.bloom.model.Plant
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPlantsByUser(userId: String): Flow<List<Plant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: Plant)

    @Delete
    suspend fun deletePlant(plant: Plant)

    @Query("SELECT * FROM plants WHERE id = :plantId")
    suspend fun getPlantById(plantId: String): Plant?

    @Query("DELETE FROM plants WHERE userId = :userId")
    suspend fun deleteAllPlantsForUser(userId: String)

    @Query("SELECT * FROM plants WHERE userId = :userId")
    suspend fun getPlantsSync(userId: String): List<Plant>

    @Query("UPDATE plants SET synced = :synced WHERE id = :plantId")
    suspend fun updateSyncedStatus(plantId: String, synced: Boolean)

    @Query("SELECT COUNT(*) FROM plants WHERE userId = :userId AND synced = 0")
    suspend fun getUnsyncedCount(userId: String): Int
}
