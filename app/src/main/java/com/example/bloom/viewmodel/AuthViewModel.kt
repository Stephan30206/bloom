package com.example.bloom.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private lateinit var googleSignInClient: GoogleSignInClient

    init {
        // Vérifier immédiatement l'état d'authentification
        checkAuthStatus()

        // Écouter les changements d'état d'authentification
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.postValue(AuthState.Authenticated)
            } else {
                _authState.postValue(AuthState.Unauthenticated)
            }
        }
    }

    fun initializeGoogleSignIn(context: Context) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getWebClientId(context))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    private fun getWebClientId(context: Context): String {
        // Méthode 1: Récupérer depuis les ressources string
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        )

        return if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            // Méthode 2: Utiliser une valeur par défaut (remplacez par votre Web Client ID)
            "103953800507-e5gq0qru2vq1k4tq9q9q9q9q9q9q9q9q.apps.googleusercontent.com"
        }
    }

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            _authState.postValue(AuthState.Error("Google Sign-In failed: ${e.statusCode}"))
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        _authState.postValue(AuthState.Loading)

        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.postValue(AuthState.Authenticated)
                } else {
                    _authState.postValue(
                        AuthState.Error(
                            task.exception?.message ?: "Google Sign-In failed"
                        )
                    )
                }
            }
    }

    fun checkAuthStatus() {
        val user = auth.currentUser
        if (user == null) {
            _authState.postValue(AuthState.Authenticated)
        } else {
            _authState.postValue(AuthState.Unauthenticated)
        }
    }

    fun signUp(email: String, password: String) {
        _authState.postValue(AuthState.Loading)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.postValue(AuthState.Authenticated)
                } else {
                    _authState.postValue(
                        AuthState.Error(
                            task.exception?.message ?: "Something went wrong"
                        )
                    )
                }
            }
    }

    fun login(email: String, password: String) {
        _authState.postValue(AuthState.Loading)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.postValue(AuthState.Authenticated)
                } else {
                    _authState.postValue(
                        AuthState.Error(
                            task.exception?.message ?: "Something went wrong"
                        )
                    )
                }
            }
    }

    fun logout() {
        auth.signOut()
        googleSignInClient.signOut() // Déconnexion Google aussi
        _authState.postValue(AuthState.Unauthenticated)
    }
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}