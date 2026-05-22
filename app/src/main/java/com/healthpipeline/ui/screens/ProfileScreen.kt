package com.healthpipeline.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.healthpipeline.viewmodels.HealthDataViewModel

@Composable
fun ProfileScreen(
    viewModel: HealthDataViewModel,
    modifier: Modifier = Modifier
) {
    val healthData by viewModel.healthData.collectAsState()
    val bio = healthData.biometrics
    
    // Edit mode state
    var isEditing by remember { mutableStateOf(false) }
    var editAge by remember { mutableStateOf(bio.age?.toString() ?: "") }
    var editGender by remember { mutableStateOf(bio.gender ?: "") }

    val backgroundColorTop = Color(0xFF20232B)
    val backgroundColorBottom = Color(0xFF111216)
    val cardColor = Color.White.copy(alpha = 0.05f)
    val accentColor = Color(0xFFFF6B6B)

    Box(
        modifier = modifier
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Biometrics",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // User Info Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    if (isEditing) {
                        // Edit Mode
                        Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = editAge,
                            onValueChange = { editAge = it.filter { c -> c.isDigit() } },
                            label = { Text("Age (years)", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = editGender,
                            onValueChange = { editGender = it },
                            label = { Text("Gender", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { isEditing = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    viewModel.updateBiometrics(
                                        age = editAge.toIntOrNull(),
                                        gender = editGender.takeIf { it.isNotBlank() }
                                    )
                                    isEditing = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Save")
                            }
                        }
                    } else {
                        // View Mode
                        ProfileMetricRow("Age", bio.age?.let { "${it} years" } ?: "--", "👤")
                        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileMetricRow("Gender", bio.gender ?: "--", "🚻")
                        Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileMetricRow("System ID", healthData.exerciseSessionsList.firstOrNull()?.pseudoId2 ?: "VITAL-2026", "🆔")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { 
                                isEditing = true 
                                editAge = bio.age?.toString() ?: ""
                                editGender = bio.gender ?: ""
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("✏️ Edit", color = accentColor)
                        }
                    }
                }
            }

            Text(
                text = "Physical Stats",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BiometricGlassCard(
                    modifier = Modifier.weight(1f),
                    label = "Weight",
                    value = bio.weightKg?.let { String.format("%.1f", it) } ?: "--",
                    unit = "kg",
                    icon = "⚖️",
                    color = Color(0xFF4DFFB8),
                    cardColor = cardColor
                )
                BiometricGlassCard(
                    modifier = Modifier.weight(1f),
                    label = "Height",
                    value = bio.heightCm?.let { String.format("%.0f", it) } ?: "--",
                    unit = "cm",
                    icon = "📏",
                    color = Color(0xFF6B8AFF),
                    cardColor = cardColor
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ProfileMetricRow(label: String, value: String, icon: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color.Gray, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BiometricGlassCard(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    icon: String,
    color: Color,
    cardColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "$label ($unit)", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
