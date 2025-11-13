package com.chaitany.oralvisjetpack.ui.screens

import android.Manifest
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chaitany.oralvisjetpack.viewmodel.ImageSequenceViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImageSequenceScreen(
    folderName: String,
    clinicId: Int,
    patientId: Int,
    onComplete: () -> Unit,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    
    // Retrieve excelBytes from SharedPreferences
    val excelBytes = remember {
        val prefs = context.getSharedPreferences("patient_data", android.content.Context.MODE_PRIVATE)
        val encodedBytes = prefs.getString("excel_bytes_${clinicId}_$patientId", null)
        if (encodedBytes != null) {
            android.util.Base64.decode(encodedBytes, android.util.Base64.DEFAULT)
        } else {
            ByteArray(0)
        }
    }
    
    val viewModel: ImageSequenceViewModel = remember {
        ImageSequenceViewModel(context, folderName, clinicId, patientId, excelBytes)
    }

    // Collect state
    val capturedImageUri by viewModel.capturedImageUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val allImagesCaptured by viewModel.allImagesCaptured.collectAsState()
    val isRecaptureMode by viewModel.isRecaptureMode.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }
    
    // Camera permission handling
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Back button handler
    BackHandler {
        showExitDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Check permission first
        if (!cameraPermissionState.status.isGranted) {
            // Permission not granted - show request UI
            PermissionRequestScreen(
                onRequestPermission = {
                    cameraPermissionState.launchPermissionRequest()
                }
            )
        } else {
            // Permission granted - show camera, review, or grid screen
            if (allImagesCaptured) {
                // Show grid screen with all captured images
                ImageReviewGridScreen(
                    folderName = folderName,
                    clinicId = clinicId,
                    patientId = patientId,
                    imageFiles = viewModel.imageMemoryMap,
                    excelBytes = excelBytes,
                    onRecaptureImage = { stepIndex ->
                        viewModel.setStepForRecapture(stepIndex)
                    },
                    onComplete = onComplete
                )
            } else if (capturedImageUri == null) {
                // Camera mode
                CameraCaptureLayout(viewModel = viewModel)
            } else {
                // Review mode
                ImageReviewLayout(
                    viewModel = viewModel,
                    onComplete = {
                        // After saving, check if all images are captured
                        if (viewModel.currentStep.value >= 7) {
                            // All images captured, will show grid on next recomposition
                        } else {
                            // Continue to next step
                        }
                    }
                )
            }
        }

        // Loading Overlay
        if (isLoading || isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3FBF8B))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isUploading) "Uploading to AWS..." else "Processing...",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Error message
        errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = message)
            }
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearError()
            }
        }
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Do you want to exit?") },
            text = { Text("Your current progress won't be saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onBackPressed()
                        onBackPressed()
                    }
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Camera Permission",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF3FBF8B)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Permission Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "This app needs access to your camera to capture dental images.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FBF8B)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Grant Permission", fontSize = 16.sp)
        }
    }
}

