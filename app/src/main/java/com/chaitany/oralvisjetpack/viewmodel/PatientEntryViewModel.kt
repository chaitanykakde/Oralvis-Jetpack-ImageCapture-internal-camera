package com.chaitany.oralvisjetpack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaitany.oralvisjetpack.data.repository.PatientCounterRepository
import com.chaitany.oralvisjetpack.utils.CSVUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PatientEntryViewModel(
    private val patientCounterRepository: PatientCounterRepository
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

