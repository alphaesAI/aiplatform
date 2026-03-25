package com.healthpipeline.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthpipeline.data.models.HealthData
import com.healthpipeline.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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


    fun syncData() {
        viewModelScope.launch {
            val currentData = _healthData.value
            if (currentData.steps > 0) {
                _isSyncing.value = true
                _cloudSyncStatus.value = "Syncing..."
                
                val result = repository.syncHealthData(currentData)
                
                if (result.isSuccess) {
                    _cloudSyncStatus.value = "Synced successfully"
                } else {
                    _cloudSyncStatus.value = "Sync failed"
                }
                
                _isSyncing.value = false
            }
        }
    }
}