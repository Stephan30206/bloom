package com.example.bloom.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavHostController
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
                // Si authentifié, naviguer vers PlantListScreen
                if (navController.currentDestination?.route != Screen.PlantList.route) {
                    navController.navigate(Screen.PlantList.route) {
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

    NavigationGraph(
        navController = navController,
        authViewModel = authViewModel,
        plantViewModel = plantViewModel,
        onGoogleSignInClick = onGoogleSignInClick
    )
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel,
    onGoogleSignInClick: () -> Unit = {}
) {
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

        composable(Screen.PlantList.route) {
            val authState by authViewModel.authState.observeAsState()
            when (authState) {
                is AuthState.Authenticated -> {
                    PlantListScreen(
                        navController = navController,
                        authViewModel = authViewModel,
                        plantViewModel = plantViewModel,
                        userId = (authState as AuthState.Authenticated).userId
                    )
                }
                else -> {
                    // Rediriger vers le login si non authentifié
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.PlantList.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable(Screen.Discovery.route) {
            val authState by authViewModel.authState.observeAsState()
            when (authState) {
                is AuthState.Authenticated -> {
                    NewDiscoveryScreen(
                        navController = navController,
                        plantViewModel = plantViewModel
                    )
                }
                else -> {
                    // Rediriger vers le login si non authentifié
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Discovery.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable(Screen.Settings.route) {
            SettingPage(
                onBackPressed = { navController.popBackStack() },
                onUpgradeToPro = { /* TODO: Handle upgrade */ },
                onChangeLanguage = { /* TODO: Handle language change */ },
                onShare = { /* TODO: Handle share */ },
                onContact = { /* TODO: Handle contact */ }
            )
        }

        composable(Screen.PlantDetail.route) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
            val authState by authViewModel.authState.observeAsState()
            when (authState) {
                is AuthState.Authenticated -> {
                    PlantDetailScreen(
                        navController = navController,
                        plantViewModel = plantViewModel,
                        plantId = plantId
                    )
                }
                else -> {
                    // Rediriger vers le login si non authentifié
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.PlantDetail.route) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}

// Routes
const val LoginScreenRoute = "login"
const val SignUpScreenRoute = "signup"
const val PlantListScreenRoute = "plant_list"
const val DiscoveryScreenRoute = "discovery"
const val SettingsScreenRoute = "settings"
const val PlantDetailRoute = "plant_detail"