package com.example.bloom.screens

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
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
import kotlinx.coroutines.delay
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

    // ViewModel states
    val isLoading by plantViewModel.isLoading.observeAsState(false)
    val error by plantViewModel.error.observeAsState()
    val currentPlant by plantViewModel.currentPlant.observeAsState()

    // Camera permission
    val cameraPermissionState = rememberPermissionState(
        permission = Manifest.permission.CAMERA
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            capturedImageUri = it
            // Convert URI to Bitmap and identify
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap?.let { bmp ->
                        plantViewModel.identifyAndSavePlant(bmp)
                    } ?: run {
                        plantViewModel.clearError()
                        plantViewModel._error.postValue("Unable to load selected image")
                    }
                }
            } catch (e: Exception) {
                Log.e("Discovery", "Image loading error: ${e.message}")
                plantViewModel.clearError()
                plantViewModel._error.postValue("Error loading image")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedImageUri?.let { uri ->
                // Convert URI to Bitmap and identify
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        bitmap?.let { bmp ->
                            plantViewModel.identifyAndSavePlant(bmp)
                        } ?: run {
                            plantViewModel.clearError()
                            plantViewModel._error.postValue("Unable to load photo")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Discovery", "Photo loading error: ${e.message}")
                    plantViewModel.clearError()
                    plantViewModel._error.postValue("Error loading photo")
                }
            }
        } else {
            plantViewModel.clearError()
            plantViewModel._error.postValue("Photo capture failed")
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
                // Fallback to gallery
                galleryLauncher.launch("image/*")
            }
        } else {
            // Request permission
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        plantViewModel.clearCurrentPlant()
    }

    // Automatic navigation after successful identification
    LaunchedEffect(currentPlant) {
        if (currentPlant != null && !isLoading) {
            Log.d("Discovery", "Identification successful, navigating to list")

            // Wait a bit to show the result
            delay(1000)

            // Go back to previous screen
            navController.popBackStack()

            // IMPORTANT: Reset for next identification
            plantViewModel.clearCurrentPlant()
        }
    }

    // Display errors
    LaunchedEffect(error) {
        error?.let {
            Log.e("Discovery", "Error: $it")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "New Discovery",
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
                            contentDescription = "Back"
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
            // Image preview
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
                            text = "Identifying plant...",
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }
                } else if (capturedImageUri != null) {
                    AsyncImage(
                        model = capturedImageUri,
                        contentDescription = "Captured plant",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Take a photo to identify the plant",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Take Photo button
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
                    text = "Take a Photo",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gallery button
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
                    text = "Choose from Gallery",
                    fontSize = 16.sp,
                    color = Color.Black
                )
            }

            // Display errors - Improved design
            error?.let { errorMessage ->
                Spacer(modifier = Modifier.height(24.dp))

                val (displayMessage, isPlantError) = when {
                    errorMessage.contains("pas une plante", ignoreCase = true) ||
                            errorMessage.contains("not a plant", ignoreCase = true) ||
                            errorMessage.contains("does not contain", ignoreCase = true) ->
                        Pair("This image doesn't appear to contain a plant.\nPlease take a photo of a real plant or flower.", true)

                    errorMessage.contains("Object does not exist", ignoreCase = true) ->
                        Pair("Connection error. Please check your internet.", false)

                    errorMessage.contains("storage", ignoreCase = true) ||
                            errorMessage.contains("upload", ignoreCase = true) ->
                        Pair("Storage problem. Please try again.", false)

                    errorMessage.contains("authentifié", ignoreCase = true) ||
                            errorMessage.contains("authenticated", ignoreCase = true) ->
                        Pair("You must be logged in to identify plants.", false)

                    errorMessage.contains("Impossible", ignoreCase = true) ->
                        Pair("Unable to load the image. Please try again.", false)

                    errorMessage.contains("Erreur", ignoreCase = true) ->
                        Pair("An error occurred. Please try again.", false)

                    else -> Pair(errorMessage, false)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlantError) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isPlantError) Color(0xFFD32F2F) else Color(0xFFF57C00),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlantError) "Not a Plant" else "Error",
                                fontWeight = FontWeight.Bold,
                                color = if (isPlantError) Color(0xFFD32F2F) else Color(0xFFF57C00),
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayMessage,
                                color = Color.DarkGray,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Show retry button only when it's not a plant
                if (isPlantError) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            plantViewModel.clearError()
                            capturedImageUri = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Take Another Photo",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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