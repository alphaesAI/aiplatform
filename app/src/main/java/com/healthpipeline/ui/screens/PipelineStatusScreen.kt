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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineStatusScreen(modifier: Modifier = Modifier) {
    var isRunning by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0.75f) }
    var currentStage by remember { mutableStateOf("Data Transformation") }
    var recordsProcessed by remember { mutableStateOf(1847) }
    var totalRecords by remember { mutableStateOf(2450) }
    
    // Simulate progress updates
    LaunchedEffect(isRunning) {
        while (isRunning && currentProgress < 1.0f) {
            delay(2000)
            currentProgress = (currentProgress + 0.01f).coerceAtMost(1.0f)
            recordsProcessed = (recordsProcessed + 25).coerceAtMost(totalRecords)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pipeline Status",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            AssistChip(
                onClick = { },
                label = { 
                    Text(if (isRunning) "RUNNING" else "PAUSED")
                },
                leadingIcon = {
                    Text(if (isRunning) "🟢" else "🟡")
                }
            )
        }
        
        // Current Pipeline Info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Pipeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Job ID: pipeline_20241106_074212", style = MaterialTheme.typography.bodySmall)
                Text("Started: 2024-11-06 07:42:12", style = MaterialTheme.typography.bodySmall)
                Text("Duration: 00:15:23", style = MaterialTheme.typography.bodySmall)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Overall Progress: ${(currentProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    progress = { currentProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                
                Text(
                    text = "Current Stage: $currentStage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Control buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isRunning = !isRunning }
                    ) {
                        Text(if (isRunning) "⏸️ Pause" else "▶️ Resume")
                    }
                    OutlinedButton(
                        onClick = { /* TODO: Stop pipeline */ }
                    ) {
                        Text("⏹️ Stop")
                    }
                    OutlinedButton(
                        onClick = { /* TODO: View logs */ }
                    ) {
                        Text("📋 Logs")
                    }
                }
            }
        }
        
        // Stage Progress
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Stage Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val stages = listOf(
                    "Authentication" to "✅ Completed - 00:00:05",
                    "Data Extraction" to "✅ Completed - 00:01:30",
                    "Data Transformation" to "🔄 In Progress - 00:02:10",
                    "Vector Storage" to "⏳ Pending",
                    "Index Creation" to "⏳ Pending"
                )
                
                stages.forEach { (stage, status) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stage,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Processing Statistics
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Processing Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = recordsProcessed.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Records Processed",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalRecords.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Total Records",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "12.3/sec",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Processing Rate",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "245 MB",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Memory Usage",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🟢", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Elasticsearch Status: Connected",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Recent Logs
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Recent Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val logs = listOf(
                    "[07:44:45] INFO: Processing heart rate data batch 15",
                    "[07:44:42] INFO: Generated embeddings for 50 records",
                    "[07:44:38] INFO: Transforming sleep data to text",
                    "[07:44:35] WARN: Rate limit approaching, slowing down",
                    "[07:44:30] INFO: Successfully extracted 2,450 records"
                )
                
                logs.forEach { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { /* TODO: View full logs */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Full Logs")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PipelineStatusScreenPreview() {
    HealthPipelineTheme {
        PipelineStatusScreen()
    }
}
