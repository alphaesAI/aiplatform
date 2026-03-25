package com.healthpipeline

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import androidx.work.* // NEW
import com.healthpipeline.data.HealthConnectManager
import com.healthpipeline.data.local.HealthDatabase
import com.healthpipeline.data.repository.HealthRepository
import com.healthpipeline.ui.screens.DashboardScreen
import com.healthpipeline.ui.theme.HealthPipelineTheme
import com.healthpipeline.viewmodels.HealthDataViewModel
import java.util.concurrent.TimeUnit // NEW

class MainActivity : ComponentActivity() {
    
    private val requestPermissionActivityContract = 
        PermissionController.createRequestPermissionResultContract()
    
    private lateinit var requestPermissions: androidx.activity.result.ActivityResultLauncher<Set<String>>
    
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var cloudSyncService: CloudSyncService
    private lateinit var repository: HealthRepository
    private lateinit var viewModel: HealthDataViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize dependencies
        healthConnectManager = HealthConnectManager(this)
        cloudSyncService = CloudSyncService(this)
        
        val database = HealthDatabase.getDatabase(this)
        val healthDataDao = database.healthDataDao()
        
        repository = HealthRepository(healthConnectManager, cloudSyncService, healthDataDao)
        viewModel = HealthDataViewModel(repository)

        // NEW: Start the background sync robot
        scheduleSyncWorker()

        // 2. Initialize permission launcher
        requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
            Log.d("HealthPermissions", "Permission result received: $granted")
            viewModel.checkPermissions()
        }
        
        viewModel.checkPermissions()

        setContent {
            HealthPipelineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onRequestPermissions = { permissions ->
                            requestPermissions.launch(permissions)
                        },
                        onOpenSettings = {
                            val intent = Intent("androidx.health.ACTION_MANAGE_HEALTH_PERMISSIONS")
                            intent.putExtra("android.provider.extra.HEALTH_PERMISSIONS_PACKAGE_NAME", packageName)
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Failed to open settings", e)
                            }
                        }
                    )
                }
            }
        }
    }

    // NEW: Function to schedule background syncing
    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Only runs when internet is available
            .setRequiresBatteryNotLow(true)               // Don't drain battery if it's low
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HealthSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Don't restart if already running
            syncRequest
        )
        Log.d("MainActivity", "Background Sync Robot Scheduled!")
    }
}