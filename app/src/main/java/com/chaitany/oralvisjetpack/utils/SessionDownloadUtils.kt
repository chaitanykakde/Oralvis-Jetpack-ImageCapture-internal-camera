package com.chaitany.oralvisjetpack.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.chaitany.oralvisjetpack.OralVisApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.File
import java.io.FileOutputStream

object SessionDownloadUtils {
    private const val TAG = "SessionDownloadUtils"
    private const val BUCKET_NAME = "oralvis-patient-images"
    
    suspend fun downloadAndCreateZip(
        context: Context,
        clinicId: String,
        patientId: String,
        imagePaths: Map<String, String>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting download for clinicId=$clinicId, patientId=$patientId")
            
            // Get credentials provider
            val credentialsProvider = OralVisApplication.credentialsProvider
            
            // Configure client
            val clientConfig = ClientConfiguration().apply {
                connectionTimeout = 30000
                socketTimeout = 60000
                maxErrorRetry = 3
            }
            
            // Initialize S3 client
            val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
            val s3Client = AmazonS3Client(credentialsProvider, clientConfig)
            s3Client.setRegion(region)
            
            // Create Documents/myapp directory
            val documentsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "myapp"
            )
            
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            
            // Create zip file
            val zipFileName = "${clinicId}_${patientId}_session.zip"
            val zipFile = File(documentsDir, zipFileName)
            val zipOutputStream = ZipArchiveOutputStream(FileOutputStream(zipFile))
            
            // Download and add images to zip
            imagePaths.forEach { (fileName, s3Path) ->
                try {
                    Log.d(TAG, "Downloading image: $s3Path")
                    val s3Object = s3Client.getObject(BUCKET_NAME, s3Path)
                    val imageBytes = s3Object.objectContent.use { inputStream ->
                        inputStream.readBytes()
                    }
                    
                    // Add to zip
                    val entry = ZipArchiveEntry(fileName)
                    entry.size = imageBytes.size.toLong()
                    zipOutputStream.putArchiveEntry(entry)
                    zipOutputStream.write(imageBytes)
                    zipOutputStream.closeArchiveEntry()
                    
                    Log.d(TAG, "Added $fileName to zip (${imageBytes.size / 1024}KB)")
                } catch (e: Exception) {
                    Log.e(TAG, "Error downloading image: $s3Path", e)
                    // Continue with other images
                }
            }
            
            // Add Excel file to zip (same as when creating zip locally)
            try {
                val prefs = context.getSharedPreferences("patient_data", Context.MODE_PRIVATE)
                val encodedBytes = prefs.getString("excel_bytes_${clinicId}_$patientId", null)
                if (encodedBytes != null) {
                    val excelBytes = android.util.Base64.decode(encodedBytes, android.util.Base64.DEFAULT)
                    val excelFileName = "${clinicId}_$patientId.csv"
                    val excelEntry = ZipArchiveEntry(excelFileName)
                    excelEntry.size = excelBytes.size.toLong()
                    zipOutputStream.putArchiveEntry(excelEntry)
                    zipOutputStream.write(excelBytes)
                    zipOutputStream.closeArchiveEntry()
                    Log.d(TAG, "Added Excel file to zip: $excelFileName")
                } else {
                    Log.w(TAG, "Excel file not found in SharedPreferences for clinicId=$clinicId, patientId=$patientId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding Excel file to zip", e)
                // Continue even if Excel file fails
            }
            
            zipOutputStream.close()
            
            val zipPath = zipFile.absolutePath
            Log.d(TAG, "Zip file created successfully: $zipPath")
            Log.d(TAG, "Download location: Documents/myapp/ folder")
            Log.d(TAG, "Full path: $zipPath")
            Result.success(zipFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating zip file", e)
            Result.failure(e)
        }
    }
}



