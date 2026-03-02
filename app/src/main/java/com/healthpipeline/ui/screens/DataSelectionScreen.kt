package com.healthpipeline.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.healthpipeline.ui.theme.HealthPipelineTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSelectionScreen(modifier: Modifier = Modifier) {
    var selectedDataTypes by remember { 
        mutableStateOf(
            mapOf(
                "steps" to true,
                "heartRate" to true,
                "sleep" to true,
                "calories" to false,
                "distance" to false,
                "exercise" to false
            )
        )
    }
    
    var selectedDateRange by remember { mutableStateOf("Last 7 days") }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Data Selection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Choose the health data types and date range for extraction",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Data Types Selection
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Types",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Data type checkboxes
                val dataTypes = listOf(
                    "steps" to "🚶 Steps Data",
                    "heartRate" to "❤️ Heart Rate",
                    "sleep" to "😴 Sleep Data",
                    "calories" to "🔥 Calories Burned",
                    "distance" to "📏 Distance Traveled",
                    "exercise" to "🏃 Exercise Sessions"
                )
                
                dataTypes.forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedDataTypes[key] ?: false,
                            onCheckedChange = { checked ->
                                selectedDataTypes = selectedDataTypes.toMutableMap().apply {
                                    this[key] = checked
                                }
                            }
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedDataTypes = selectedDataTypes.mapValues { true }
                        }
                    ) {
                        Text("Select All")
                    }
                    OutlinedButton(
                        onClick = {
                            selectedDataTypes = selectedDataTypes.mapValues { false }
                        }
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }
        
        // Date Range Selection
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quick select options
                Text(
                    text = "Quick Select:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dateOptions = listOf("Last 7 days", "Last 30 days", "Last 3 months")
                    dateOptions.forEach { option ->
                        FilterChip(
                            onClick = { selectedDateRange = option },
                            label = { Text(option) },
                            selected = selectedDateRange == option
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom date range (placeholder)
                OutlinedButton(
                    onClick = { /* TODO: Open date picker */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Custom Date Range")
                }
            }
        }
        
        // Data Preview
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val selectedCount = selectedDataTypes.values.count { it }
                val estimatedRecords = when (selectedDateRange) {
                    "Last 7 days" -> selectedCount * 7 * 50
                    "Last 30 days" -> selectedCount * 30 * 50
                    "Last 3 months" -> selectedCount * 90 * 50
                    else -> 0
                }
                
                Text(
                    text = "Estimated Records: ~${estimatedRecords.toString().replace(Regex("(\\d)(?=(\\d{3})+(?!\\d))"), "$1,")}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Data Types Selected: $selectedCount",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Date Range: $selectedDateRange",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Estimated Processing Time: ~${(estimatedRecords / 500).coerceAtLeast(1)} minutes",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (selectedCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Sample Data Preview:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (selectedDataTypes["steps"] == true) {
                        Text("• Steps: 8,245 (Today)", style = MaterialTheme.typography.bodySmall)
                    }
                    if (selectedDataTypes["heartRate"] == true) {
                        Text("• Heart Rate: 72 bpm avg (Today)", style = MaterialTheme.typography.bodySmall)
                    }
                    if (selectedDataTypes["sleep"] == true) {
                        Text("• Sleep: 7h 32m (Last Night)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { /* TODO: Navigate back */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = { /* TODO: Refresh preview */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh")
            }
        }
        
        val selectedCount = selectedDataTypes.size
        
        Button(
            onClick = { /* TODO: Start pipeline */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCount > 0
        ) {
            Text("Start Pipeline")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DataSelectionScreenPreview() {
    HealthPipelineTheme {
        DataSelectionScreen()
    }
}
