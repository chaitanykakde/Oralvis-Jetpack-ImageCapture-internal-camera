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
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator
import com.amazonaws.services.dynamodbv2.model.Condition
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.QueryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(private val context: Context) : ViewModel() {
    
    private val _patients = MutableStateFlow<List<PatientData>>(emptyList())
    val patients: StateFlow<List<PatientData>> = _patients.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun loadPatientsForClinic(clinicId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val patientsList = withContext(Dispatchers.IO) {
                    fetchPatientsFromDynamoDB(clinicId)
                }
                
                // Sort by patientId (descending) to show newest first
                val sortedPatients = patientsList.sortedByDescending { 
                    it.patientId?.toIntOrNull() ?: 0 
                }
                
                _patients.value = sortedPatients
                Log.d("HistoryViewModel", "Loaded ${sortedPatients.size} patients for clinic $clinicId")
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error loading patients", e)
                _errorMessage.value = "Failed to load history: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun fetchPatientsFromDynamoDB(clinicId: Int): List<PatientData> {
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
                
                // Try GSI query first, fallback to scan if GSI doesn't exist
                val patientsList = try {
                    // Use low-level query API for GSI query
                    val queryRequest = QueryRequest()
                        .withTableName("OralVis_Patients")
                        .withIndexName("ClinicIdIndex")
                        .withKeyConditionExpression("clinicId = :clinicId")
                        .withExpressionAttributeValues(
                            mapOf(":clinicId" to AttributeValue().withN(clinicId.toString()))
                        )
                    
                    // Perform query using the GSI
                    val queryResult: QueryResult = dynamoDBClient.query(queryRequest)
                    
                    // Convert DynamoDB items to PatientData objects manually
                    queryResult.items.mapNotNull { item ->
                        try {
                            val patient = PatientData()
                            patient.patientId = item["patientId"]?.s
                            // Try number first, then string (in case clinicId is stored as string)
                            patient.clinicId = item["clinicId"]?.n?.toIntOrNull() 
                                ?: item["clinicId"]?.s?.toIntOrNull()
                            patient.name = item["name"]?.s
                            patient.age = item["age"]?.s
                            patient.gender = item["gender"]?.s
                            patient.phone = item["phone"]?.s
                            // Convert imagePaths map if present
                            item["imagePaths"]?.m?.let { mapAttr ->
                                patient.imagePaths = mapAttr.mapValues { it.value.s ?: "" }
                            }
                            patient
                        } catch (e: Exception) {
                            Log.e("HistoryViewModel", "Error converting item to PatientData", e)
                            null
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to scan if GSI query fails (GSI might not exist or other error)
                    Log.w("HistoryViewModel", "GSI query failed, falling back to scan: ${e.message}")
                    
                    val dynamoDBMapper = DynamoDBMapper(dynamoDBClient)
                    val scanExpression = DynamoDBScanExpression().apply {
                        addFilterCondition(
                            "clinicId",
                            Condition()
                                .withComparisonOperator(ComparisonOperator.EQ)
                                .withAttributeValueList(AttributeValue().withN(clinicId.toString()))
                        )
                    }
                    
                    val scanResult: PaginatedScanList<PatientData> = dynamoDBMapper.scan(
                        PatientData::class.java,
                        scanExpression
                    )
                    
                    scanResult.toList()
                }
                
                Log.d("HistoryViewModel", "Fetched ${patientsList.size} patients from DynamoDB for clinicId=$clinicId")
                
                patientsList
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error fetching from DynamoDB", e)
                throw e
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}

