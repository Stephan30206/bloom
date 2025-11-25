package com.example.bloom.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(
    onBackPressed: () -> Unit = {},
    onUpgradeToPro: () -> Unit = {},
    onChangeLanguage: () -> Unit = {},
    onShare: () -> Unit = {},
    onContact: () -> Unit = {}
) {
    Scaffold(        topBar = {
            TopAppBar(
                title = { Text("Paramètres", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFB2F9A0), Color(0xFF75E6A4))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "Offre limitée rien que pour vous!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        "Touchez pour réclamer maintenant",
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onUpgradeToPro,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Passer à PRO", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Utilisation d'icônes disponibles dans Icons.Default
            SettingItem(
                icon = Icons.Default.Settings,
                title = "Langue",
                trailingText = "Français",
                onClick = onChangeLanguage
            )

            SettingItem(
                icon = Icons.Default.Star,
                title = "Évaluez-nous",
                onClick = {}
            )

            SettingItem(
                icon = Icons.Default.Share,
                title = "Partager l'application",
                onClick = onShare
            )

            SettingItem(
                icon = Icons.Default.Lock,
                title = "Politique de confidentialité",
                onClick = {}
            )

            SettingItem(
                icon = Icons.Default.List,
                title = "Conditions générales",
                onClick = {}
            )

            SettingItem(
                icon = Icons.Default.Email,
                title = "Nous contacter",
                onClick = onContact
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0288D1))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontSize = 16.sp, color = Color.Black, modifier = Modifier.weight(1f))
            if (trailingText != null) {
                Text(trailingText, color = Color(0xFF0288D1))
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingPagePreview() {
    SettingPage()
}