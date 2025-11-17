package com.example.bloom.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.bloom.R
import com.example.bloom.model.Plant
import com.example.bloom.repository.PlantRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantListScreen(
    onBackPressed: () -> Unit = {},
    onSettingsPressed: () -> Unit = {},
    onAddNewPlant: () -> Unit = {}
) {
    val plants = remember { PlantRepository.plants }

    Scaffold(
        containerColor = Color(0xFFF4F4F7), // fond gris clair
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    IconButton(onClick = onSettingsPressed) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewPlant,
                containerColor = Color(0xFF27C76F) // vert rond comme l'image
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Add New Plant"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            items(plants) { plant ->
                PlantItem(plant = plant)
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
fun PlantItem(plant: Plant) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),  // plus grand comme l'image
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {

        Column {

            // IMAGE EN HAUT
            if (!plant.imageUri.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = plant.imageUri),
                    contentDescription = plant.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.selection),
                        contentDescription = "Plant",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp)
                            .clip(RoundedCornerShape(26.dp)),   // ⭐ arrondi correct
                        contentScale = ContentScale.Crop
                    )

                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TEXTE EN BAS
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {

                Text(
                    text = plant.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EnergySavingsLeaf,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = plant.date,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PlantListScreenPreview() {
    PlantListScreen()
}

@Preview(showBackground = true)
@Composable
fun PlantItemPreview() {
    PlantItem(plant = Plant("Monstra Deliciosa", "10/06/2023"))
}