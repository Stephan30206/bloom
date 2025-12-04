package com.example.bloom.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bloom.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _userProfile = MutableLiveData<User>()
    val userProfile: LiveData<User> = _userProfile

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Settings preferences
    private val _notificationsEnabled = MutableLiveData<Boolean>(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _autoSyncEnabled = MutableLiveData<Boolean>(true)
    val autoSyncEnabled: LiveData<Boolean> = _autoSyncEnabled

    private val _selectedLanguage = MutableLiveData<String>("English")
    val selectedLanguage: LiveData<String> = _selectedLanguage

    init {
        loadUserProfile()
    }

    // Load current user profile
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    _userProfile.postValue(
                        User(
                            uid = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName ?: "",
                            photoUrl = user.photoUrl?.toString()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading profile", e)
                _error.postValue("Failed to load profile: ${e.message}")
            }
        }
    }

    // Update user profile (name and photo)
    fun updateProfile(newName: String, photoUrl: String? = null) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                val user = auth.currentUser
                if (user == null) {
                    _error.postValue("User not authenticated")
                    return@launch
                }

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .apply {
                        photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
                    }
                    .build()

                user.updateProfile(profileUpdates).await()

                // Reload profile
                loadUserProfile()
                _successMessage.postValue("Profile updated successfully")
                Log.d("SettingsViewModel", "Profile updated successfully")

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating profile", e)
                _error.postValue("Failed to update profile: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Change password
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            try {
                val user = auth.currentUser
                if (user == null || user.email == null) {
                    _error.postValue("User not authenticated")
                    return@launch
                }

                // Re-authenticate user first
                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(user.email!!, currentPassword)

                user.reauthenticate(credential).await()

                // Update password
                user.updatePassword(newPassword).await()

                _successMessage.postValue("Password changed successfully")
                Log.d("SettingsViewModel", "Password changed successfully")

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error changing password", e)
                when {
                    e.message?.contains("password") == true ->
                        _error.postValue("Current password is incorrect")
                    e.message?.contains("weak") == true ->
                        _error.postValue("New password is too weak (min 6 characters)")
                    else ->
                        _error.postValue("Failed to change password: ${e.message}")
                }
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Toggle notifications
    fun toggleNotifications(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            try {
                _notificationsEnabled.postValue(enabled)

                // Save to SharedPreferences
                val prefs = context.getSharedPreferences("bloom_settings", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("notifications_enabled", enabled).apply()

                Log.d("SettingsViewModel", "Notifications ${if (enabled) "enabled" else "disabled"}")
                _successMessage.postValue("Notifications ${if (enabled) "enabled" else "disabled"}")

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error toggling notifications", e)
                _error.postValue("Failed to update notifications: ${e.message}")
            }
        }
    }

    // Toggle auto-sync
    fun toggleAutoSync(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            try {
                _autoSyncEnabled.postValue(enabled)

                // Save to SharedPreferences
                val prefs = context.getSharedPreferences("bloom_settings", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()

                Log.d("SettingsViewModel", "Auto-sync ${if (enabled) "enabled" else "disabled"}")
                _successMessage.postValue("Auto-sync ${if (enabled) "enabled" else "disabled"}")

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error toggling auto-sync", e)
                _error.postValue("Failed to update auto-sync: ${e.message}")
            }
        }
    }

    // Change language
    fun changeLanguage(language: String, context: Context) {
        viewModelScope.launch {
            try {
                _selectedLanguage.postValue(language)

                // Save to SharedPreferences
                val prefs = context.getSharedPreferences("bloom_settings", Context.MODE_PRIVATE)
                prefs.edit().putString("selected_language", language).apply()

                Log.d("SettingsViewModel", "Language changed to: $language")
                _successMessage.postValue("Language changed to $language")

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error changing language", e)
                _error.postValue("Failed to change language: ${e.message}")
            }
        }
    }

    // Load settings from SharedPreferences
    fun loadSettings(context: Context) {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("bloom_settings", Context.MODE_PRIVATE)

                _notificationsEnabled.postValue(prefs.getBoolean("notifications_enabled", true))
                _autoSyncEnabled.postValue(prefs.getBoolean("auto_sync_enabled", true))
                _selectedLanguage.postValue(prefs.getString("selected_language", "English") ?: "English")

                Log.d("SettingsViewModel", "Settings loaded from preferences")

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading settings", e)
            }
        }
    }

    // Clear cache
    fun clearCache(context: Context) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                // Clear app cache
                context.cacheDir.deleteRecursively()

                // Clear Coil image cache
                coil.ImageLoader(context).memoryCache?.clear()
                coil.ImageLoader(context).diskCache?.clear()

                _successMessage.postValue("Cache cleared successfully")
                Log.d("SettingsViewModel", "Cache cleared")

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error clearing cache", e)
                _error.postValue("Failed to clear cache: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Clear error message
    fun clearError() {
        _error.postValue(null)
    }

    // Clear success message
    fun clearSuccessMessage() {
        _successMessage.postValue(null)
    }
}