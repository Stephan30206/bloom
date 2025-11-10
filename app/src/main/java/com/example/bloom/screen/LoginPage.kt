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
import com.example.bloom.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit = {}) {
    var isSignIn by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // 🌱 Logo vert
            Box(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_transparent),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(170.dp)
                        .padding(end = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(35.dp))

            // 🔁 Header Sign up / Sign in
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x80AAAAAA), RoundedCornerShape(24.dp))
                    .padding(5.dp, 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { isSignIn = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSignIn) Color(0xFF4CAF50) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Text("Sign in", color = if (isSignIn) Color.Black else Color.Gray)
                }

                Button(
                    onClick = { isSignIn = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isSignIn) Color(0xFF4CAF50) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Sign up", color = if (!isSignIn) Color.Black else Color.Gray)
                }
            }

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

            // Confirmez mot de passe (si inscription)
            if (!isSignIn) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    label = { Text("Confirm Password") },
                    placeholder = { Text("confirm your password") },
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
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 🟩 Bouton principal
            Button(
                onClick = {
                    // TODO: Ajouter la logique d'authentification
                    onAuthSuccess() // Navigation vers Discovery
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text(
                    text = if (isSignIn) "Sign In" else "Sign Up",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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

            // 🔘 Connexion Google (fond blanc, bordure grise)
            OutlinedButton(
                onClick = {
                    // TODO: Ajouter la logique Google
                    onAuthSuccess() // Navigation vers Discovery
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
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    AuthScreen()
}