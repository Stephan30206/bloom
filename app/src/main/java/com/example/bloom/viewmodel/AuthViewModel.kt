package com.example.bloom.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _authState = MutableLiveData<AuthState>(AuthState.Unauthenticated)
    val authState: LiveData<AuthState> = _authState

    private var googleSignInClient: GoogleSignInClient? = null
    private var isGoogleSignInInitialized = false

    // Email de l'utilisateur connecté
    private val _currentUserEmail = MutableLiveData<String?>()
    val currentUserEmail: LiveData<String?> = _currentUserEmail

    // État de vérification de l'email
    private val _isEmailVerified = MutableLiveData<Boolean>()
    val isEmailVerified: LiveData<Boolean> = _isEmailVerified

    init {
        checkAuthStatus()

        // Ajouter un listener pour les changements d'authentification
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            updateUserInfo(user)
            _authState.value = if (user != null) {
                AuthState.Authenticated(user.uid)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    // Fonction pour mettre à jour les informations utilisateur
    private fun updateUserInfo(user: com.google.firebase.auth.FirebaseUser?) {
        _currentUserEmail.value = user?.email
        _isEmailVerified.value = user?.isEmailVerified ?: false

        // Log pour debug
        Log.d("AuthViewModel", "User email: ${user?.email ?: "null"}, verified: ${user?.isEmailVerified ?: false}")
    }

    fun initializeGoogleSignIn(context: Context) {
        try {
            Log.d("AuthViewModel", "Initializing Google Sign-In")

            // Option 1: Client ID depuis google-services.json
            val clientId = try {
                // Cherche le web client ID dans google-services.json
                context.getString(
                    context.resources.getIdentifier(
                        "default_web_client_id",
                        "string",
                        context.packageName
                    )
                )
            } catch (e: Exception) {
                // Option 2: Client ID de debug
                Log.w("AuthViewModel", "Using debug client ID")
                "639817168316-vlvrscdtiqd4k7g88p25a3ct7qaq5m4d.apps.googleusercontent.com"
            }

            Log.d("AuthViewModel", "Client ID: ${clientId.take(20)}...")

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
            isGoogleSignInInitialized = true
            Log.d("AuthViewModel", "Google Sign-In initialized successfully")

        } catch (e: Exception) {
            Log.e("AuthViewModel", "Google Sign-In initialization failed", e)
            _authState.value = AuthState.Error("Google Sign-In initialization failed: ${e.message}")
        }
    }

    fun getGoogleSignInIntent(): Intent {
        if (!isGoogleSignInInitialized || googleSignInClient == null) {
            throw IllegalStateException(
                "GoogleSignInClient not initialized. Call initializeGoogleSignIn() first."
            )
        }
        return googleSignInClient!!.signInIntent
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                Log.d("AuthViewModel", "Handling Google Sign-In result")

                if (data == null) {
                    _authState.value = AuthState.Error("No data received from Google Sign-In")
                    return@launch
                }

                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)

                if (account == null) {
                    _authState.value = AuthState.Error("Google account is null")
                    return@launch
                }

                Log.d("AuthViewModel", "Google account received: ${account.email}")
                firebaseAuthWithGoogle(account)

            } catch (e: ApiException) {
                Log.e("AuthViewModel", "Google Sign-In API Exception", e)
                _authState.value = AuthState.Error("Google Sign-In failed: ${e.statusCode} - ${e.message}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In error", e)
                _authState.value = AuthState.Error("Sign-In error: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting Firebase auth with Google")

                if (account?.idToken == null) {
                    _authState.value = AuthState.Error("Google ID token is null")
                    return@launch
                }

                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val authResult = auth.signInWithCredential(credential).await()

                val user = authResult.user
                if (user != null) {
                    Log.d("AuthViewModel", "Firebase auth successful: ${user.uid}")
                    // Mettre à jour les infos utilisateur
                    updateUserInfo(user)
                    _authState.value = AuthState.Authenticated(user.uid)
                } else {
                    _authState.value = AuthState.Error("Firebase user is null")
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Firebase auth error", e)
                _authState.value = AuthState.Error("Firebase auth error: ${e.message}")
            }
        }
    }

    private fun checkAuthStatus() {
        val user = auth.currentUser
        updateUserInfo(user)
        _authState.value = if (user != null) {
            AuthState.Authenticated(user.uid)
        } else {
            AuthState.Unauthenticated
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                updateUserInfo(user)
                _authState.value = AuthState.Authenticated(user?.uid ?: "")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign up failed: ${e.message}")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user
                updateUserInfo(user)
                _authState.value = AuthState.Authenticated(user?.uid ?: "")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun logout() {
        try {
            auth.signOut()
            googleSignInClient?.signOut()
            _authState.value = AuthState.Unauthenticated
            // Réinitialiser les infos utilisateur
            _currentUserEmail.value = null
            _isEmailVerified.value = false
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Logout error", e)
        }
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }

    // Fonction pour rafraîchir les informations utilisateur (utile après vérification d'email)
    fun refreshUserInfo() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateUserInfo(auth.currentUser)
                Log.d("AuthViewModel", "User info refreshed. Verified: ${auth.currentUser?.isEmailVerified}")
            }
        }
    }
}