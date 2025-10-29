package com.chaitany.oralvisjetpack.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaitany.oralvisjetpack.utils.ImageUtils
import com.chaitany.oralvisjetpack.utils.PreferencesManager
import com.chaitany.oralvisjetpack.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
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
    val overlayResId: Int?,
    val exampleResId: Int
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

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isFlashEnabled = MutableStateFlow(false)
    val isFlashEnabled: StateFlow<Boolean> = _isFlashEnabled.asStateFlow()
    
    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()
    
    private val _currentCaptureStep = MutableStateFlow<CaptureStep?>(null)
    val currentCaptureStep: StateFlow<CaptureStep?> = _currentCaptureStep.asStateFlow()
    
    // Camera control objects - set by CameraPreview after initialization
    var cameraControl: CameraControl? = null  // ✅ Made public for torch control
        private set
    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: Executor? = null

    private val imageMemoryMap = mutableMapOf<String, ByteArray>()

    val dentalSteps = listOf(
        DentalStep("Front teeth (closed bite)", getResourceId("dental_ref_1")),
        DentalStep("Right side front teeth (closed bite)", getResourceId("dental_ref_2")),
        DentalStep("Left side front teeth (closed bite)", getResourceId("dental_ref_3")),
        DentalStep("Upper jaw (maxillary occlusal view)", getResourceId("dental_ref_4")),
        DentalStep("Lower jaw (mandibular occlusal view)", getResourceId("dental_ref_5")),
        DentalStep("Right cheek (buccal view)", getResourceId("dental_ref_6")),
        DentalStep("Left cheek (buccal view)", getResourceId("dental_ref_7")),
        DentalStep("Tongue (visible area)", getResourceId("dental_ref_8"))
    )
    
    private val captureSteps = listOf(
        CaptureStep("Capture front teeth (closed bite)", null, getResourceId("dental_ref_1")),
        CaptureStep("Capture right side (closed bite)", null, getResourceId("dental_ref_2")),
        CaptureStep("Capture left side (closed bite)", null, getResourceId("dental_ref_3")),
        CaptureStep("Capture upper jaw view", null, getResourceId("dental_ref_4")),
        CaptureStep("Capture lower jaw view", null, getResourceId("dental_ref_5")),
        CaptureStep("Capture right cheek", null, getResourceId("dental_ref_6")),
        CaptureStep("Capture left cheek", null, getResourceId("dental_ref_7")),
        CaptureStep("Capture tongue area", null, getResourceId("dental_ref_8"))
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
        
        viewModelScope.launch {
            try {
                // Set processing flag immediately to prevent multiple clicks
                _isProcessing.value = true
                
                // Process image on IO dispatcher for better performance
                val bitmap = _capturedBitmap.value
                if (bitmap != null) {
                    withContext(Dispatchers.IO) {
                        val finalBitmap = if (_isMirrored.value) {
                            applyTransform(bitmap, _currentStep.value)
                        } else {
                            bitmap
                        }
                        
                        val serialNumber = _currentStep.value + 1
                        val fileName = "${clinicId}_${patientId}_$serialNumber.jpg"  // ✅ JPEG not PNG
                        val imageBytes = ImageUtils.bitmapToByteArray(finalBitmap, quality = 85)  // ✅ JPEG quality 85
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
                }

                if (_currentStep.value < dentalSteps.size - 1) {
                    // Move to next step
                    _currentStep.value += 1
                    preferencesManager.saveCurrentStep(_currentStep.value)
                    updateCaptureStep()
                    _capturedImageUri.value = null
                    _isMirrored.value = false
                    _isProcessing.value = false
                } else {
                    // All images captured, create zip
                    preferencesManager.clearCurrentStep()
                    _isLoading.value = true
                    zipImages(onComplete)
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
                onComplete()
            }.onFailure { error ->
                _errorMessage.value = "Error creating zip: ${error.message}"
            }
        } finally {
            _isLoading.value = false
            _isProcessing.value = false
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

