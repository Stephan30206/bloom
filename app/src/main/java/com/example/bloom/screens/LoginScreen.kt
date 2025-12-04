package com.example.bloom.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    onGoogleSignInClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by authViewModel.authState.observeAsState()

    // 📱 Obtenir les dimensions de l'écran
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // 📐 Adapter les tailles selon l'écran
    val logoSize = when {
        screenHeight < 600.dp -> 100.dp  // Petit écran
        screenHeight < 800.dp -> 140.dp  // Écran moyen
        else -> 170.dp                    // Grand écran
    }

    val horizontalPadding = when {
        screenWidth < 360.dp -> 16.dp    // Très petit
        screenWidth < 400.dp -> 20.dp    // Petit
        else -> 24.dp                     // Normal et grand
    }

    val spacingSmall = when {
        screenHeight < 600.dp -> 8.dp
        screenHeight < 800.dp -> 12.dp
        else -> 15.dp
    }

    val spacingMedium = when {
        screenHeight < 600.dp -> 12.dp
        screenHeight < 800.dp -> 16.dp
        else -> 20.dp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())  // 🔧 Permet de scroller sur petits écrans
                .padding(horizontal = horizontalPadding)
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {

            // Logo - taille adaptative
            Image(
                painter = painterResource(id = R.drawable.logobloom),
                contentDescription = "Logo",
                modifier = Modifier.size(logoSize)
            )

            Spacer(modifier = Modifier.height(spacingSmall))

            // Header
            Text(
                text = "Sign In",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(spacingMedium))

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

            Spacer(modifier = Modifier.height(10.dp))

            // 🔒 Champ mot de passe
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
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

            // Afficher les erreurs
            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(spacingMedium))

            // 🟩 Bouton de connexion
            Button(
                onClick = {
                    authViewModel.login(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = authState != AuthState.Loading && email.isNotEmpty() && password.isNotEmpty()
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lien vers l'inscription
            TextButton(
                onClick = { navController.navigate(SignUpScreenRoute) }
            ) {
                Text(
                    text = "Don't have an account? Sign Up",
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(spacingMedium))

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

            // 🔧 Espace supplémentaire en bas pour les petits écrans
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text("Login Screen Preview")
    }
}