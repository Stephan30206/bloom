package com.example.bloom.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object PlantList : Screen("plant_list")
    object Discovery : Screen("discovery")
    object Settings : Screen("settings")
    object PlantDetail : Screen("plant_detail/{plantId}") {
        fun createRoute(plantId: String) = "plant_detail/$plantId"
    }
}

// Routes constantes pour un accès facile
object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val PLANT_LIST = "plant_list"
    const val DISCOVERY = "discovery"
    const val SETTINGS = "settings"

    fun plantDetail(plantId: String): String = "plant_detail/$plantId"
}