package com.example.bloom.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bloom.navigation.Screen
import com.example.bloom.viewmodel.AuthViewModel
import com.example.bloom.viewmodel.PlantViewModel

sealed class BottomNavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavScreen(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )

    object Scanner : BottomNavScreen(
        route = "scanner",
        title = "Scan",
        icon = Icons.Default.Add
    )

    object Profile : BottomNavScreen(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.Person
    )
}

@Composable
fun MainScreenWithBottomNav(
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel,
    userId: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(
        BottomNavScreen.Home.route,
        BottomNavScreen.Scanner.route,
        BottomNavScreen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavRoutes) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    val items = listOf(
                        BottomNavScreen.Home,
                        BottomNavScreen.Scanner,
                        BottomNavScreen.Profile
                    )

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(BottomNavScreen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2E7D32),
                                selectedTextColor = Color(0xFF2E7D32),
                                indicatorColor = Color(0xFFE8F5E9),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavScreen.Home.route) {
                PlantListScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    plantViewModel = plantViewModel,
                    userId = userId
                )
            }

            composable(BottomNavScreen.Scanner.route) {
                NewDiscoveryScreen(
                    navController = navController,
                    plantViewModel = plantViewModel
                )
            }

            composable(BottomNavScreen.Profile.route) {
                ProfileScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    userId = userId
                )
            }

            composable(Screen.PlantDetail.route) { backStackEntry ->
                val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
                PlantDetailScreen(
                    navController = navController,
                    plantViewModel = plantViewModel,
                    plantId = plantId
                )
            }

            composable("settings") {
                SettingsScreen(
                    navController = navController,
                    settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    authViewModel = authViewModel
                )
            }

            composable("about") {
                AboutScreen(navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userId: String
) {
    val currentUserEmail by authViewModel.currentUserEmail.observeAsState()
    val isEmailVerified by authViewModel.isEmailVerified.observeAsState(false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card avec info utilisateur - Email au lieu de User ID
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(48.dp)
                        )
                        Column {
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = currentUserEmail ?: "Not signed in",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navController.navigate("settings")
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF2E7D32)
                        )
                        Text("Settings")
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navController.navigate("about")
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = Color(0xFF2E7D32)
                        )
                        Text("About")
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bouton Logout
            Button(
                onClick = {
                    authViewModel.logout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Logout",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}