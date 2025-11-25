package com.example.bloom.navigation

import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object SignUpRoute

@Serializable
data class PlantListRoute(val userId: String)

@Serializable
object DiscoveryRoute

@Serializable
data class PlantDetailRoute(val plantId: String)

@Serializable
object SettingsRoute