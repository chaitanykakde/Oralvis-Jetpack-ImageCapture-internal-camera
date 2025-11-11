package com.chaitany.oralvisjetpack.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaitany.oralvisjetpack.data.model.ClinicLoginData
import com.chaitany.oralvisjetpack.utils.CsvAuthUtils
import com.chaitany.oralvisjetpack.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(private val context: Context) : ViewModel() {
    
    private val preferencesManager = PreferencesManager(context)
    
    private val _clinicId = MutableStateFlow("")
    val clinicId: StateFlow<String> = _clinicId.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _loginSuccess = MutableStateFlow<ClinicLoginData?>(null)
    val loginSuccess: StateFlow<ClinicLoginData?> = _loginSuccess.asStateFlow()
    
    fun updateClinicId(id: String) {
        _clinicId.value = id
        _errorMessage.value = null
    }
    
    fun updatePassword(password: String) {
        _password.value = password
        _errorMessage.value = null
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun login() {
        val id = _clinicId.value.trim()
        val pwd = _password.value.trim()
        
        if (id.isEmpty() || pwd.isEmpty()) {
            _errorMessage.value = "Please enter Clinic ID and Password"
            return
        }
        
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                // Get all clinics to check ID and password separately
                val clinics = withContext(Dispatchers.IO) {
                    CsvAuthUtils.readClinicsFromAssets(context)
                }
                
                // Check if clinic ID exists
                val clinicById = clinics.firstOrNull { it.id == id }
                
                if (clinicById == null) {
                    // Clinic ID doesn't exist - check if password matches any clinic
                    val clinicByPassword = clinics.firstOrNull { it.password == pwd }
                    if (clinicByPassword == null) {
                        // Both are incorrect
                        _errorMessage.value = "Both are incorrect"
                    } else {
                        // Clinic ID is incorrect but password might be correct for another clinic
                        _errorMessage.value = "Both are incorrect"
                    }
                } else {
                    // Clinic ID exists - check password
                    if (clinicById.password == pwd) {
                        // Both are correct - login successful
                        preferencesManager.saveClinicSession(
                            clinicId = clinicById.id,
                            clinicName = clinicById.name,
                            shortName = clinicById.shortName
                        )
                        _loginSuccess.value = clinicById
                    } else {
                        // Clinic ID is correct but password is incorrect
                        _errorMessage.value = "Password is incorrect"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LoginViewModel", "Login error", e)
                _errorMessage.value = "Error during login: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

