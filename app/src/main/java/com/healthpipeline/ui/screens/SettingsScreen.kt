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
fun SettingsScreen(modifier: Modifier = Modifier) {
    var autoSync by remember { mutableStateOf(true) }
    var syncFrequency by remember { mutableStateOf("Hourly") }
    var debugLogging by remember { mutableStateOf(false) }
    
    var notificationCompletion by remember { mutableStateOf(true) }
    var notificationErrors by remember { mutableStateOf(true) }
    var notificationSummaries by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Configure your pipeline and account settings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Account Settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Account Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Connected Account: john.doe@gmail.com",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Last Sync: 2024-11-06 07:42:12",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Data Access: ✅ Steps ✅ Heart Rate ✅ Sleep",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Refresh token */ }
                    ) {
                        Text("🔄 Refresh Token")
                    }
                    OutlinedButton(
                        onClick = { /* TODO: Reconnect account */ }
                    ) {
                        Text("🔗 Reconnect")
                    }
                }
            }
        }
        
        // Pipeline Configuration
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pipeline Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Auto Sync Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Sync", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { autoSync = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Sync Frequency
                Text(
                    text = "Sync Frequency",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val frequencies = listOf("Real-time", "Hourly", "Daily", "Weekly")
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = syncFrequency,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        frequencies.forEach { frequency ->
                            DropdownMenuItem(
                                text = { Text(frequency) },
                                onClick = {
                                    syncFrequency = frequency
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Configuration Options
                OutlinedTextField(
                    value = "1000",
                    onValueChange = { },
                    label = { Text("Batch Size") },
                    suffix = { Text("records") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = "all-MiniLM-L6-v2",
                    onValueChange = { },
                    label = { Text("Embedding Model") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = "90 days",
                    onValueChange = { },
                    label = { Text("Data Retention") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Notifications
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Notification toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pipeline completion", style = MaterialTheme.typography.bodyMedium)
                    Checkbox(
                        checked = notificationCompletion,
                        onCheckedChange = { notificationCompletion = it }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Errors and failures", style = MaterialTheme.typography.bodyMedium)
                    Checkbox(
                        checked = notificationErrors,
                        onCheckedChange = { notificationErrors = it }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Daily summaries", style = MaterialTheme.typography.bodyMedium)
                    Checkbox(
                        checked = notificationSummaries,
                        onCheckedChange = { notificationSummaries = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = "john.doe@gmail.com",
                    onValueChange = { },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { /* TODO: Edit email */ }) {
                            Text("Edit")
                        }
                    }
                )
            }
        }
        
        // Advanced Settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Advanced Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = "100",
                        onValueChange = { },
                        label = { Text("API Rate Limit") },
                        suffix = { Text("req/min") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = "30",
                        onValueChange = { },
                        label = { Text("Timeout") },
                        suffix = { Text("sec") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = "3",
                    onValueChange = { },
                    label = { Text("Retry Attempts") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Debug Logging", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = debugLogging,
                        onCheckedChange = { debugLogging = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dangerous actions
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "⚠️ Dangerous Actions",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* TODO: Clear data */ },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("🗑️ Clear Data")
                            }
                            OutlinedButton(
                                onClick = { /* TODO: Export settings */ }
                            ) {
                                Text("📥 Export Settings")
                            }
                        }
                    }
                }
            }
        }
        
        // Save/Reset Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* TODO: Save settings */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("💾 Save Changes")
            }
            OutlinedButton(
                onClick = { /* TODO: Reset settings */ },
                modifier = Modifier.weight(1f)
            ) {
                Text("↩️ Reset")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HealthPipelineTheme {
        SettingsScreen()
    }
}
