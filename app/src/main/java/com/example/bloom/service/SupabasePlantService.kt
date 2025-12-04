package com.example.bloom.service

import com.example.bloom.model.Plant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabasePlant(
    @SerialName("id")
    val id: String,

    @SerialName("name")
    val name: String,

    @SerialName("summary")
    val summary: String,

    @SerialName("image_url")
    val imageUrl: String,

    @SerialName("timestamp")
    val timestamp: Long,

    @SerialName("user_id")
    val userId: String
)

class SupabasePlantService(private val supabaseClient: SupabaseClient) {

    suspend fun getPlantsByUser(userId: String): List<Plant> {
        return withContext(Dispatchers.IO) {
            try {
                val response = supabaseClient
                    .postgrest["plants"]
                    .select {
                        // Filtrer par utilisateur
                        eq("user_id", userId)

                        // Trier par timestamp décroissant
                        order(column = "timestamp", order = Order.DESCENDING)
                    }
                    .decodeList<SupabasePlant>()

                response.map { supabasePlant ->
                    Plant(
                        id = supabasePlant.id,
                        name = supabasePlant.name,
                        summary = supabasePlant.summary,
                        imageUrl = supabasePlant.imageUrl,
                        timestamp = supabasePlant.timestamp,
                        userId = supabasePlant.userId,
                        synced = true
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun insertPlant(plant: Plant): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val supabasePlant = SupabasePlant(
                    id = plant.id,
                    name = plant.name,
                    summary = plant.summary,
                    imageUrl = plant.imageUrl,
                    timestamp = plant.timestamp,
                    userId = plant.userId
                )

                supabaseClient
                    .postgrest["plants"]
                    .insert(supabasePlant)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun deletePlant(plantId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                supabaseClient
                    .postgrest["plants"]
                    .delete {
                        eq("id", plantId)
                    }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun deleteAllPlantsForUser(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                supabaseClient
                    .postgrest["plants"]
                    .delete {
                        eq("user_id", userId)
                    }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}