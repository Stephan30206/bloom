package com.example.bloom.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bloom.model.Plant
import com.example.bloom.repository.PlantRepository
import com.example.bloom.viewmodel.AuthViewModel
import androidx.compose.runtime.livedata.observeAsState
import com.example.bloom.viewmodel.AuthState

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    onGoogleSignInClick: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.observeAsState()

    // Gérer la navigation basée sur l'état d'authentification
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Si authentifié, naviguer vers PlantListScreen
                if (navController.currentDestination?.route != PlantListScreenRoute.toString()) {
                    navController.navigate(PlantListScreenRoute) {
                        popUpTo(0) // Clear back stack
                    }
                }
            }
            is AuthState.Unauthenticated, is AuthState.Error -> {
                // Si non authentifié, naviguer vers LoginScreen
                if (navController.currentDestination?.route != LoginScreenRoute.toString()) {
                    navController.navigate(LoginScreenRoute) {
                        popUpTo(0) // Clear back stack
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
        onGoogleSignInClick = onGoogleSignInClick
    )
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    onGoogleSignInClick: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = LoginScreenRoute
    ) {
        composable<LoginScreenRoute> {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel,
                onGoogleSignInClick = onGoogleSignInClick
            )
        }

        composable<SignUpScreenRoute> {
            SignUpScreen(
                navController = navController,
                authViewModel = authViewModel,
                onGoogleSignInClick = onGoogleSignInClick
            )
        }

        composable<PlantListScreenRoute> {
            PlantListScreen(
                onBackPressed = {
                    authViewModel.logout()
                    navController.navigate(LoginScreenRoute) {
                        popUpTo(PlantListScreenRoute) { inclusive = true }
                    }
                },
                onAddNewPlant = {
                    navController.navigate(DiscoveryScreenRoute)
                }
            )
        }

        composable<DiscoveryScreenRoute> {
            NewDiscoveryScreen(
                onBackPressed = { navController.popBackStack() },
                onSettingsPressed = {
                    navController.navigate(SettingsScreenRoute)
                },
                onPlantIdentified = { plantName, date, imageUri ->
                    // Ajouter la nouvelle plante identifiée
                    PlantRepository.addPlant(Plant(plantName, date, imageUri))
                    // Retourner à la liste des plantes
                    navController.popBackStack()
                }
            )
        }

        composable<SettingsScreenRoute> {
            SettingPage(
                onBackPressed = { navController.popBackStack() },
                onUpgradeToPro = { /* TODO: Handle upgrade */ },
                onChangeLanguage = { /* TODO: Handle language change */ },
                onShare = { /* TODO: Handle share */ },
                onContact = { /* TODO: Handle contact */ }
            )
        }
    }
}

// Routes supplémentaires
@kotlinx.serialization.Serializable
object PlantListScreenRoute

@kotlinx.serialization.Serializable
object DiscoveryScreenRoute

@kotlinx.serialization.Serializable
object SettingsScreenRoute