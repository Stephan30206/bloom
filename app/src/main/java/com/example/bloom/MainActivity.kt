package com.example.bloom

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.bloom.database.PlantDatabase
import com.example.bloom.repository.PlantRepository
import com.example.bloom.screens.MainScreen
import com.example.bloom.service.GeminiAIService
import com.example.bloom.service.SupabasePlantService
import com.example.bloom.ui.theme.BloomTheme
import com.example.bloom.viewmodel.AuthViewModel
import com.example.bloom.viewmodel.PlantViewModel
import com.example.bloom.viewmodel.PlantViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private var plantViewModel: PlantViewModel? = null
    private val firebaseAuth = FirebaseAuth.getInstance()

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

        // CRITICAL: Set content IMMEDIATELY with minimal UI
        setContent {
            BloomTheme {
                Surface {
                    MinimalStartupUI()
                }
            }
        }

        // Initialize AFTER UI is rendered
        lifecycleScope.launch {
            initializeAppAsync()
        }
    }

    @Composable
    private fun MinimalStartupUI() {
        var isReady by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            // Wait for initialization to complete
            while (plantViewModel == null) {
                delay(50)
            }
            isReady = true
        }

        if (isReady && plantViewModel != null) {
            MainScreen(
                authViewModel = authViewModel,
                plantViewModel = plantViewModel!!,
                onGoogleSignInClick = { handleGoogleSignIn() }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    /**
     * Initialize EVERYTHING efficiently
     * Heavy I/O on background, lifecycle-dependent on main
     */
    private suspend fun initializeAppAsync() {
        try {
            Log.d("MainActivity", "🚀 Start async init")

            // Google Sign-In (lightweight, must be on main)
            withContext(Dispatchers.Main) {
                authViewModel.initializeGoogleSignIn(this@MainActivity)
            }

            // Heavy I/O operations in parallel using coroutineScope
            coroutineScope {
                val dbDeferred = async(Dispatchers.IO) {
                    PlantDatabase.getInstance(applicationContext)
                }

                // CRITICAL: Supabase MUST be initialized on Main thread
                // because it registers lifecycle observers
                val supabaseDeferred = async(Dispatchers.Main) {
                    SupabaseClient.client
                }

                // Wait for both
                val plantDatabase = dbDeferred.await()
                val supabaseClient = supabaseDeferred.await()

                // Create services
                val plantDao = plantDatabase.plantDao()
                val supabasePlantService = SupabasePlantService(supabaseClient)
                val plantRepository = PlantRepository(plantDao, supabasePlantService)
                val geminiAIService = GeminiAIService()

                // Create ViewModel on main thread
                withContext(Dispatchers.Main) {
                    plantViewModel = ViewModelProvider(
                        this@MainActivity,
                        PlantViewModelFactory(plantRepository, geminiAIService)
                    )[PlantViewModel::class.java]

                    // 🔧 CORRECTION: Setup Firebase listener avec synchronisation
                    firebaseAuth.addAuthStateListener { auth ->
                        auth.currentUser?.let { user ->
                            Log.d("MainActivity", "🔐 User logged in: ${user.uid}")

                            // ✅ Synchroniser automatiquement au login
                            lifecycleScope.launch(Dispatchers.IO) {
                                Log.d("MainActivity", "🔄 Auto-syncing on login...")
                                plantViewModel?.syncPlants(user.uid)
                            }
                        }
                    }
                }
            }

            Log.d("MainActivity", "✅ Init complete")

        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Init failed", e)
            withContext(Dispatchers.Main) {
                // Show error to user if needed
            }
        }
    }

    private fun handleGoogleSignIn() {
        try {
            val signInIntent = authViewModel.getGoogleSignInIntent()
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Google Sign-In error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up when activity is destroyed
        plantViewModel = null
    }
}