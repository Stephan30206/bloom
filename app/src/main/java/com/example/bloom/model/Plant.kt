package com.example.bloom.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val summary: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val synced: Boolean = false
) {
    companion object {
        fun generateId(): String = "plant_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}