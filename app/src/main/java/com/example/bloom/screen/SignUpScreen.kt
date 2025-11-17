package com.example.bloom.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.example.bloom.R
import com.example.bloom.viewmodel.AuthState
import com.example.bloom.viewmodel.AuthViewModel
import kotlinx.serialization.Serializable
import androidx.compose.runtime.livedata.observeAsState

@Serializable
object SignUpScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    onGoogleSignInClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    val authState by authViewModel.authState.observeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo_transparent),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(170.dp)
            )

            Spacer(modifier = Modifier.height(35.dp))

            // Header
            Text(
                text = "Sign Up",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 📧 Champ e-mail
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("enter your email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF4CAF50),
                    focusedLabelColor = Color(0xFF4CAF50)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔒 Champ mot de passe
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (confirmPassword.isNotEmpty() && it != confirmPassword) {
                        passwordError = "Passwords don't match"
                    } else {
                        passwordError = ""
                    }
                },
                label = { Text("Password") },
                placeholder = { Text("enter your password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = Color(0xFF4CAF50),
                    focusedLabelColor = Color(0xFF4CAF50)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔒 Champ confirmation mot de passe
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (password != it) {
                        passwordError = "Passwords don't match"
                    } else {
                        passwordError = ""
                    }
                },
                label = { Text("Confirm Password") },
                placeholder = { Text("confirm your password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (passwordError.isEmpty()) Color(0xFF4CAF50) else Color.Red,
                    unfocusedBorderColor = if (passwordError.isEmpty()) Color.Gray else Color.Red,
                    cursorColor = Color(0xFF4CAF50),
                    focusedLabelColor = Color(0xFF4CAF50)
                )
            )

            // Afficher l'erreur de mot de passe
            if (passwordError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = passwordError,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            // Afficher les erreurs d'authentification
            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 🟩 Bouton d'inscription
            Button(
                onClick = {
                    if (password == confirmPassword) {
                        authViewModel.signUp(email, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = authState != AuthState.Loading &&
                        email.isNotEmpty() &&
                        password.isNotEmpty() &&
                        confirmPassword.isNotEmpty() &&
                        password == confirmPassword
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign Up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lien vers la connexion
            TextButton(
                onClick = {
                    navController.navigate(LoginScreenRoute) {
                        popUpTo(SignUpScreenRoute) { inclusive = true }
                    }
                }
            ) {
                Text(
                    text = "Already have an account? Sign In",
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Ligne séparatrice avec "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.Gray,
                    thickness = 1.dp
                )
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.Gray,
                    thickness = 1.dp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 🔘 Connexion Google (FONCTIONNEL)
            OutlinedButton(
                onClick = {
                    onGoogleSignInClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chrome),
                    contentDescription = "Google logo",
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )

                Text(
                    "Continue with Google",
                    fontSize = 16.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Navigation automatique après inscription réussie
    LaunchedEffect(authState) {
        if (authState == AuthState.Authenticated) {
            navController.navigate(PlantListScreenRoute) {
                popUpTo(SignUpScreenRoute) { inclusive = true }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    // Preview sans dépendances
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Sign Up Screen Preview")
    }
}