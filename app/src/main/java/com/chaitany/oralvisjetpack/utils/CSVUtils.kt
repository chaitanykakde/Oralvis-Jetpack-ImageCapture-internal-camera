package com.chaitany.oralvisjetpack.utils

object CSVUtils {
    
    /**
     * Creates a simple CSV file with patient data
     * This replaces the heavy Apache POI library, saving ~20MB in APK size
     */
    fun createPatientCSV(
        name: String,
        age: String,
        gender: String,
        phone: String,
        timestamp: Long? = null
    ): ByteArray {
        val timestampStr = timestamp?.let { 
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it))
        } ?: ""
        
        val csv = buildString {
            // Header row
            appendLine("Name,Age,Gender,Phone Number,Timestamp")
            // Data row
            appendLine("\"$name\",$age,$gender,$phone,$timestampStr")
        }
        return csv.toByteArray(Charsets.UTF_8)
    }
}

