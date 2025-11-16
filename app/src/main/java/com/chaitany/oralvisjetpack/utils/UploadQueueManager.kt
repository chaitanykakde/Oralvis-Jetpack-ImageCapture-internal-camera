package com.chaitany.oralvisjetpack.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import com.chaitany.oralvisjetpack.OralVisApplication
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.chaitany.oralvisjetpack.data.model.PatientData
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

object UploadQueueManager {
    private const val QUEUE_FOLDER = ".upload_queue"
    private const val TAG = "UploadQueueManager"
    private val processingJobs = ConcurrentHashMap<String, Job>()
    
    /**
     * Add patient data to upload queue
     */
    fun addToQueue(
        context: Context,
        folderName: String,
        clinicId: String,
        patientId: Int,
        imageFiles: Map<String, ByteArray>,
        excelBytes: ByteArray
    ) {
        try {
            Log.d(TAG, "Adding to queue: clinicId=$clinicId, patientId=$patientId, images=${imageFiles.size}")
            val queueDir = File(context.getExternalFilesDir(null), QUEUE_FOLDER)
            if (!queueDir.exists()) {
                queueDir.mkdirs()
                Log.d(TAG, "Created queue directory: ${queueDir.absolutePath}")
            }
            
            val patientFolder = File(queueDir, "${clinicId}_$patientId")
            if (!patientFolder.exists()) {
                patientFolder.mkdirs()
                Log.d(TAG, "Created patient folder: ${patientFolder.absolutePath}")
            }
            
            // Save images
            imageFiles.forEach { (fileName, bytes) ->
                val imageFile = File(patientFolder, fileName)
                FileOutputStream(imageFile).use { it.write(bytes) }
                Log.d(TAG, "Saved image to queue: $fileName (${bytes.size / 1024}KB)")
            }
            
            // Save metadata
            val metadataFile = File(patientFolder, "metadata.json")
            // CRITICAL: clinicId must be quoted in JSON since it can contain letters (e.g., "MDC202501")
            val metadata = """
                {
                    "folderName": "$folderName",
                    "clinicId": "$clinicId",
                    "patientId": $patientId,
                    "excelBytes": "${android.util.Base64.encodeToString(excelBytes, android.util.Base64.DEFAULT)}"
                }
            """.trimIndent()
            FileOutputStream(metadataFile).use { it.write(metadata.toByteArray()) }
            Log.d(TAG, "Saved metadata to queue")
            
            Log.d(TAG, "Successfully added to queue: ${patientFolder.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to queue", e)
            throw e
        }
    }
    
