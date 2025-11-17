package com.example.bloom.repository

import androidx.compose.runtime.mutableStateListOf
import com.example.bloom.model.Plant

object PlantRepository {
    val plants = mutableStateListOf(
        Plant("Monstra Deliciosa", "10/06/2023")
//        Plant("Fiddle Leaf Fig", "10/06/2023"),
//        Plant("Snake Plant", "10/04/2023"),
//        Plant("Pothos", "10/02/2023"),
//        Plant("ZZ Plant", "10/02/2023")
    )

    fun addPlant(plant: Plant) {
        plants.add(0, plant)
    }
}