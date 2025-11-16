package com.chaitany.oralvisjetpack.utils

import android.content.Context
import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.chaitany.oralvisjetpack.OralVisApplication
import com.chaitany.oralvisjetpack.data.model.ClinicLoginData
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringReader

object CsvAuthUtils {
    
    private const val TAG = "CsvAuthUtils"
    private const val S3_BUCKET_NAME = "oralvisclinics"
    private const val S3_CSV_KEY = "clinics_2025.csv" // S3 path for CSV file
    
    /**
     * Downloads and reads the clinics_2025.csv file from S3
     * Falls back to assets if S3 download fails
     * Returns a list of ClinicLoginData objects
     */
    fun readClinicsFromS3OrAssets(context: Context): List<ClinicLoginData> {
        // Try to download from S3 first
        val s3Clinics = try {
            downloadClinicsFromS3(context)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download CSV from S3, falling back to assets: ${e.message}")
            null
        }
        
        // If S3 download succeeded, use it
        if (s3Clinics != null && s3Clinics.isNotEmpty()) {
            Log.d(TAG, "✓ Successfully loaded ${s3Clinics.size} clinics from S3")
            return s3Clinics
        }
        
        // Fallback to assets
        Log.d(TAG, "Using clinics from assets folder (fallback)")
        return readClinicsFromAssets(context)
    }
    
    /**
     * Downloads the clinics CSV file from S3
     * Returns a list of ClinicLoginData objects
     */
    private fun downloadClinicsFromS3(context: Context): List<ClinicLoginData> {
        return try {
            val credentialsProvider = OralVisApplication.credentialsProvider
            
            val clientConfig = ClientConfiguration().apply {
                connectionTimeout = 30000
                socketTimeout = 60000
                maxErrorRetry = 3
            }
            
            val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
            val s3Client = AmazonS3Client(credentialsProvider, clientConfig)
            s3Client.setRegion(region)
            
            Log.d(TAG, "Downloading CSV from S3: s3://$S3_BUCKET_NAME/$S3_CSV_KEY")
            
            val getObjectRequest = GetObjectRequest(S3_BUCKET_NAME, S3_CSV_KEY)
            val s3Object = s3Client.getObject(getObjectRequest)
            val csvContent = s3Object.objectContent.bufferedReader().use { it.readText() }
            
            Log.d(TAG, "✓ CSV downloaded from S3 (${csvContent.length} bytes)")
            
            // Parse the CSV content
            parseCsvContent(csvContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading CSV from S3", e)
            throw e
        }
    }
    
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
                clinics.addAll(parseCsvLines(lines))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading clinics CSV from assets", e)
        }
        
        return clinics
    }
    
    /**
     * Parses CSV content (from S3 or assets) into ClinicLoginData objects
     */
    private fun parseCsvContent(csvContent: String): List<ClinicLoginData> {
        val clinics = mutableListOf<ClinicLoginData>()
        val reader = BufferedReader(StringReader(csvContent))
        
        // Skip header line
        reader.readLine()
        
        reader.useLines { lines ->
            clinics.addAll(parseCsvLines(lines))
        }
        
        return clinics
    }
    
    /**
     * Parses CSV lines into ClinicLoginData objects
     */
    private fun parseCsvLines(lines: Sequence<String>): List<ClinicLoginData> {
        val clinics = mutableListOf<ClinicLoginData>()
        
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
                    Log.d(TAG, "Parsed clinic: id=${clinic.id}, name=${clinic.name}")
                }
            }
        }
        
        return clinics
    }
    
    /**
     * Validates clinic ID and password against the CSV data from S3 (with assets fallback)
     * Returns the matching ClinicLoginData if found, null otherwise
     */
    fun validateLogin(context: Context, clinicId: String, password: String): ClinicLoginData? {
        val clinics = readClinicsFromS3OrAssets(context)
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

