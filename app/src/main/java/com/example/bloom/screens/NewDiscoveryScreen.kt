package com.example.bloom.screens

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.bloom.viewmodel.PlantViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewDiscoveryScreen(
    navController: NavHostController,
    plantViewModel: PlantViewModel
) {
    val context = LocalContext.current
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    // États du ViewModel
    val isLoading by plantViewModel.isLoading.observeAsState(false)
    val error by plantViewModel.error.observeAsState()
    val currentPlant by plantViewModel.currentPlant.observeAsState()

    // Permission caméra
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            capturedImageUri = it
            // Convertir URI en Bitmap et identifier
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap?.let { bmp ->
                        plantViewModel.identifyAndSavePlant(bmp)
                    } ?: run {
                        plantViewModel.clearError()
                        plantViewModel._error.postValue("Impossible de charger l'image sélectionnée")
                    }
                }
            } catch (e: Exception) {
                Log.e("Discovery", "Erreur chargement image: ${e.message}")
                plantViewModel.clearError()
                plantViewModel._error.postValue("Erreur lors du chargement de l'image")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedImageUri?.let { uri ->
                // Convertir URI en Bitmap et identifier
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        bitmap?.let { bmp ->
                            plantViewModel.identifyAndSavePlant(bmp)
                        } ?: run {
                            plantViewModel.clearError()
                            plantViewModel._error.postValue("Impossible de charger la photo")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Discovery", "Erreur chargement photo: ${e.message}")
                    plantViewModel.clearError()
                    plantViewModel._error.postValue("Erreur lors du chargement de la photo")
                }
            }
        } else {
            plantViewModel.clearError()
            plantViewModel._error.postValue("Échec de la prise de photo")
        }
    }

    val takePhoto = {
        if (cameraPermissionState.status.isGranted) {
            try {
                val photoFile = createImageFile(context)
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                capturedImageUri = photoUri
                cameraLauncher.launch(photoUri)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback vers la galerie
                galleryLauncher.launch("image/*")
            }
        } else {
            // Demander la permission
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Navigation automatique après identification réussie
    LaunchedEffect(currentPlant) {
        if (currentPlant != null && !isLoading) {
            Log.d("Discovery", "Identification réussie, navigation vers la liste")
            // Retour à l'écran précédent
            navController.popBackStack()
        }
    }

    // Afficher les erreurs
    LaunchedEffect(error) {
        error?.let {
            Log.e("Discovery", "Erreur: $it")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nouvelle Découverte",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isLoading) navController.popBackStack()
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Aperçu de l'image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Identification en cours...",
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }
                } else if (capturedImageUri != null) {
                    AsyncImage(
                        model = capturedImageUri,
                        contentDescription = "Plante capturée",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Prenez une photo pour identifier la plante",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Bouton Prendre une photo
            Button(
                onClick = takePhoto,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Prendre une Photo",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bouton Galerie
            OutlinedButton(
                onClick = {
                    if (!isLoading) galleryLauncher.launch("image/*")
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Choisir depuis la Galerie",
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }

            // Afficher les erreurs
            error?.let { errorMessage ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = when {
                        errorMessage.contains("Object does not exist") ->
                            "Erreur de connexion. Vérifiez votre internet."
                        errorMessage.contains("storage") || errorMessage.contains("upload") ->
                            "Problème de sauvegarde. Réessayez."
                        errorMessage.contains("authentifié") ->
                            "Vous devez être connecté pour identifier des plantes"
                        else -> errorMessage
                    },
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir("Pictures")
    return File.createTempFile(
        "JPEG_${timeStamp}_",
        ".jpg",
        storageDir
    )
}