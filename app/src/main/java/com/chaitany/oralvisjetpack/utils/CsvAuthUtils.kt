package com.chaitany.oralvisjetpack.utils

import android.content.Context
import com.chaitany.oralvisjetpack.data.model.ClinicLoginData
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvAuthUtils {
    
    /**
     * Reads and parses the clinics_2025.csv file from assets folder
     * Returns a list of ClinicLoginData objects
     */
    fun readClinicsFromAssets(context: Context): List<ClinicLoginData> {
        val clinics = mutableListOf<ClinicLoginData>()
        
        try {
            val inputStream = context.assets.open("clinics_2025.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            // Skip header line
            reader.readLine()
            
            // Read each line
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        // Parse CSV line, handling quoted fields
                        val parts = parseCsvLine(line)
                        if (parts.size >= 4) {
                            // CSV format: id, name, short_name, password
                            val clinic = ClinicLoginData(
                                id = parts[0].trim(),
                                name = parts[1].trim(),
                                shortName = parts[2].trim(),
                                password = parts[3].trim()
                            )
                            clinics.add(clinic)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CsvAuthUtils", "Error reading clinics CSV", e)
        }
        
        return clinics
    }
    
    /**
     * Validates clinic ID and password against the CSV data
     * Returns the matching ClinicLoginData if found, null otherwise
     */
    fun validateLogin(context: Context, clinicId: String, password: String): ClinicLoginData? {
        val clinics = readClinicsFromAssets(context)
        return clinics.firstOrNull { 
            it.id == clinicId && it.password == password 
        }
    }
    
    /**
     * Parse a CSV line, handling quoted fields that may contain commas
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        result.add(current.toString()) // Add the last field
        
        return result
    }
}

