package com.example.bloom.viewmodel

import android.content.Context
import android.content.Intent
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

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _authState = MutableLiveData<AuthState>(AuthState.Unauthenticated)
    val authState: LiveData<AuthState> = _authState

    private lateinit var googleSignInClient: GoogleSignInClient

    init {
        checkAuthStatus()
    }

    fun initializeGoogleSignIn(context: Context) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getWebClientId(context))
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(context, gso)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getWebClientId(context: Context): String {
        return try {
            val resourceId = context.resources.getIdentifier(
                "default_web_client_id", "string", context.packageName
            )
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                _authState.value = AuthState.Error("Google Sign-In failed: ${e.message}")
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign-In error: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            _authState.postValue(AuthState.Authenticated(user?.uid ?: ""))
                        } else {
                            _authState.postValue(AuthState.Error(
                                task.exception?.message ?: "Google Sign-In failed"
                            ))
                        }
                    }
            } catch (e: Exception) {
                _authState.postValue(AuthState.Error("Firebase auth error: ${e.message}"))
            }
        }
    }

    private fun checkAuthStatus() {
        val user = auth.currentUser
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
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        _authState.postValue(AuthState.Authenticated(user?.uid ?: ""))
                    } else {
                        _authState.postValue(AuthState.Error(
                            task.exception?.message ?: "Sign up failed"
                        ))
                    }
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
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        _authState.postValue(AuthState.Authenticated(user?.uid ?: ""))
                    } else {
                        _authState.postValue(AuthState.Error(
                            task.exception?.message ?: "Login failed"
                        ))
                    }
                }
        }
    }

    fun logout() {
        auth.signOut()
        if (::googleSignInClient.isInitialized) {
            googleSignInClient.signOut()
        }
        _authState.value = AuthState.Unauthenticated
    }

    fun clearError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Unauthenticated
        }
    }
}