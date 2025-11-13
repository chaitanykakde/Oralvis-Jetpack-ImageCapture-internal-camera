package com.chaitany.oralvisjetpack.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaitany.oralvisjetpack.OralVisApplication
import com.chaitany.oralvisjetpack.data.model.PatientData
import com.chaitany.oralvisjetpack.utils.ImageUtils
import com.chaitany.oralvisjetpack.utils.PatientMetadataUtils
import com.chaitany.oralvisjetpack.utils.PreferencesManager
import com.chaitany.oralvisjetpack.utils.ZipUtils
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.regions.Regions
import com.amazonaws.ClientConfiguration
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executor

data class DentalStep(
    val instruction: String,
    val referenceImageResId: Int
)

data class CaptureStep(
    val stepTitle: String,
    @DrawableRes val overlayResId: Int,
    @DrawableRes val exampleResId: Int,
    val requiresMirroring: Boolean
)

class ImageSequenceViewModel(
    private val context: Context,
    private val folderName: String,
    private val clinicId: Int,
    private val patientId: Int,
    private val excelBytes: ByteArray
) : ViewModel() {

    private val preferencesManager = PreferencesManager(context)
    
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _capturedImageUri = MutableStateFlow<Uri?>(null)
    val capturedImageUri: StateFlow<Uri?> = _capturedImageUri.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _isMirrored = MutableStateFlow(false)
    val isMirrored: StateFlow<Boolean> = _isMirrored.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isFlashEnabled = MutableStateFlow(true) // Default to ON
    val isFlashEnabled: StateFlow<Boolean> = _isFlashEnabled.asStateFlow()
    
    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()
    
    private val _currentCaptureStep = MutableStateFlow<CaptureStep?>(null)
    val currentCaptureStep: StateFlow<CaptureStep?> = _currentCaptureStep.asStateFlow()
    
    private val _allImagesCaptured = MutableStateFlow(false)
    val allImagesCaptured: StateFlow<Boolean> = _allImagesCaptured.asStateFlow()
    
    // Camera control objects - set by CameraPreview after initialization
    var cameraControl: CameraControl? = null  // ✅ Made public for torch control
        private set
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: Executor? = null

    val imageMemoryMap = mutableMapOf<String, ByteArray>()
    
    // Function to set specific step for recapturing
    fun setStepForRecapture(step: Int) {
        _currentStep.value = step
        updateCaptureStep()
        _capturedImageUri.value = null
        _capturedBitmap.value = null
        _allImagesCaptured.value = false
        _isRecaptureMode.value = true // Enable recapture mode
        // Remove the image from memory map so it can be recaptured
        val fileName = "${clinicId}_${patientId}_${step + 1}.jpg"
        imageMemoryMap.remove(fileName)
    }
    
    // Flag to track if we're in recapture mode (single image only)
    private val _isRecaptureMode = MutableStateFlow(false)
    val isRecaptureMode: StateFlow<Boolean> = _isRecaptureMode.asStateFlow()
    
    fun setRecaptureMode(enabled: Boolean) {
        _isRecaptureMode.value = enabled
    }

    val dentalSteps = listOf(
        DentalStep("1. Front teeth", getResourceId("dental_ref_1")),
        DentalStep("2. Right Side teeth", getResourceId("dental_ref_2")),
        DentalStep("3. Left Side teeth", getResourceId("dental_ref_3")),
        DentalStep("4. Upper jaw", getResourceId("dental_ref_4")),
        DentalStep("5. Lower jaw", getResourceId("dental_ref_5")),
        DentalStep("6. Right cheek", getResourceId("dental_ref_6")),
        DentalStep("7. Left cheek", getResourceId("dental_ref_7")),
        DentalStep("8. Tongue", getResourceId("dental_ref_8"))
    )
    
    private val captureSteps = listOf(
        CaptureStep("1. Front", getResourceId("overlay_1"), getResourceId("theeth1"), false),
        CaptureStep("2. Right", getResourceId("overlay_3"), getResourceId("theeth2"), true),
        CaptureStep("3. Left", getResourceId("overlay_2"), getResourceId("theeth3"), true),
        CaptureStep("4. Upper", getResourceId("overlay_4"), getResourceId("theeth4"), false),
        CaptureStep("5. Lower", getResourceId("overlay_5"), getResourceId("theeth5"), false),
        CaptureStep("6. Right Cheek", getResourceId("overlay_7"), getResourceId("theeth6"), false),
        CaptureStep("7. Left Cheek", getResourceId("overlay_6"), getResourceId("theeth7"), false),
        CaptureStep("8. Tongue", getResourceId("overlay_8"), getResourceId("theeth8"), false)
    )

    init {
        loadSavedStep()
        updateCaptureStep()
    }

    private fun loadSavedStep() {
        _currentStep.value = preferencesManager.getCurrentStep()
    }
    
    private fun updateCaptureStep() {
        val step = _currentStep.value
        if (step < captureSteps.size) {
            _currentCaptureStep.value = captureSteps[step]
        }
    }

    private fun getResourceId(name: String): Int {
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }

    // ✅ OPTIMIZED: Load bitmap on background thread to prevent UI freeze
    fun setCapturedImage(uri: Uri) {
        _capturedImageUri.value = uri
        
        viewModelScope.launch(Dispatchers.IO) {  // Background thread
            var bitmap = ImageUtils.uriToBitmapOptimized(context, uri, maxWidth = 1920)
            
            // Auto-flip if using front camera to match preview
            if (_isFrontCamera.value && bitmap != null) {
                bitmap = ImageUtils.flipBitmapHorizontally(bitmap)
            }
            
            withContext(Dispatchers.Main) {  // Switch back to main thread
                _capturedBitmap.value = bitmap
                _isMirrored.value = false
            }
        }
    }

    fun toggleMirror() {
        _isMirrored.value = !_isMirrored.value
    }

    fun retakeImage() {
        _capturedImageUri.value = null
        _capturedBitmap.value = null
        _isMirrored.value = false
    }

    // ✅ OPTIMIZED: Fast background processing with JPEG compression and memory management
    fun saveAndNext(onComplete: () -> Unit) {
        // Prevent multiple clicks
        if (_isProcessing.value) {
            return
        }
        
        // Validate that we actually have a captured image
        val bitmap = _capturedBitmap.value
        if (bitmap == null || bitmap.isRecycled) {
            _errorMessage.value = "Please capture an image first"
            return
        }
        
        viewModelScope.launch {
            try {
                // Set processing flag immediately to prevent multiple clicks
                _isProcessing.value = true
                
                // Process image on IO dispatcher for better performance
                withContext(Dispatchers.IO) {
                    val finalBitmap = if (_isMirrored.value) {
                        applyTransform(bitmap, _currentStep.value)
                    } else {
                        bitmap
                    }
                    
                    val serialNumber = _currentStep.value + 1
                    val fileName = "${clinicId}_${patientId}_$serialNumber.jpg"  // ✅ JPEG not PNG
                    // Use 75% quality for faster uploads (still good quality, ~30% smaller files)
                    val imageBytes = ImageUtils.bitmapToByteArray(finalBitmap, quality = 75)
                    imageMemoryMap[fileName] = imageBytes
                    
                    // ✅ Recycle transformed bitmap if it's a copy
                    if (_isMirrored.value && finalBitmap != bitmap) {
                        finalBitmap.recycle()
                    }
                }
                
                // ✅ Clear bitmap from memory immediately to prevent leaks
                withContext(Dispatchers.Main) {
                    _capturedBitmap.value?.recycle()
                    _capturedBitmap.value = null
                }

                // Check if in recapture mode (single image only)
                if (_isRecaptureMode.value) {
                    // Recapture mode: save this image and return to grid
                    preferencesManager.clearCurrentStep()
                    _isProcessing.value = false
                    _allImagesCaptured.value = true
                    _isRecaptureMode.value = false
                } else if (_currentStep.value < dentalSteps.size - 1) {
                    // Move to next step
                    _currentStep.value += 1
                    preferencesManager.saveCurrentStep(_currentStep.value)
                    updateCaptureStep()
                    _capturedImageUri.value = null
                    _isMirrored.value = false
                    _isProcessing.value = false
                } else {
                    // All images captured, navigate to review grid
                    preferencesManager.clearCurrentStep()
                    _isProcessing.value = false
                    _allImagesCaptured.value = true
                    // Don't call onComplete here - navigate to grid screen instead
                    // The grid screen will handle zip creation and upload
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving image: ${e.message}"
                _isProcessing.value = false
                _isLoading.value = false
            }
        }
    }

    private fun applyTransform(bitmap: Bitmap, step: Int): Bitmap {
        return when (step) {
            1, 2 -> ImageUtils.flipBitmapHorizontally(bitmap)  // Right/Left side
            3, 4 -> ImageUtils.flipBitmapVertically(bitmap)    // Upper/Lower jaw
            else -> bitmap
        }
    }

    private suspend fun zipImages(onComplete: () -> Unit) {
        try {
            val result = ZipUtils.createZipFile(
                context = context,
                folderName = folderName,
                imageFiles = imageMemoryMap,
                excelBytes = excelBytes,
                clinicId = clinicId,
                patientId = patientId
            )

            result.onSuccess {
                // After zip creation, upload to AWS
                uploadPatientDataToAWS(onComplete)
            }.onFailure { error ->
                _errorMessage.value = "Error creating zip: ${error.message}"
                _isLoading.value = false
                _isProcessing.value = false
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error creating zip: ${e.message}"
            _isLoading.value = false
            _isProcessing.value = false
        }
    }
    
    private fun uploadPatientDataToAWS(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isUploading.value = true
                _uploadSuccess.value = false
                
                // Get clinicId from active session (PreferencesManager)
                // This ensures we use the logged-in clinic's ID, not a parameter
                val activeClinicId = preferencesManager.getClinicIdInt()
                if (activeClinicId == 0) {
                    throw Exception("No active clinic session found")
                }
                
                // Get credentials provider
                val credentialsProvider = OralVisApplication.credentialsProvider
                
                // Configure client with increased timeouts for large uploads
                val clientConfig = ClientConfiguration().apply {
                    connectionTimeout = 60000 // 60 seconds
                    socketTimeout = 120000 // 120 seconds
                    maxErrorRetry = 3
                }
                
                // Initialize S3 client with region
                val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
                val s3Client = AmazonS3Client(credentialsProvider, clientConfig)
                s3Client.setRegion(region)
                val bucketName = "oralvis-patient-images"
                
                // Create map to store S3 keys
                val imagePaths = mutableMapOf<String, String>()
                
                // Upload all images in parallel for maximum speed
                val uploadJobs = imageMemoryMap.map { (fileName, imageBytes) ->
                    async {
                        try {
                            // Use activeClinicId + patientId prefix for S3 path (clinicId_patientId format)
                            val s3Key = "public/${activeClinicId}_${patientId}/$fileName"
                            
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
                            Log.d("ImageSequenceVM", "Uploaded $fileName to S3: $s3Key (${imageBytes.size / 1024}KB)")
                            Pair(fileName, s3Key)
                        } catch (e: Exception) {
                            Log.e("ImageSequenceVM", "Failed to upload $fileName", e)
                            throw e
                        }
                    }
                }
                
                // Wait for all uploads to complete
                val uploadResults = uploadJobs.awaitAll()
                uploadResults.forEach { (fileName, s3Key) ->
                    imagePaths[fileName] = s3Key
                }
                
                // Get patient metadata using activeClinicId
                val patientMetadata = PatientMetadataUtils.getPatientMetadata(context, activeClinicId, patientId)
                
                if (patientMetadata != null) {
                    // Initialize DynamoDB client with region
                    val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
                    val dynamoDBClient = com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient(
                        credentialsProvider,
                        clientConfig
                    )
                    dynamoDBClient.setRegion(region)
                    
                    // Initialize DynamoDB mapper
                    val dynamoDBMapper = DynamoDBMapper(dynamoDBClient)
                    
                    // Create PatientData object with activeClinicId from session
                    val finalTimestamp = patientMetadata.timestamp ?: System.currentTimeMillis()
                    val patientData = PatientData().apply {
                        this.patientId = patientId.toString()
                        this.clinicId = activeClinicId // Use clinicId from active session
                        this.name = patientMetadata.name
                        this.age = patientMetadata.age
                        this.gender = patientMetadata.gender
                        this.phone = patientMetadata.phone
                        this.imagePaths = imagePaths
                        this.timestamp = finalTimestamp
                    }
                    
                    // Log timestamp before saving
                    val timestampStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(finalTimestamp))
                    Log.d("ImageSequenceVM", "Saving patient data to DynamoDB: patientId=$patientId, timestamp=$finalTimestamp ($timestampStr)")
                    
                    // Save to DynamoDB
                    dynamoDBMapper.save(patientData)
                    Log.d("ImageSequenceVM", "Successfully saved patient data to DynamoDB with timestamp=$finalTimestamp")
                    
                    withContext(Dispatchers.Main) {
                        _uploadSuccess.value = true
                        onComplete()
                    }
                } else {
                    throw Exception("Patient metadata not found")
                }
                
            } catch (e: Exception) {
                Log.e("ImageSequenceVM", "AWS upload failed", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Upload Failed. Saved locally."
                    onComplete() // Still complete even if upload fails
                }
            } finally {
                _isUploading.value = false
                _isLoading.value = false
                _isProcessing.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun onBackPressed() {
        preferencesManager.clearCurrentStep()
    }

    fun canMirror(): Boolean {
        val step = _currentStep.value
        return step in listOf(1, 2, 3, 4)
    }
    
    // Called by CameraPreview after camera initialization
    fun setCameraControls(control: CameraControl, capture: ImageCapture, executor: Executor) {
        cameraControl = control
        imageCapture = capture
        cameraExecutor = executor
        Log.d("ImageSequenceVM", "Camera controls initialized")
        
        // Enable flash by default when camera starts
        if (_isFlashEnabled.value) {
            try {
                control.enableTorch(true)
                Log.d("ImageSequenceVM", "Flash enabled by default")
            } catch (e: Exception) {
                Log.e("ImageSequenceVM", "Failed to enable flash by default", e)
            }
        }
    }
    
    // Camera control methods
    fun onCaptureClicked() {
        val capture = imageCapture
        val executor = cameraExecutor
        
        if (capture == null || executor == null) {
            _errorMessage.value = "Camera not ready"
            Log.e("ImageSequenceVM", "Capture attempted but camera not initialized")
            return
        }
        
        val photoFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    setCapturedImage(uri)
                    Log.d("ImageSequenceVM", "Image captured successfully")
                }
                
                override fun onError(exception: ImageCaptureException) {
                    _errorMessage.value = "Capture failed: ${exception.message}"
                    Log.e("ImageSequenceVM", "Capture failed", exception)
                }
            }
        )
    }
    
    fun onFlashClicked() {
        _isFlashEnabled.value = !_isFlashEnabled.value
        
        // Enable/disable torch immediately
        cameraControl?.enableTorch(_isFlashEnabled.value)
    }
    
    fun onFlipCameraClicked() {
        _isFrontCamera.value = !_isFrontCamera.value
        // Camera selector will be updated when camera rebinds
    }
    
    override fun onCleared() {
        super.onCleared()
        cameraControl = null
        imageCapture = null
        cameraExecutor = null
    }
}

