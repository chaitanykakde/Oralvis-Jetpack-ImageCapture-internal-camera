package com.chaitany.oralvisjetpack.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaitany.oralvisjetpack.OralVisApplication
import com.chaitany.oralvisjetpack.data.model.PatientData
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionDetailViewModel(private val context: Context) : ViewModel() {
    
    private val _patient = MutableStateFlow<PatientData?>(null)
    val patient: StateFlow<PatientData?> = _patient.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadPatient(patientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val patientData = withContext(Dispatchers.IO) {
                    fetchPatientFromDynamoDB(patientId)
                }
                
                _patient.value = patientData
                Log.d("SessionDetailViewModel", "Loaded patient: ${patientData?.name}")
            } catch (e: Exception) {
                Log.e("SessionDetailViewModel", "Error loading patient", e)
                _errorMessage.value = "Failed to load patient: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun fetchPatientFromDynamoDB(patientId: String): PatientData? {
        return withContext(Dispatchers.IO) {
            try {
                val credentialsProvider = OralVisApplication.credentialsProvider
                
                // Configure client
                val clientConfig = ClientConfiguration().apply {
                    connectionTimeout = 30000
                    socketTimeout = 60000
                    maxErrorRetry = 3
                }
                
                // Initialize DynamoDB client
                val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
                val dynamoDBClient = AmazonDynamoDBClient(credentialsProvider, clientConfig)
                dynamoDBClient.setRegion(region)
                
                // Initialize DynamoDB mapper
                val dynamoDBMapper = DynamoDBMapper(dynamoDBClient)
                
                // Load patient by ID
                val patient = PatientData().apply {
                    this.patientId = patientId
                }
                
                val loadedPatient = dynamoDBMapper.load(patient)
                Log.d("SessionDetailViewModel", "Fetched patient from DynamoDB: ${loadedPatient?.name}")
                
                loadedPatient
            } catch (e: Exception) {
                Log.e("SessionDetailViewModel", "Error fetching from DynamoDB", e)
                throw e
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

