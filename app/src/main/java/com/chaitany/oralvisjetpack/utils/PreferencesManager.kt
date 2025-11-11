package com.chaitany.oralvisjetpack.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("oralvis_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
        private const val KEY_CLINIC_ID = "clinic_id"
        private const val KEY_CLINIC_NAME = "clinic_name"
        private const val KEY_CLINIC_SHORT_NAME = "clinic_short_name"
    }
    
    fun saveCurrentStep(step: Int) {
        prefs.edit().putInt(KEY_CURRENT_STEP, step).apply()
    }
    
    fun getCurrentStep(): Int {
        return prefs.getInt(KEY_CURRENT_STEP, 0)
    }
    
    fun clearCurrentStep() {
        prefs.edit().remove(KEY_CURRENT_STEP).apply()
    }
    
    // Clinic session management
    fun saveClinicSession(clinicId: String, clinicName: String, shortName: String) {
        prefs.edit()
            .putString(KEY_CLINIC_ID, clinicId)
            .putString(KEY_CLINIC_NAME, clinicName)
            .putString(KEY_CLINIC_SHORT_NAME, shortName)
            .apply()
    }
    
    fun getClinicId(): String? {
        return prefs.getString(KEY_CLINIC_ID, null)
    }
    
    fun getClinicName(): String? {
        return prefs.getString(KEY_CLINIC_NAME, null)
    }
    
    fun getClinicShortName(): String? {
        return prefs.getString(KEY_CLINIC_SHORT_NAME, null)
    }
    
    fun getClinicIdInt(): Int {
        return getClinicId()?.toIntOrNull() ?: 0
    }
    
    fun clearClinicSession() {
        prefs.edit()
            .remove(KEY_CLINIC_ID)
            .remove(KEY_CLINIC_NAME)
            .remove(KEY_CLINIC_SHORT_NAME)
            .apply()
    }
    
    fun isLoggedIn(): Boolean {
        return getClinicId() != null
    }
}

