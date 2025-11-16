package com.chaitany.oralvisjetpack.utils

import android.content.Context
import android.content.SharedPreferences

object PatientMetadataUtils {
    private const val PREFS_NAME = "patient_metadata"
    
    fun savePatientMetadata(
        context: Context,
        clinicId: String,
        patientId: Int,
        name: String,
        age: String,
        gender: String,
        phone: String,
        timestamp: Long? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("name_${clinicId}_$patientId", name)
            .putString("age_${clinicId}_$patientId", age)
            .putString("gender_${clinicId}_$patientId", gender)
            .putString("phone_${clinicId}_$patientId", phone)
            .putLong("timestamp_${clinicId}_$patientId", timestamp ?: System.currentTimeMillis())
            .apply()
    }
    
    fun getPatientMetadata(
        context: Context,
        clinicId: String,
        patientId: Int
    ): PatientMetadata? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString("name_${clinicId}_$patientId", null)
        val age = prefs.getString("age_${clinicId}_$patientId", null)
        val gender = prefs.getString("gender_${clinicId}_$patientId", null)
        val phone = prefs.getString("phone_${clinicId}_$patientId", null)
        val timestamp = prefs.getLong("timestamp_${clinicId}_$patientId", 0L)
        
        return if (name != null && age != null && gender != null) {
            PatientMetadata(name, age, gender, phone ?: "", if (timestamp > 0) timestamp else null)
        } else {
            null
        }
    }
    
    data class PatientMetadata(
        val name: String,
        val age: String,
        val gender: String,
        val phone: String,
        val timestamp: Long? = null
    )
}








