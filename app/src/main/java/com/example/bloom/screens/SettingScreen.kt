package com.example.bloom.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bloom.viewmodel.AuthViewModel
import com.example.bloom.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val userProfile by settingsViewModel.userProfile.observeAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.observeAsState(true)
    val autoSyncEnabled by settingsViewModel.autoSyncEnabled.observeAsState(true)
    val selectedLanguage by settingsViewModel.selectedLanguage.observeAsState("English")
    val error by settingsViewModel.error.observeAsState()
    val successMessage by settingsViewModel.successMessage.observeAsState()
    val isLoading by settingsViewModel.isLoading.observeAsState(false)

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    // Load settings on start
    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            // Show error/success messages
            error?.let {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { settingsViewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(it)
                }
            }
            successMessage?.let {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = Color(0xFF2E7D32),
                    action = {
                        TextButton(onClick = { settingsViewModel.clearSuccessMessage() }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(it, color = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Account
            Text(
                text = "ACCOUNT",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            SettingsCard {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Edit Profile",
                    subtitle = userProfile?.name?.ifEmpty { "Set your name" } ?: "Loading...",
                    onClick = { showEditProfileDialog = true }
                )
                Divider()
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    subtitle = "Update your password",
                    onClick = { showChangePasswordDialog = true }
                )
            }

            // Section: Preferences
            Text(
                text = "PREFERENCES",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            SettingsCard {
                SettingsItemWithSwitch(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Receive plant care reminders",
                    checked = notificationsEnabled,
                    onCheckedChange = {
                        settingsViewModel.toggleNotifications(it, context)
                    }
                )
                Divider()
                SettingsItemWithSwitch(
                    icon = Icons.Default.Sync,
                    title = "Auto-Sync",
                    subtitle = "Sync plants automatically",
                    checked = autoSyncEnabled,
                    onCheckedChange = {
                        settingsViewModel.toggleAutoSync(it, context)
                    }
                )
                Divider()
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = selectedLanguage,
                    onClick = { showLanguageDialog = true }
                )
            }

            // Section: Data
            Text(
                text = "DATA",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            SettingsCard {
                Divider()
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Clear Cache",
                    subtitle = "Free up storage space",
                    onClick = { showClearCacheDialog = true },
                    iconTint = Color(0xFFD32F2F)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2E7D32))
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userProfile?.displayName ?: "",
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { newName ->
                settingsViewModel.updateProfile(newName)
                showEditProfileDialog = false
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                settingsViewModel.changePassword(currentPassword, newPassword)
                showChangePasswordDialog = false
            }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = selectedLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = { language ->
                settingsViewModel.changeLanguage(language, context)
                showLanguageDialog = false
            }
        )
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Cache",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Clear Cache?") },
            text = { Text("This will free up storage space by removing temporary files and cached images.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsViewModel.clearCache(context)
                        showClearCacheDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Edit Profile Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                Text("Enter your display name", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Change Password Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        passwordError = if (it.length < 6) "Min 6 characters" else ""
                    },
                    label = { Text("New Password") },
                    singleLine = true,
                    isError = passwordError.isNotEmpty(),
                    supportingText = if (passwordError.isNotEmpty()) {
                        { Text(passwordError) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        passwordError = if (it != newPassword) "Passwords don't match" else ""
                    },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentPassword, newPassword) },
                enabled = currentPassword.isNotBlank() &&
                        newPassword.length >= 6 &&
                        newPassword == confirmPassword
            ) {
                Text("Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Language Selection Dialog
@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf(
        "English" to "🇬🇧"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                languages.forEach { (language, flag) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(language) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onSelect(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(flag, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(language)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = Color(0xFF2E7D32)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}

@Composable
fun SettingsItemWithSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2E7D32),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}