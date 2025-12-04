package com.example.bloom.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bloom.navigation.Screen
import com.example.bloom.viewmodel.AuthState
import com.example.bloom.viewmodel.AuthViewModel
import com.example.bloom.viewmodel.PlantViewModel

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel,
    onGoogleSignInClick: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.observeAsState()

    // Gérer la navigation basée sur l'état d'authentification
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Si authentifié, naviguer vers MainScreenWithBottomNav
                if (navController.currentDestination?.route != "main_with_bottom_nav") {
                    navController.navigate("main_with_bottom_nav") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated, is AuthState.Error -> {
                // Si non authentifié, naviguer vers LoginScreen
                if (navController.currentDestination?.route != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {
                // AuthState.Loading - on reste sur l'écran actuel
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel,
                onGoogleSignInClick = onGoogleSignInClick
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                navController = navController,
                authViewModel = authViewModel,
                onGoogleSignInClick = onGoogleSignInClick
            )
        }

        // 🔧 NOUVEAU : Écran principal avec bottom navigation
        composable("main_with_bottom_nav") {
            val currentAuthState by authViewModel.authState.observeAsState()
            when (currentAuthState) {
                is AuthState.Authenticated -> {
                    MainScreenWithBottomNav(
                        authViewModel = authViewModel,
                        plantViewModel = plantViewModel,
                        userId = (currentAuthState as AuthState.Authenticated).userId
                    )
                }
                else -> {
                    // Rediriger vers le login si non authentifié
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo("main_with_bottom_nav") { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}

// Routes constantes (conservées pour compatibilité)
const val LoginScreenRoute = "login"
const val SignUpScreenRoute = "signup"
const val PlantListScreenRoute = "plant_list"
const val DiscoveryScreenRoute = "discovery"
const val SettingsScreenRoute = "settings"
const val PlantDetailRoute = "plant_detail"