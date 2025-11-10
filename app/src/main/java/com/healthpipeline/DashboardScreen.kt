package com.healthpipeline

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.healthpipeline.data.*
import com.healthpipeline.ui.theme.HealthPipelineTheme
import com.healthpipeline.utils.PermissionManager
import com.healthpipeline.utils.PermissionStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Managers and clients - with error handling
    val permissionManager = remember { 
        PermissionManager(context, context as androidx.activity.ComponentActivity) 
    }
    val apiClient = remember { ApiClient(context) }
    val healthConnectManager = remember { HealthConnectManager(context) }
    
    // State management
    var healthSummary by remember { mutableStateOf<HealthDataSummary?>(null) }
    var permissionStatus by remember { mutableStateOf(PermissionStatus.SOME_MISSING) }
    var isLoading by remember { mutableStateOf(false) }
    var apiConnectionStatus by remember { mutableStateOf("Unknown") }
    var currentJobId by remember { mutableStateOf<String?>(null) }
    var processingStatus by remember { mutableStateOf<ProcessingStatusResponse?>(null) }
    
    // Permission launcher
    val permissionLauncher = permissionManager.createPermissionLauncher { granted ->
        if (granted) {
            lifecycleOwner.lifecycleScope.launch {
                isLoading = true
                // TODO: Add health data loading when Health Connect is properly configured
                permissionStatus = permissionManager.getPermissionStatus()
                isLoading = false
            }
        }
    }
    
    // Test API connection on startup
    LaunchedEffect(Unit) {
        isLoading = true
        
        // Check permissions
        permissionStatus = permissionManager.getPermissionStatus()
        // TODO: Load health data when Health Connect is properly configured
        
        // Test API connection
        val connectionResult = apiClient.testConnection()
        apiConnectionStatus = when (connectionResult) {
            is ApiResult.Success -> "Connected"
            is ApiResult.Error -> "Disconnected: ${connectionResult.message}"
        }
        
        isLoading = false
    }
    
    // Poll processing status if job is running
    LaunchedEffect(currentJobId) {
        currentJobId?.let { jobId ->
            while (processingStatus?.status in listOf("pending", "processing")) {
                kotlinx.coroutines.delay(2000) // Poll every 2 seconds
                
                val statusResult = apiClient.safeApiCall {
                    apiClient.healthApiService.getProcessingStatus(jobId)
                }
                
                if (statusResult is ApiResult.Success) {
                    processingStatus = statusResult.data
                }
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Health Pipeline Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // API Connection Status
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "API Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (statusText, statusColor) = when {
                        apiConnectionStatus == "Connected" -> "● CONNECTED" to MaterialTheme.colorScheme.primary
                        apiConnectionStatus.startsWith("Disconnected") -> "● DISCONNECTED" to MaterialTheme.colorScheme.error
                        else -> "● CHECKING..." to MaterialTheme.colorScheme.outline
                    }
                    
                    Text(
                        text = statusText,
                        color = statusColor
                    )
                }
                
                if (apiConnectionStatus.startsWith("Disconnected")) {
                    Text(
                        text = apiConnectionStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedButton(
                        onClick = {
                            lifecycleOwner.lifecycleScope.launch {
                                val result = apiClient.testConnection()
                                apiConnectionStatus = when (result) {
                                    is ApiResult.Success -> "Connected"
                                    is ApiResult.Error -> "Disconnected: ${result.message}"
                                }
                            }
                        }
                    ) {
                        Text("Retry Connection")
                    }
                }
            }
        }
        
        // Health Connect Status Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Health Connect Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                when (permissionStatus) {
                    PermissionStatus.ALL_GRANTED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "● CONNECTED",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    PermissionStatus.SOME_MISSING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "● PERMISSIONS NEEDED",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { permissionManager.requestPermissions(permissionLauncher) }
                        ) {
                            Text("Grant Health Permissions")
                        }
                    }
                    PermissionStatus.HEALTH_CONNECT_NOT_AVAILABLE -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "● NOT AVAILABLE",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "Health Connect is not installed on this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Processing Status (if job is running)
        processingStatus?.let { status ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Processing Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Job: ${status.jobId.take(8)}...",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Status: ${status.status.uppercase()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (status.currentStage != null) {
                        Text(
                            text = "Stage: ${status.currentStage}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.progress.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "${(status.progress * 100).toInt()}% - ${status.recordsProcessed}/${status.recordsTotal} records",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Quick Actions
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            lifecycleOwner.lifecycleScope.launch {
                                if (permissionStatus == PermissionStatus.ALL_GRANTED && 
                                    apiConnectionStatus == "Connected") {
                                    
                                    isLoading = true
                                    
                                    // Get health data from Health Connect
                                    val stepsData = healthConnectManager.getTodaySteps()
                                    val heartRateData = healthConnectManager.getTodayHeartRate()
                                    val sleepData = healthConnectManager.getLastNightSleep()
                                    
                                    // Prepare API request
                                    val healthDataList = mutableListOf<Any>()
                                    stepsData?.let { stepData ->
                                        healthDataList.add(mapOf(
                                            "timestamp" to stepData.timestamp,
                                            "steps" to stepData.steps
                                        ))
                                    }
                                    
                                    if (healthDataList.isNotEmpty()) {
                                        val request = HealthDataRequest(
                                            userId = "user_${System.currentTimeMillis()}",
                                            dataType = "steps",
                                            data = healthDataList
                                        )
                                        
                                        // Send to API
                                        val result = apiClient.safeApiCall {
                                            apiClient.healthApiService.sendHealthData(request)
                                        }
                                        
                                        when (result) {
                                            is ApiResult.Success -> {
                                                currentJobId = result.data.jobId
                                                // Start polling for status
                                            }
                                            is ApiResult.Error -> {
                                                // Handle error
                                            }
                                        }
                                    }
                                    
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = permissionStatus == PermissionStatus.ALL_GRANTED && 
                                 apiConnectionStatus == "Connected" && 
                                 !isLoading &&
                                 processingStatus?.status !in listOf("pending", "processing")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Extract Data")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = { /* TODO: Navigate to analytics */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("View Results")
                    }
                }
            }
        }
        
        // Health Data Display (existing code)
        if (isLoading && processingStatus == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (healthSummary != null && permissionStatus == PermissionStatus.ALL_GRANTED) {
            // Health Data Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Steps Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${healthSummary?.stepsData?.steps ?: 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Steps Today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Heart Rate Card
                Card(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${healthSummary?.heartRateData?.averageBpm ?: "--"}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Avg Heart Rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Sleep Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sleep Last Night",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = healthSummary?.sleepData?.getFormattedDuration() ?: "No data",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        } else {
            // No data available
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Health data will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Grant permissions and ensure API connection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    HealthPipelineTheme {
        DashboardScreen()
    }
}
