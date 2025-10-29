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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    val errorMessage by viewModel.errorMessage.collectAsState()

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
            // Permission granted - show camera or review screen
            if (capturedImageUri == null) {
                // Camera mode - 40/40/20 layout
                CameraCaptureLayout(viewModel = viewModel)
            } else {
                // Review mode
                ImageReviewLayout(
                    viewModel = viewModel,
                    onComplete = onComplete
                )
            }
        }

        // Loading Overlay
        if (isLoading) {
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
                        Text("Processing...", fontWeight = FontWeight.Bold, color = Color.Black)
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
    
    // 40/40/20 layout with black background
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        // Top 40% - Camera Preview Card
        Card(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            CameraPreview(
                viewModel = viewModel,
                isFlashEnabled = isFlashEnabled,
                isFrontCamera = isFrontCamera,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Middle 40% - Example Image Card
        Card(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            captureStep?.exampleResId?.let { exampleId ->
                Image(
                    painter = painterResource(id = exampleId),
                    contentDescription = "Example Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        // Bottom 20% - Controls
        Box(
            modifier = Modifier
                .weight(0.2f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flip Camera Button
                IconButton(
                    onClick = { viewModel.onFlipCameraClicked() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Shutter Button (large white circle with black center - classic camera button)
                IconButton(
                    onClick = { viewModel.onCaptureClicked() },
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Transparent, CircleShape)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black, CircleShape)
                        )
                    }
                }

                // Flash/Torch Button
                IconButton(
                    onClick = { viewModel.onFlashClicked() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isFlashEnabled) Color.Yellow else Color.White.copy(alpha = 0.3f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Flash",
                        tint = if (isFlashEnabled) Color.Black else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Captured image preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            capturedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .graphicsLayer(
                            scaleX = if (isMirrored && currentStep in listOf(1, 2)) -1f else 1f,
                            scaleY = if (isMirrored && currentStep in listOf(3, 4)) -1f else 1f
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Retake Button
                OutlinedButton(
                    onClick = { viewModel.retakeImage() },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retake", tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retake", color = Color.White)
                }

                // Mirror Button (conditional)
                if (viewModel.canMirror()) {
                    OutlinedButton(
                        onClick = { viewModel.toggleMirror() },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Mirror", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isMirrored) "Original" else "Mirror", color = Color.White)
                    }
                }

                // Save & Next Button
                Button(
                    onClick = { viewModel.saveAndNext(onComplete) },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FBF8B))
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Next")
                    }
                }
            }
        }
    }
}
