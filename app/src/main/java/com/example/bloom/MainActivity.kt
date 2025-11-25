package com.example.bloom

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModelProvider
import com.example.bloom.database.PlantDatabase
import com.example.bloom.repository.PlantRepository
import com.example.bloom.screens.MainScreen
import com.example.bloom.service.GeminiAIService
import com.example.bloom.ui.theme.BloomTheme
import com.example.bloom.viewmodel.AuthViewModel
import com.example.bloom.viewmodel.PlantViewModel
import com.example.bloom.viewmodel.PlantViewModelFactory

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var plantViewModel: PlantViewModel

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModel.handleGoogleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        // Initialisation du ViewModel avec Factory
        val plantDatabase = PlantDatabase.getInstance(this)
        val plantRepository = PlantRepository(plantDatabase.plantDao())
        val geminiAIService = GeminiAIService()

        plantViewModel = ViewModelProvider(
            this,
            PlantViewModelFactory(plantRepository, geminiAIService)
        )[PlantViewModel::class.java]

        // Initialiser Google Sign-In
        authViewModel.initializeGoogleSignIn(this)

        setContent {
            BloomTheme {
                Surface {
                    MainScreen(
                        authViewModel = authViewModel,
                        plantViewModel = plantViewModel,
                        onGoogleSignInClick = {
                            val signInIntent = authViewModel.getGoogleSignInIntent()
                            googleSignInLauncher.launch(signInIntent)
                        }
                    )
                }
            }
        }
    }
}