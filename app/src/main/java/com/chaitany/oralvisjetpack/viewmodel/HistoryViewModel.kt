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
                
                // Log all fetched patients for debugging
                Log.d("HistoryViewModel", "Fetched ${patientsList.size} total patients from database")
                patientsList.forEach { patient ->
                    Log.d("HistoryViewModel", "Fetched Patient ${patient.patientId}: timestamp=${patient.timestamp}, name=${patient.name}")
                }
                
                // Check timestamp values for all patients
                patientsList.forEach { patient ->
                    Log.d("HistoryViewModel", "Checking Patient ${patient.patientId}: timestamp=${patient.timestamp}, timestamp type=${patient.timestamp?.javaClass?.simpleName}")
                }
                
                // Filter out patients without timestamps - only show new patients with timestamps
                // But also include patients with timestamps that might be 0 or null (for debugging)
                val patientsWithTimestamps = patientsList.filter { patient ->
                    val hasTimestamp = patient.timestamp != null && patient.timestamp!! > 0L
                    if (!hasTimestamp) {
                        Log.w("HistoryViewModel", "Filtering out Patient ${patient.patientId}: timestamp is null or <= 0")
                    }
                    hasTimestamp
                }
                
                Log.d("HistoryViewModel", "After filtering: ${patientsWithTimestamps.size} patients with valid timestamps out of ${patientsList.size} total")
                
                if (patientsWithTimestamps.isEmpty()) {
                    if (patientsList.isNotEmpty()) {
                        Log.w("HistoryViewModel", "WARNING: No patients with timestamps found! Showing all patients for debugging.")
                        // Temporarily show all patients if none have timestamps (for debugging)
                        // This helps us see if patients exist but just don't have timestamps
                        val allPatientsSorted = patientsList.sortedByDescending { patient ->
                            patient.timestamp ?: 0L
                        }
                        _patients.value = allPatientsSorted
                    } else {
                        Log.w("HistoryViewModel", "No patients found at all for clinic $clinicId")
                        _patients.value = emptyList()
                    }
                    return@launch
                }
                
                patientsWithTimestamps.forEach { patient ->
                    Log.d("HistoryViewModel", "Valid Patient ${patient.patientId}: timestamp=${patient.timestamp}, name=${patient.name}")
                }
                
                // Sort by timestamp (descending) to show newest first
                val sortedPatients = patientsWithTimestamps.sortedByDescending { patient ->
                    patient.timestamp!!
                }
                
                // Log final sorted order
                Log.d("HistoryViewModel", "Final sorted list (${sortedPatients.size} patients):")
                sortedPatients.forEachIndexed { index, patient ->
                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(patient.timestamp!!))
                    Log.d("HistoryViewModel", "  [$index] Patient ${patient.patientId}: timestamp=${patient.timestamp} ($dateStr), name=${patient.name}")
                }
                
                // Update the list atomically - all at once to prevent partial updates
                _patients.value = sortedPatients
                Log.d("HistoryViewModel", "Updated UI with ${sortedPatients.size} patients for clinic $clinicId")
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
                            // Read timestamp (can be number or string)
                            patient.timestamp = item["timestamp"]?.n?.toLongOrNull()
                                ?: item["timestamp"]?.s?.toLongOrNull()
                            
                            // Log timestamp reading for debugging
                            if (patient.timestamp == null) {
                                Log.w("HistoryViewModel", "Patient ${patient.patientId} has no timestamp in database")
                            } else {
                                Log.d("HistoryViewModel", "Patient ${patient.patientId} timestamp: ${patient.timestamp}")
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
                    
                    val scannedPatients = scanResult.toList()
                    
                    // Log scanned patients and their timestamps
                    Log.d("HistoryViewModel", "Scanned ${scannedPatients.size} patients")
                    scannedPatients.forEach { patient ->
                        Log.d("HistoryViewModel", "Scanned Patient ${patient.patientId}: timestamp=${patient.timestamp}, name=${patient.name}")
                    }
                    
                    scannedPatients
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

