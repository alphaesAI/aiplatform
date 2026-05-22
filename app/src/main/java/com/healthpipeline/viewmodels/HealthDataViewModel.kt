package com.healthpipeline.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.repository.HealthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthDataViewModel(
    private val repository: HealthRepository
) : ViewModel() {

    private val _healthData = MutableStateFlow(HealthData())
    val healthData: StateFlow<HealthData> = _healthData.asStateFlow()

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _permissionStatus = MutableStateFlow("Checking...")
    val permissionStatus: StateFlow<String> = _permissionStatus.asStateFlow()

    private val _isLoadingData = MutableStateFlow(false)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Not synced")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun checkPermissions() {
        viewModelScope.launch {
            val (status, granted) = repository.checkPermissions()
            _permissionStatus.value = status
            _hasPermissions.value = granted
            
            if (granted) {
                loadHealthData()
            }
        }
    }

    fun loadHealthData() {
        viewModelScope.launch {
            _isLoadingData.value = true
            _healthData.value = repository.getTodayHealthData()
            _isLoadingData.value = false
        }
    }

    /**
     * 🚀 FIXED: Now accepts the Real User ID from Auth
     */
    fun syncData(userId: String?) {
        Log.d("HealthViewModel", "🔄 Sync requested for user: $userId")
        if (userId == null) {
            _syncMessage.value = "❌ Authentication required to sync"
            return
        }

        viewModelScope.launch {
            val currentData = _healthData.value
            Log.d("HealthViewModel", "📊 Data to sync: Steps=${currentData.totalSteps}, Active=${currentData.totalActiveMinutes}")
            
            if (currentData.totalSteps == 0 && currentData.totalActiveMinutes == 0) {
                Log.w("HealthViewModel", "⚠️ Skipping sync: No activity data found")
                _syncMessage.value = "⚠️ No health data to sync"
                delay(2000)
                _syncMessage.value = null
                return@launch
            }
            
            _isSyncing.value = true
            _cloudSyncStatus.value = "Syncing..."
            
            try {
                Log.d("HealthViewModel", "🚀 Triggering repository sync...")
                val result = repository.syncHealthData(currentData, userId)
                
                if (result.isSuccess) {
                    Log.i("HealthViewModel", "✅ Sync Success!")
                    _cloudSyncStatus.value = "Last synced: ${getCurrentTime()}"
                    _syncMessage.value = "✅ Synced successfully"
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("HealthViewModel", "❌ Sync Failed: $error")
                    _cloudSyncStatus.value = "Sync failed"
                    _syncMessage.value = "❌ Sync failed: $error"
                }
            } catch (e: Exception) {
                Log.e("HealthViewModel", "❌ Sync Exception", e)
                _cloudSyncStatus.value = "Sync error"
                _syncMessage.value = "❌ Error: ${e.message}"
            } finally {
                _isSyncing.value = false
                delay(3000)
                _syncMessage.value = null
            }
        }
    }
    
    private fun getCurrentTime(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date())
    }
    
    fun clearSyncMessage() {
        _syncMessage.value = null
    }
    
    /**
     * Update biometrics with manually entered age and gender
     * (Health Connect doesn't store these, so we save locally)
     */
    fun updateBiometrics(age: Int?, gender: String?) {
        viewModelScope.launch {
            val currentData = _healthData.value
            val updatedBiometrics = currentData.biometrics.copy(
                age = age,
                gender = gender
            )
            _healthData.value = currentData.copy(
                biometrics = updatedBiometrics
            )
            Log.d("HealthViewModel", "📊 Updated biometrics - Age: $age, Gender: $gender")
        }
    }
}
