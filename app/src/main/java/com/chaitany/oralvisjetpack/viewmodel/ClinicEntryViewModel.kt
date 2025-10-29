package com.chaitany.oralvisjetpack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaitany.oralvisjetpack.data.model.Clinic
import com.chaitany.oralvisjetpack.data.repository.ClinicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClinicEntryViewModel(
    private val clinicRepository: ClinicRepository
) : ViewModel() {

    private val _clinicName = MutableStateFlow("")
    val clinicName: StateFlow<String> = _clinicName.asStateFlow()

    private val _clinicId = MutableStateFlow("")
    val clinicId: StateFlow<String> = _clinicId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateClinicName(name: String) {
        _clinicName.value = name
    }

    fun updateClinicId(id: String) {
        _clinicId.value = id
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun saveClinicInfo(onSuccess: (String, Int) -> Unit) {
        viewModelScope.launch {
            val name = _clinicName.value.trim()
            val idText = _clinicId.value.trim()
            val clinicIdInt = idText.toIntOrNull()

            if (name.isEmpty() || clinicIdInt == null) {
                _errorMessage.value = "Please enter valid clinic name and ID"
                return@launch
            }

            _isLoading.value = true
            try {
                val clinic = Clinic(
                    id = clinicIdInt,
                    name = name,
                    clinicId = clinicIdInt
                )
                clinicRepository.insertClinic(clinic)
                onSuccess(name, clinicIdInt)
            } catch (e: Exception) {
                _errorMessage.value = "Error saving clinic: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