@Composable
fun CameraCaptureLayout(viewModel: ImageSequenceViewModel) {
    val captureStep by viewModel.currentCaptureStep.collectAsState()
    val isFlashEnabled by viewModel.isFlashEnabled.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val context = LocalContext.current
    
    // Colors matching the design
    val primaryBlue = Color(0xFF4A8BBF)
    val darkBlue = Color(0xFF1E3A5F)
    val lightBlueBorder = Color(0xFFE3F2FD)
    
    // 80/20 layout with white background and blue border (left, right, bottom only)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {
        // Border on left, right, and bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 5.dp, color = lightBlueBorder)
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section - Minimal, integrated into layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step Name instead of Logo
                val currentStep by viewModel.currentStep.collectAsState()
                val dentalSteps = viewModel.dentalSteps
                val stepName = if (currentStep < dentalSteps.size) {
                    dentalSteps[currentStep].instruction
                } else {
                    ""
                }
                
                Text(
                    text = stepName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Instruction Text
                Text(
                    text = "Kindly position your teeth according to the outline displayed below",
                    fontSize = 13.sp,
                    color = darkBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 18.sp
                )
            }
            
            // Preview Section (80% of remaining space)
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                // Camera Preview as bottom layer
                CameraPreview(
                    viewModel = viewModel,
                    isFlashEnabled = isFlashEnabled,
                    isFrontCamera = isFrontCamera,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay Image in center - teeth outline
                captureStep?.overlayResId?.let { overlayId ->
                    Image(
                        painter = painterResource(id = overlayId),
                        contentDescription = "Camera Overlay",
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Guide Image in top right corner - teeth capturing guide
                captureStep?.exampleResId?.let { guideId ->
                    Image(
                        painter = painterResource(id = guideId),
                        contentDescription = "Teeth Guide",
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Controls Section (20% of remaining space)
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash/Torch Button (Left) - Lightning bolt icon
                    IconButton(
                        onClick = { viewModel.onFlashClicked() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (isFlashEnabled) primaryBlue else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = primaryBlue,
                                shape = CircleShape
                            )
                    ) {
                        // Using text-based lightning bolt symbol
                        Text(
                            text = "⚡",
                            fontSize = 24.sp,
                            color = if (isFlashEnabled) Color.White else primaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Shutter Button (Center) - Blue circle with white center
                    IconButton(
                        onClick = { viewModel.onCaptureClicked() },
                        modifier = Modifier
                            .size(72.dp)
                            .background(primaryBlue, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color.White, CircleShape)
                        )
                    }

                    // Flip Camera Button (Right) - Camera switch icon
                    IconButton(
                        onClick = { viewModel.onFlipCameraClicked() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Transparent, CircleShape)
                            .border(
                                width = 2.dp,
                                color = primaryBlue,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cameraswitch,
                            contentDescription = "Flip Camera",
                            tint = primaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    viewModel: ImageSequenceViewModel,
    isFlashEnabled: Boolean,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    // ✅ Remember PreviewView to avoid recreating it
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    
    // ✅ OPTIMIZED: Only rebind camera when isFrontCamera changes (not on every recomposition)
    DisposableEffect(isFrontCamera) {
        val cameraProvider = cameraProviderFuture.get()
        
        // Select camera
        val cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        // Build preview
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        
        // Build image capture (no flash mode - using torch instead)
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
        
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            
            // Pass camera controls to ViewModel
            val executor = ContextCompat.getMainExecutor(context)
            viewModel.setCameraControls(camera.cameraControl, imageCapture, executor)
            
            // Enable torch if it was already on
            if (isFlashEnabled) {
                camera.cameraControl.enableTorch(true)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("CameraPreview", "Failed to bind camera", e)
        }
        
        onDispose {
            cameraProvider.unbindAll()
        }
    }
    
    // ✅ Handle torch state changes without rebinding camera
    LaunchedEffect(isFlashEnabled) {
        viewModel.cameraControl?.enableTorch(isFlashEnabled)
    }
    
    // ✅ AndroidView only displays the preview, doesn't rebind camera
    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

@Composable
fun ImageReviewLayout(
    viewModel: ImageSequenceViewModel,
    onComplete: () -> Unit
) {
    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val isMirrored by viewModel.isMirrored.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val context = LocalContext.current
    
    // Colors matching the design
    val primaryBlue = Color(0xFF4A8BBF)
    val darkBlue = Color(0xFF1E3A5F)
    val lightBlueBorder = Color(0xFFE3F2FD)
    
    // Border on left, right, and bottom only
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {
        // Border decoration
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(width = 5.dp, color = lightBlueBorder)
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section - Minimal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step Name instead of Logo
                val dentalSteps = viewModel.dentalSteps
                val stepName = if (currentStep < dentalSteps.size) {
                    dentalSteps[currentStep].instruction
                } else {
                    ""
                }
                
                Text(
                    text = stepName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Instruction Text
                Text(
                    text = "Kindly position your teeth according to the outline displayed below",
                    fontSize = 13.sp,
                    color = darkBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    lineHeight = 18.sp
                )
            }
            
            // Captured image preview (65-70% of screen) with rounded corners
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                capturedBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .graphicsLayer(
                                scaleX = if (isMirrored && currentStep in listOf(1, 2)) -1f else 1f,
                                scaleY = if (isMirrored && currentStep in listOf(3, 4)) -1f else 1f
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Action buttons (15-20% of screen) - Stacked vertically
            Column(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save & Next Button (Blue, solid) - Top button
                Button(
                    onClick = { viewModel.saveAndNext(onComplete) },
                    enabled = !isProcessing && capturedBitmap != null && !capturedBitmap!!.isRecycled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...", color = Color.White, fontSize = 16.sp)
                    } else {
                        Text("Save and Next", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                // Retake Button (White with blue border) - Bottom button
                OutlinedButton(
                    onClick = { viewModel.retakeImage() },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = primaryBlue,
                        containerColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, primaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retake", color = primaryBlue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
