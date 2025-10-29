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
        phone: String
    ): ByteArray {
        val csv = buildString {
            // Header row
            appendLine("Name,Age,Gender,Phone Number")
            // Data row
            appendLine("\"$name\",$age,$gender,$phone")
        }
        return csv.toByteArray(Charsets.UTF_8)
    }
}

