package com.chaitany.oralvisjetpack.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("oralvis_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_CURRENT_STEP = "current_step"
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
}

