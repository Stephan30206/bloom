package com.example.bloom.model

data class Plant(
    val name: String,
    val date: String,
    val imageUri: String? = null,
    val id: Long = System.currentTimeMillis()
)