    /**
     * Process upload queue in background with progress updates
     */
    fun processQueueWithProgress(
        context: Context,
        onProgress: (current: Int, total: Int, message: String) -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        scope.launch {
            try {
                val queueDir = File(context.getExternalFilesDir(null), QUEUE_FOLDER)
                if (!queueDir.exists() || !queueDir.isDirectory) {
                    withContext(Dispatchers.Main) {
                        onComplete(true, "No items in queue")
                    }
                    return@launch
                }
                
                // Updated regex to support clinicId with letters (e.g., "MDC202501_3", "CLINIC001_1")
                // Pattern: clinicId (alphanumeric + underscore/hyphen) + "_" + patientId (digits)
                val patientFolders = queueDir.listFiles { file ->
                    file.isDirectory && file.name.matches(Regex("[\\w-]+_\\d+"))
                } ?: emptyArray()
                
                Log.d(TAG, "Found ${patientFolders.size} patient folders in queue")
                patientFolders.forEach { folder ->
                    Log.d(TAG, "  - Queue folder: ${folder.name}")
                }
                
                if (patientFolders.isEmpty()) {
                    Log.w(TAG, "No patient folders found matching pattern [\\w-]+_\\d+")
                    withContext(Dispatchers.Main) {
                        onComplete(true, "No items in queue")
                    }
                    return@launch
                }
                
                // Calculate total images across all patients
                val totalPatients = patientFolders.size
                var uploadedImages = 0
                var totalImagesCount = 0
                var completedPatients = 0
                val imageProgressLock = java.util.concurrent.locks.ReentrantLock()
                
                // First, count total images
                patientFolders.forEach { folder ->
                    val imageFiles = folder.listFiles { file ->
                        file.isFile && file.name.endsWith(".jpg")
                    } ?: emptyArray()
                    totalImagesCount += imageFiles.size
                }
                
                for (patientFolder in patientFolders) {
                    val key = patientFolder.name
                    if (processingJobs.containsKey(key)) {
                        continue
                    }
                    
                    val job = launch {
                        try {
                            uploadPatientData(
                                context = context,
                                patientFolder = patientFolder,
                                onImageProgress = { imageIndex, patientImageCount, fileName ->
                                    imageProgressLock.lock()
                                    try {
                                        uploadedImages++
                                        val progress = if (totalImagesCount > 0) {
                                            ((uploadedImages * 100) / totalImagesCount).coerceIn(0, 100)
                                        } else {
                                            0
                                        }
                                        val message = "Uploading: $fileName ($uploadedImages/$totalImagesCount images)"
                                        launch(Dispatchers.Main) {
                                            onProgress(progress, 100, message)
                                        }
                                    } finally {
                                        imageProgressLock.unlock()
                                    }
                                },
                                onComplete = { success ->
                                    imageProgressLock.lock()
                                    try {
                                        completedPatients++
                                        if (success) {
                                            patientFolder.deleteRecursively()
                                            Log.d(TAG, "Deleted queue folder: ${patientFolder.absolutePath}")
                                        } else {
                                            Log.w(TAG, "Upload failed for: ${patientFolder.absolutePath}")
                                        }
                                        val message = if (success) {
                                            "Completed $completedPatients/$totalPatients patients"
                                        } else {
                                            "Failed $completedPatients/$totalPatients patients"
                                        }
                                        launch(Dispatchers.Main) {
                                            val progress = if (totalPatients > 0) {
                                                ((completedPatients * 100) / totalPatients).coerceIn(0, 100)
                                            } else {
                                                0
                                            }
                                            onProgress(progress, 100, message)
                                        }
                                    } finally {
                                        imageProgressLock.unlock()
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing queue item: ${patientFolder.name}", e)
                            imageProgressLock.lock()
                            try {
                                completedPatients++
                                launch(Dispatchers.Main) {
                                    val progress = if (totalPatients > 0) {
                                        ((completedPatients * 100) / totalPatients).coerceIn(0, 100)
                                    } else {
                                        0
                                    }
                                    onProgress(progress, 100, "Error: ${e.message}")
                                }
                            } finally {
                                imageProgressLock.unlock()
                            }
                        } finally {
                            processingJobs.remove(key)
                        }
                    }
                    
                    processingJobs[key] = job
                }
                
                // Wait for all uploads to complete
                processingJobs.values.forEach { it.join() }
                
                withContext(Dispatchers.Main) {
                    onComplete(true, "All uploads completed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing queue", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "Upload failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Process upload queue in background
     */
    fun processQueue(
        context: Context,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        scope.launch {
            try {
                val queueDir = File(context.getExternalFilesDir(null), QUEUE_FOLDER)
                if (!queueDir.exists() || !queueDir.isDirectory) {
                    onComplete?.invoke(true, "No items in queue")
                    return@launch
                }
                
                // Updated regex to support clinicId with letters (e.g., "MDC202501_3", "CLINIC001_1")
                // Pattern: clinicId (alphanumeric + underscore/hyphen) + "_" + patientId (digits)
                val patientFolders = queueDir.listFiles { file ->
                    file.isDirectory && file.name.matches(Regex("[\\w-]+_\\d+"))
                } ?: emptyArray()
                
                Log.d(TAG, "Found ${patientFolders.size} patient folders in queue (processQueue)")
                patientFolders.forEach { folder ->
                    Log.d(TAG, "  - Queue folder: ${folder.name}")
                }
                
                if (patientFolders.isEmpty()) {
                    Log.w(TAG, "No patient folders found matching pattern [\\w-]+_\\d+")
                    onComplete?.invoke(true, "No items in queue")
                    return@launch
                }
                
                for (patientFolder in patientFolders) {
                    val key = patientFolder.name
                    if (processingJobs.containsKey(key)) {
                        continue // Already processing
                    }
                    
                    val job = launch {
                        try {
                            uploadPatientData(
                                context = context,
                                patientFolder = patientFolder,
                                onImageProgress = null, // No progress callback for non-progress version
                                onComplete = { success ->
                                    if (success) {
                                        // Delete folder after successful upload
                                        patientFolder.deleteRecursively()
                                        Log.d(TAG, "Deleted queue folder: ${patientFolder.absolutePath}")
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing queue item", e)
                        } finally {
                            processingJobs.remove(key)
                        }
                    }
                    
                    processingJobs[key] = job
                }
                
                // Wait for all uploads to complete
                processingJobs.values.forEach { it.join() }
                
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, "Upload completed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing queue", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Upload failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Check if internet is available
     */
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Upload patient data from queue folder
     */
    private suspend fun uploadPatientData(
        context: Context,
        patientFolder: File,
        onImageProgress: ((imageIndex: Int, totalImages: Int, fileName: String) -> Unit)? = null,
        onComplete: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Starting upload for: ${patientFolder.name} ===")
            if (!isInternetAvailable(context)) {
                Log.w(TAG, "No internet available, skipping upload for: ${patientFolder.name}")
                onComplete(false)
                return@withContext
            }
            Log.d(TAG, "Internet available, proceeding with upload")
            
            // Read metadata
            val metadataFile = File(patientFolder, "metadata.json")
            if (!metadataFile.exists()) {
                Log.e(TAG, "Metadata file not found in: ${patientFolder.absolutePath}")
                onComplete(false)
                return@withContext
            }
            
            Log.d(TAG, "Reading metadata from: ${metadataFile.absolutePath}")
            val metadataJson = metadataFile.readText()
            Log.d(TAG, "Raw metadata JSON: $metadataJson")
            
            val folderName = extractJsonValue(metadataJson, "folderName")
            val clinicIdStr = extractJsonValue(metadataJson, "clinicId")
            val patientIdStr = extractJsonValue(metadataJson, "patientId")
            val excelBytesBase64 = extractJsonValue(metadataJson, "excelBytes")
            
            if (clinicIdStr.isEmpty() || patientIdStr.isEmpty()) {
                Log.e(TAG, "Failed to extract clinicId or patientId. clinicId='$clinicIdStr', patientId='$patientIdStr'")
                throw Exception("Invalid metadata: missing clinicId or patientId")
            }
            
            // clinicId can be a string (e.g., "CLINIC001", "ABC123")
            val clinicId = clinicIdStr
            val patientId = patientIdStr.toIntOrNull() ?: throw Exception("Invalid patientId: '$patientIdStr'")
            val excelBytes = android.util.Base64.decode(excelBytesBase64, android.util.Base64.DEFAULT)
            
            Log.d(TAG, "Metadata loaded: clinicId=$clinicId, patientId=$patientId, folderName=$folderName")
            
            // Get credentials provider
            val credentialsProvider = OralVisApplication.credentialsProvider
            
            // Configure client
            val clientConfig = ClientConfiguration().apply {
                connectionTimeout = 60000
                socketTimeout = 120000
                maxErrorRetry = 3
            }
            
            // Initialize S3 client
            val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
            val s3Client = AmazonS3Client(credentialsProvider, clientConfig)
            s3Client.setRegion(region)
            val bucketName = "oralvis-patient-images"
            
            // Upload images
            val imagePaths = mutableMapOf<String, String>()
            val imageFiles = patientFolder.listFiles { file ->
                file.isFile && file.name.endsWith(".jpg")
            } ?: emptyArray()
            
            Log.d(TAG, "Found ${imageFiles.size} images to upload")
            imageFiles.forEachIndexed { index, imageFile ->
                try {
                    val fileName = imageFile.name
                    // Use consistent format: clinicId_patientId (matches ImageSequenceViewModel)
                    val s3Key = "public/${clinicId}_${patientId}/$fileName"
                    val imageBytes = imageFile.readBytes()
                    Log.d(TAG, "Uploading image ${index + 1}/${imageFiles.size}: $fileName (${imageBytes.size / 1024}KB)")
                    
                    // Report progress before upload
                    onImageProgress?.invoke(index + 1, imageFiles.size, fileName)
                    
                    val metadata = ObjectMetadata().apply {
                        contentLength = imageBytes.size.toLong()
                        contentType = "image/jpeg"
                    }
                    
                    val putObjectRequest = PutObjectRequest(
                        bucketName,
                        s3Key,
                        java.io.ByteArrayInputStream(imageBytes),
                        metadata
                    )
                    
                    s3Client.putObject(putObjectRequest)
                    imagePaths[fileName] = s3Key
                    Log.d(TAG, "Successfully uploaded $fileName to S3: $s3Key")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to upload ${imageFile.name}", e)
                    // Check if it's the placeholder ID error
                    if (e.message?.contains("YOUR_IDENTITY_POOL_ID") == true || 
                        e.message?.contains("identityPoolId") == true) {
                        Log.e(TAG, "⚠️ AWS Configuration Error: Please replace 'YOUR_IDENTITY_POOL_ID' in OralVisApplication.kt with your actual Cognito Identity Pool ID")
                    }
                    throw e
                }
            }
            Log.d(TAG, "All ${imageFiles.size} images uploaded successfully")
            
            // Get patient metadata
            val patientMetadata = PatientMetadataUtils.getPatientMetadata(context, clinicId, patientId)
            
            if (patientMetadata != null) {
                // Initialize DynamoDB client
                val dynamoDBClient = AmazonDynamoDBClient(credentialsProvider, clientConfig)
                dynamoDBClient.setRegion(region)
                
                // Initialize DynamoDB mapper
                val dynamoDBMapper = DynamoDBMapper(dynamoDBClient)
                
                // Create PatientData object with timestamp
                val patientData = PatientData().apply {
                    this.patientId = patientId.toString()
                    this.clinicId = clinicId
                    this.name = patientMetadata.name
                    this.age = patientMetadata.age
                    this.gender = patientMetadata.gender
                    this.phone = patientMetadata.phone
                    this.imagePaths = imagePaths
                    this.timestamp = patientMetadata.timestamp ?: System.currentTimeMillis()
                }
                
                // CRITICAL: Log clinicId before saving to verify it's correct
                val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(patientData.timestamp!!))
                Log.d(TAG, "=== SAVING PATIENT DATA TO DYNAMODB (QUEUE) ===")
                Log.d(TAG, "patientId=$patientId, clinicId=$clinicId, timestamp=${patientData.timestamp} ($timestampStr)")
                Log.d(TAG, "PatientData.clinicId=${patientData.clinicId}, PatientData.patientId=${patientData.patientId}")
                
                // Save to DynamoDB
                dynamoDBMapper.save(patientData)
                Log.d(TAG, "✓ Successfully saved patient data to DynamoDB: patientId=$patientId, clinicId=$clinicId, timestamp=${patientData.timestamp}")
                
                onComplete(true)
            } else {
                Log.e(TAG, "Patient metadata not found for clinicId=$clinicId, patientId=$patientId")
                throw Exception("Patient metadata not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for ${patientFolder.name}", e)
            e.printStackTrace()
            onComplete(false)
        }
    }
    
    private fun extractJsonValue(json: String, key: String): String {
        // Try to match string value first: "key": "value"
        val stringPattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val stringMatch = stringPattern.find(json)
        if (stringMatch != null) {
            return stringMatch.groupValues[1]
        }
        
        // Try to match numeric value: "key": 123
        val numberPattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        val numberMatch = numberPattern.find(json)
        if (numberMatch != null) {
            return numberMatch.groupValues[1]
        }
        
        Log.w(TAG, "Could not extract value for key: $key from JSON: $json")
        return ""
    }
    
    /**
     * Initialize background upload service (call this from Application onCreate)
     */
    fun initializeBackgroundUpload(context: Context) {
        // Process queue on app start
        processQueue(context)
        
        // Also set up periodic retry (every 5 minutes)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            while (true) {
                delay(5 * 60 * 1000) // 5 minutes
                if (isInternetAvailable(context)) {
                    processQueue(context)
                }
            }
        }
    }
}

