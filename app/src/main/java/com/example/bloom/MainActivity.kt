package com.example.bloom

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bloom.screen.MainScreen
import com.example.bloom.ui.theme.BloomTheme
import com.example.bloom.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel

    // Contract pour le résultat de Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
//        authViewModel.handleGoogleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BloomTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Initialiser le ViewModel
                    authViewModel = viewModel()

                    // Initialiser Google Sign-In
                    LaunchedEffect(Unit) {
                        authViewModel.initializeGoogleSignIn(this@MainActivity)
                    }

                    MainScreen(
                        authViewModel = authViewModel,
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