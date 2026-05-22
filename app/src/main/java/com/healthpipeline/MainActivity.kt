package com.healthpipeline

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.healthpipeline.data.HealthConnectManager
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import com.healthpipeline.data.local.HealthDatabase
import com.healthpipeline.data.repository.HealthRepository
import com.healthpipeline.ui.components.PHIABottomNavigation
import com.healthpipeline.ui.screens.*
import com.healthpipeline.ui.theme.HealthPipelineTheme
import com.healthpipeline.viewmodels.HealthDataViewModel
import com.healthpipeline.viewmodels.AuthViewModel
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    
    private val requestPermissionActivityContract = 
        PermissionController.createRequestPermissionResultContract()
    
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>
    // Flag to track if launcher is ready
    private var isPermissionLauncherReady = false
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var cloudSyncService: CloudSyncService
    private lateinit var repository: HealthRepository
    private lateinit var healthViewModel: HealthDataViewModel
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        healthConnectManager = HealthConnectManager(this)
        cloudSyncService = CloudSyncService(this)
        
        val database = HealthDatabase.getDatabase(this)
        val healthDataDao = database.healthDataDao()
        
        repository = HealthRepository(healthConnectManager, cloudSyncService, healthDataDao)
        healthViewModel = HealthDataViewModel(repository)

        scheduleSyncWorker()

        permissionLauncher = registerForActivityResult(requestPermissionActivityContract) { granted ->
            Log.d("MainActivity", "📋 Permission result received: ${granted.size} permissions granted")
            healthViewModel.checkPermissions()
            // If permissions granted, immediately load health data
            if (granted.isNotEmpty()) {
                Log.d("MainActivity", "✅ Permissions granted, loading health data...")
                healthViewModel.loadHealthData()
            } else {
                Log.d("MainActivity", "⚠️ No permissions granted by user")
            }
        }
        isPermissionLauncherReady = true
        Log.d("MainActivity", "✅ Permission launcher registered successfully")
        
        healthViewModel.checkPermissions()

        setContent {
            HealthPipelineTheme {
                val authState by authViewModel.uiState.collectAsState()
                val hasPermissions by healthViewModel.hasPermissions.collectAsState()

                // 🚀 PROACTIVE ONBOARDING: Ask for permissions immediately after login
                LaunchedEffect(authState.isSuccess, hasPermissions) {
                    if (authState.isSuccess && !hasPermissions) {
                        Log.d("MainActivity", "🚀 AUTO-ONBOARDING: Detecting missing permissions...")
                        launchHealthConnectPermissions()
                    }
                }

                if (!authState.isSuccess) {
                    // 🔐 Check if showing Sign Up or Sign In screen
                    var showSignUp by remember { mutableStateOf(false) }
                    
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (showSignUp) {
                            SignUpScreen(
                                viewModel = authViewModel,
                                onSignUp = { email, password, name ->
                                    authViewModel.signUp(email, password, name)
                                },
                                onBackToSignIn = { showSignUp = false }
                            )
                        } else {
                            LoginScreen(
                                viewModel = authViewModel,
                                onSignInClick = { email, password ->
                                    authViewModel.signIn(email, password)
                                },
                                onSignUpClick = { showSignUp = true }
                            )
                        }
                    }
                } else {
                    var currentScreen by remember { mutableStateOf("home") }

                    Scaffold(
                        bottomBar = {
                            PHIABottomNavigation(
                                currentScreen = currentScreen,
                                onNavigate = { currentScreen = it }
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentScreen) {
                                "home" -> DashboardScreen(
                                    viewModel = healthViewModel,
                                    onSignOut = { performLogout() },
                                    onRequestPermissions = { _ ->
                                        Log.d("MainActivity", "🔘 Grant Access button clicked")
                                        launchHealthConnectPermissions()
                                    },
                                    onOpenSettings = {
                                        Log.d("MainActivity", "🔘 Opening settings rationale/dialog")
                                        launchHealthConnectPermissions()
                                    },
                                    userId = authState.userId
                                )
                                "activity" -> ActivityScreen(viewModel = healthViewModel)
                                "profile" -> ProfileScreen(viewModel = healthViewModel)
                                "settings" -> SettingsScreen(
                                    viewModel = healthViewModel,
                                    authViewModel = authViewModel,
                                    onSignOut = { performLogout() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 🔐 Logout - New simplified flow
     */
    private fun performLogout() {
        Log.d("MainActivity", "🔐 Logging out...")
        authViewModel.logout()
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        // 1. Periodic Sync (Every 15 mins when connected)
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HealthSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        // 2. Daily Sync at 11:59 PM
        scheduleMidnightSync()
    }

    private fun scheduleMidnightSync() {
        val calendar = java.util.Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 0)
        
        var delay = calendar.timeInMillis - now
        if (delay <= 0) {
            delay += TimeUnit.DAYS.toMillis(1)
        }

        val dailyRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("DailyMidnightSync")
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "MidnightSync",
            ExistingWorkPolicy.REPLACE,
            dailyRequest
        )
    }

    /**
     * Safely requests Health Connect permissions.
     * Can be called from auto-trigger (LaunchedEffect) or manual button click.
     */
    private fun launchHealthConnectPermissions() {
        // Check if Health Connect is available on this device
        val availabilityStatus = HealthConnectClient.getSdkStatus(this)
        
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            Log.e("MainActivity", "❌ Health Connect SDK is unavailable on this device")
            Toast.makeText(this, "Health Connect is not available on this device", Toast.LENGTH_LONG).show()
            return
        }
        
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            Log.e("MainActivity", "❌ Health Connect provider update required")
            Toast.makeText(this, "Please update Health Connect app", Toast.LENGTH_LONG).show()
            // Optionally redirect to Play Store
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "Could not open Play Store", e)
            }
            return
        }
        
        if (!isPermissionLauncherReady) {
            Log.e("MainActivity", "❌ Permission launcher not initialized yet!")
            Toast.makeText(this, "Permission system not ready. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!::permissionLauncher.isInitialized) {
            Log.e("MainActivity", "❌ permissionLauncher not initialized!")
            Toast.makeText(this, "Permission launcher error", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            Log.d("MainActivity", "🎯 Launching Health Connect permission dialog...")
            Log.d("MainActivity", "📦 Requesting ${healthConnectManager.permissions.size} permissions")
            permissionLauncher.launch(healthConnectManager.permissions)
            Log.d("MainActivity", "✅ Permission dialog launched successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Failed to launch permission dialog: ${e.message}", e)
            Toast.makeText(this, "Failed to open permissions: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
