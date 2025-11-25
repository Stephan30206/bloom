package com.example.bloom.viewmodel

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String) : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}