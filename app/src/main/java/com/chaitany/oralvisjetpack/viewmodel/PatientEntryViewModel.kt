package com.chaitany.oralvisjetpack.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaitany.oralvisjetpack.OralVisApplication
import com.chaitany.oralvisjetpack.data.repository.PatientCounterRepository
import com.chaitany.oralvisjetpack.utils.CSVUtils
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.QueryRequest
import com.amazonaws.services.dynamodbv2.model.QueryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PatientEntryViewModel(
    private val patientCounterRepository: PatientCounterRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _patientName = MutableStateFlow("")
    val patientName: StateFlow<String> = _patientName.asStateFlow()

    private val _age = MutableStateFlow("")
    val age: StateFlow<String> = _age.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _gender = MutableStateFlow("Male")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _patientCounter = MutableStateFlow(1)
    val patientCounter: StateFlow<Int> = _patientCounter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPatientCounter()
    }

    fun loadPatientCounter() {
        viewModelScope.launch {
            _patientCounter.value = patientCounterRepository.getPatientCounter()
        }
    }
    
    /**
     * Sync patient counter from DynamoDB for the given clinic
     * Finds the maximum patient ID in the database and sets counter to max + 1
     */
    fun syncPatientCounterFromDatabase(clinicId: Int) {
        viewModelScope.launch {
            try {
                val maxPatientId = withContext(Dispatchers.IO) {
                    getMaxPatientIdFromDynamoDB(clinicId)
                }
                
                if (maxPatientId > 0) {
                    // Set counter to max + 1
                    val nextCounter = maxPatientId + 1
                    patientCounterRepository.updatePatientCounter(nextCounter)
                    _patientCounter.value = nextCounter
                    Log.d("PatientEntryVM", "Synced patient counter from database: maxPatientId=$maxPatientId, nextCounter=$nextCounter")
                } else {
                    // No patients found, use local counter or start from 1
                    val localCounter = patientCounterRepository.getPatientCounter()
                    if (localCounter == 1) {
                        // Keep it at 1 if no patients exist
                        Log.d("PatientEntryVM", "No patients found in database for clinic $clinicId, starting from 1")
                    } else {
                        _patientCounter.value = localCounter
                    }
                }
            } catch (e: Exception) {
                Log.e("PatientEntryVM", "Error syncing patient counter from database", e)
                // Fallback to local counter
                _patientCounter.value = patientCounterRepository.getPatientCounter()
            }
        }
    }
    
    private suspend fun getMaxPatientIdFromDynamoDB(clinicId: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                val credentialsProvider = OralVisApplication.credentialsProvider
                
                val clientConfig = ClientConfiguration().apply {
                    connectionTimeout = 30000
                    socketTimeout = 60000
                    maxErrorRetry = 3
                }
                
                val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
                val dynamoDBClient = AmazonDynamoDBClient(credentialsProvider, clientConfig)
                dynamoDBClient.setRegion(region)
                
                // Query patients for this clinic using GSI
                val queryRequest = QueryRequest()
                    .withTableName("OralVis_Patients")
                    .withIndexName("ClinicIdIndex")
                    .withKeyConditionExpression("clinicId = :clinicId")
                    .withExpressionAttributeValues(
                        mapOf(":clinicId" to AttributeValue().withN(clinicId.toString()))
                    )
                
                val queryResult: QueryResult = dynamoDBClient.query(queryRequest)
                
                // Find maximum patient ID
                var maxPatientId = 0
                queryResult.items.forEach { item ->
                    val patientIdStr = item["patientId"]?.s
                    val patientId = patientIdStr?.toIntOrNull() ?: 0
                    if (patientId > maxPatientId) {
                        maxPatientId = patientId
                    }
                }
                
                Log.d("PatientEntryVM", "Found max patient ID: $maxPatientId for clinic $clinicId (total patients: ${queryResult.items.size})")
                maxPatientId
            } catch (e: Exception) {
                Log.e("PatientEntryVM", "Error querying DynamoDB for max patient ID", e)
                0
            }
        }
    }

    fun updatePatientName(name: String) {
        _patientName.value = name
    }

    fun updateAge(ageValue: String) {
        _age.value = ageValue
    }

    fun updatePhone(phoneValue: String) {
        _phone.value = phoneValue
    }

    fun updateGender(genderValue: String) {
        _gender.value = genderValue
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun savePatientData(onSuccess: (String, Int, ByteArray, Long) -> Unit) {
        viewModelScope.launch {
            val name = _patientName.value.trim()
            val ageValue = _age.value.trim()
            val phoneValue = _phone.value.trim()
            val genderValue = _gender.value

            // Validation
            if (name.isEmpty() || ageValue.isEmpty()) {
                _errorMessage.value = "Please fill in name and age"
                return@launch
            }

            if (phoneValue.isNotEmpty() && 
                (phoneValue.length != 10 || !phoneValue.all { it.isDigit() })) {
                _errorMessage.value = "Phone number must be exactly 10 digits"
                return@launch
            }

            _isLoading.value = true
            try {
                // Create folder name
                val phoneSuffix = if (phoneValue.length >= 2) {
                    phoneValue.takeLast(2)
                } else {
                    "NA"
                }
                val folderName = "${name.replace(" ", "_")}_$phoneSuffix"

                // Create CSV file (lightweight alternative to Excel - saves ~20MB APK size)
                val timestamp = System.currentTimeMillis()
                val csvBytes = CSVUtils.createPatientCSV(
                    name = name,
                    age = ageValue,
                    gender = genderValue,
                    phone = phoneValue,
                    timestamp = timestamp
                )

                val currentCounter = _patientCounter.value
                
                onSuccess(folderName, currentCounter, csvBytes, timestamp)

                // Increment counter for next patient
                patientCounterRepository.updatePatientCounter(currentCounter + 1)
                _patientCounter.value = currentCounter + 1

                // Reset form
                _patientName.value = ""
                _age.value = ""
                _phone.value = ""
                _gender.value = "Male"

            } catch (e: Exception) {
                _errorMessage.value = "Error preparing data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

