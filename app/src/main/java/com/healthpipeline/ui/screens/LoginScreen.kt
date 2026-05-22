package com.healthpipeline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthpipeline.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onSignInClick: (String, String) -> Unit,
    onSignUpClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val backgroundColorTop = Color(0xFF20232B)
    val backgroundColorBottom = Color(0xFF111216)
    val cardColor = Color.White.copy(alpha = 0.05f)
    val accentColor = Color(0xFFFF6B6B) // A slightly more coral-pink to match the glow

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(backgroundColorTop, backgroundColorBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Branding
            Text(
                text = "PHIA",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 6.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Welcome Back.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Glassmorphism Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email Address", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = accentColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Password", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = accentColor) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.03f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Sign In Button
                    Button(
                        onClick = { onSignInClick(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { /* Handle Forgot Password */ }) {
                        Text("Forgot Password?", color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign Up Option
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("New to PHIA?", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = onSignUpClick) {
                    Text("Create Account", color = accentColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(color = accentColor, modifier = Modifier.padding(16.dp))
            }
            
            uiState.error?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(8.dp))
            }
        }
    }
}
