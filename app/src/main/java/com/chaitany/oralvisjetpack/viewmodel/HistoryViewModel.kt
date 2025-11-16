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
    
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()
    
    fun loadPatientsForClinic(clinicId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Validate clinicId
            if (clinicId.isBlank()) {
                val errorMsg = "Invalid clinic ID. Please login again."
                Log.e("HistoryViewModel", errorMsg)
                _errorMessage.value = errorMsg
                _isLoading.value = false
                _patients.value = emptyList()
                return@launch
            }
            
            Log.d("HistoryViewModel", "=== STARTING loadPatientsForClinic with clinicId='$clinicId' ===")
            
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
                        Log.w("HistoryViewModel", "No patients found at all for clinicId=$clinicId")
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
                Log.e("HistoryViewModel", "❌ ERROR loading patients for clinicId='$clinicId'", e)
                e.printStackTrace()
                
                // Provide more detailed error message
                val errorMsg = when {
                    e.message?.contains("ResourceNotFoundException") == true -> 
                        "DynamoDB table 'OralVis_Patients' not found. Please create the table first."
                    e.message?.contains("AccessDeniedException") == true -> 
                        "Access denied. Please check AWS credentials."
                    e.message?.contains("ProvisionedThroughputExceededException") == true -> 
                        "Too many requests. Please try again later."
                    e.message?.contains("ValidationException") == true -> 
                        "Invalid query. Table structure may be incorrect."
                    else -> "Failed to load history: ${e.message ?: "Unknown error"}"
                }
                
                _errorMessage.value = errorMsg
                _patients.value = emptyList()
            } finally {
                _isLoading.value = false
                Log.d("HistoryViewModel", "=== COMPLETED loadPatientsForClinic for clinicId='$clinicId' ===")
            }
        }
    }
    
    private suspend fun fetchPatientsFromDynamoDB(clinicId: String): List<PatientData> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("HistoryViewModel", "=== FETCHING PATIENTS FOR CLINIC ID: $clinicId ===")
                
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
                
                // Now that clinicId is the partition key, we can query directly by clinicId
                val patientsList = try {
                    // Query by clinicId (partition key) - this will return all patients for this clinic
                    // clinicId is a String (can contain letters like "CLINIC001")
                    Log.d("HistoryViewModel", "Creating query request for clinicId='$clinicId' (type: ${clinicId.javaClass.simpleName})")
                    
                    val queryRequest = QueryRequest()
                        .withTableName("OralVis_Patients")
                        .withKeyConditionExpression("clinicId = :clinicId")
                        .withExpressionAttributeValues(
                            mapOf(":clinicId" to AttributeValue().withS(clinicId))
                        )
                    
                    Log.d("HistoryViewModel", "Executing query for clinicId='$clinicId' as partition key")
                    
                    // Perform query
                    Log.d("HistoryViewModel", "Calling dynamoDBClient.query()...")
                    val queryResult: QueryResult = try {
                        dynamoDBClient.query(queryRequest)
                    } catch (e: Exception) {
                        Log.e("HistoryViewModel", "❌ Query failed with exception", e)
                        throw e
                    }
                    
                    Log.d("HistoryViewModel", "✓ Query returned ${queryResult.items.size} items for clinicId='$clinicId'")
                    
                    // Convert DynamoDB items to PatientData objects
                    val convertedPatients = queryResult.items.mapNotNull { item ->
                        try {
                            val patient = PatientData()
                            
                            // clinicId is the partition key (hash key) - stored as String
                            patient.clinicId = item["clinicId"]?.s
                            
                            // patientId is the range key
                            patient.patientId = item["patientId"]?.s
                            
                            // Verify clinicId matches
                            if (patient.clinicId != clinicId) {
                                Log.e("HistoryViewModel", "⚠️ CLINIC ID MISMATCH! Query for clinicId=$clinicId returned patient ${patient.patientId} with clinicId=${patient.clinicId}")
                            } else {
                                Log.d("HistoryViewModel", "✓ Patient ${patient.patientId} has matching clinicId=${patient.clinicId}")
                            }
                            
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
                    
                    Log.d("HistoryViewModel", "Converted ${convertedPatients.size} patients from query")
                    convertedPatients
                } catch (e: Exception) {
                    // Fallback to DynamoDBMapper query if low-level query fails
                    Log.w("HistoryViewModel", "Low-level query failed, trying DynamoDBMapper: ${e.message}")
                    e.printStackTrace()
                    
                    try {
                        val dynamoDBMapper = DynamoDBMapper(dynamoDBClient)
                        
                        // Create a query expression for clinicId (partition key) - String type
                        val queryExpression = com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression<PatientData>()
                            .withHashKeyValues(PatientData().apply { this.clinicId = clinicId })
                        
                        val queryResult = dynamoDBMapper.query(PatientData::class.java, queryExpression)
                        val queriedPatients = queryResult.toList()
                        
                        Log.d("HistoryViewModel", "DynamoDBMapper query returned ${queriedPatients.size} patients for clinicId=$clinicId")
                        queriedPatients.forEach { patient ->
                            Log.d("HistoryViewModel", "Queried Patient ${patient.patientId}: clinicId=${patient.clinicId}, timestamp=${patient.timestamp}, name=${patient.name}")
                        }
                        
                        queriedPatients
                    } catch (e2: Exception) {
                        Log.e("HistoryViewModel", "DynamoDBMapper query also failed, falling back to scan", e2)
                        
                        // Last resort: scan with filter
                        val dynamoDBMapper = DynamoDBMapper(dynamoDBClient)
                        val scanExpression = DynamoDBScanExpression().apply {
                            addFilterCondition(
                                "clinicId",
                                Condition()
                                    .withComparisonOperator(ComparisonOperator.EQ)
                                    .withAttributeValueList(AttributeValue().withS(clinicId))
                            )
                        }
                        
                        val scanResult: PaginatedScanList<PatientData> = dynamoDBMapper.scan(
                            PatientData::class.java,
                            scanExpression
                        )
                        
                        val scannedPatients = scanResult.toList()
                        Log.d("HistoryViewModel", "Scanned ${scannedPatients.size} patients with filter clinicId=$clinicId")
                        scannedPatients
                    }
                }
                
                // Post-filter to ensure ONLY patients with matching clinicId (safety check)
                val filteredPatients = patientsList.filter { patient ->
                    val matches = patient.clinicId == clinicId
                    if (!matches) {
                        Log.e("HistoryViewModel", "🚨 FILTERING OUT PATIENT ${patient.patientId}: Expected clinicId=$clinicId but got clinicId=${patient.clinicId}")
                    }
                    matches
                }
                
                Log.d("HistoryViewModel", "=== FINAL RESULT: Fetched ${patientsList.size} patients, ${filteredPatients.size} match clinicId=$clinicId ===")
                
                // Log each patient for verification
                filteredPatients.forEach { patient ->
                    val dateStr = patient.timestamp?.let {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(it))
                    } ?: "No timestamp"
                    Log.d("HistoryViewModel", "Final patient: ${patient.patientId}, clinicId=${patient.clinicId}, name=${patient.name}, timestamp=$dateStr")
                }
                
                filteredPatients
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error fetching from DynamoDB", e)
                e.printStackTrace()
                throw e
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Delete a patient session from DynamoDB
     * @param clinicId The clinic ID (partition key)
     * @param patientId The patient ID (sort key)
     * @param onSuccess Callback when deletion succeeds
     * @param onError Callback when deletion fails
     */
    fun deletePatient(clinicId: String, patientId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            _errorMessage.value = null
            
            try {
                withContext(Dispatchers.IO) {
                    val credentialsProvider = OralVisApplication.credentialsProvider
                    
                    val clientConfig = ClientConfiguration().apply {
                        connectionTimeout = 30000
                        socketTimeout = 60000
                        maxErrorRetry = 3
                    }
                    
                    val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
                    val dynamoDBClient = AmazonDynamoDBClient(credentialsProvider, clientConfig)
                    dynamoDBClient.setRegion(region)
                    
                    val dynamoDBMapper = DynamoDBMapper(dynamoDBClient)
                    
                    // Create PatientData object with composite key for deletion
                    val patientToDelete = PatientData().apply {
                        this.clinicId = clinicId  // Partition key
                        this.patientId = patientId  // Sort key
                    }
                    
                    Log.d("HistoryViewModel", "Deleting patient: clinicId=$clinicId, patientId=$patientId")
                    
                    // Delete from DynamoDB
                    dynamoDBMapper.delete(patientToDelete)
                    
                    Log.d("HistoryViewModel", "✓ Successfully deleted patient: clinicId=$clinicId, patientId=$patientId")
                }
                
                // Remove from local list
                _patients.value = _patients.value.filter { 
                    it.clinicId != clinicId || it.patientId != patientId 
                }
                
                onSuccess()
            } catch (e: Exception) {
                Log.e("HistoryViewModel", "Error deleting patient", e)
                val errorMsg = "Failed to delete session: ${e.message}"
                _errorMessage.value = errorMsg
                onError(errorMsg)
            } finally {
                _isDeleting.value = false
            }
        }
    }
}

