package com.example.bloom.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.bloom.navigation.Screen
import com.example.bloom.viewmodel.AuthViewModel
import com.example.bloom.viewmodel.PlantViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    plantViewModel: PlantViewModel,
    userId: String
) {
    val plants by plantViewModel.plants.observeAsState(emptyList())
    val isLoading by plantViewModel.isLoading.observeAsState(false)
    val error by plantViewModel.error.observeAsState()

    // État pour la recherche
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Filtrer les plantes selon la recherche
    val filteredPlants = remember(plants, searchQuery) {
        if (searchQuery.isBlank()) {
            plants
        } else {
            plants.filter { plant ->
                plant.name.contains(searchQuery, ignoreCase = true) ||
                        plant.summary.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(userId) {
        plantViewModel.loadPlants(userId, forceSync = true)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (!isSearchActive) {
                            Text(
                                text = "My Plant Journal",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        // Bouton de recherche
                        if (!isSearchActive) {
                            IconButton(
                                onClick = { isSearchActive = true }
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )

                // Barre de recherche
                if (isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = {
                                keyboardController?.hide()
                            },
                            onClose = {
                                isSearchActive = false
                                searchQuery = ""
                                keyboardController?.hide()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Bouton de synchronisation à droite de la barre
                        IconButton(
                            onClick = {
                                plantViewModel.syncPlants(userId)
                            },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Sync",
                                tint = if (isLoading) Color.Gray else Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && plants.isEmpty()) {
                // Loader initial
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Synchronizing...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (filteredPlants.isEmpty() && searchQuery.isNotBlank()) {
                // Aucun résultat de recherche
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No plants found",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try a different search term",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else if (plants.isEmpty()) {
                // Aucune plante
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Plants Yet",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the Scan button below to discover your first plant!",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // Liste des plantes
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // En-tête avec compteur
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    "${plants.size} plant${if (plants.size > 1) "s" else ""}"
                                } else {
                                    "${filteredPlants.size} of ${plants.size} plants"
                                },
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    items(filteredPlants) { plant ->
                        PlantCard(
                            plant = plant,
                            onClick = {
                                navController.navigate(Screen.PlantDetail.createRoute(plant.id))
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Indicateur de synchronisation en cours
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(paddingValues),
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            // Afficher les erreurs
            error?.let { errorMessage ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFC62828),
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { plantViewModel.clearError() }
                            ) {
                                Text("✕", fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search plants...") },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Gray
            )
        },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2E7D32),
            unfocusedBorderColor = Color.LightGray,
            cursorColor = Color(0xFF2E7D32)
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        )
    )
}

@Composable
fun PlantCard(plant: com.example.bloom.model.Plant, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            if (plant.imageUrl.isNotEmpty()) {
                SubcomposeAsyncImage(
                    model = plant.imageUrl,
                    contentDescription = plant.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Image not available",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Image", color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = plant.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(plant.timestamp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}