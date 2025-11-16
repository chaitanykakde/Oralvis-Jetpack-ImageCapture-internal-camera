package com.chaitany.oralvisjetpack.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.chaitany.oralvisjetpack.service.UploadService
import com.chaitany.oralvisjetpack.utils.ImageUtils
import com.chaitany.oralvisjetpack.utils.UploadQueueManager
import com.chaitany.oralvisjetpack.utils.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File

@Composable
fun ImageReviewGridScreen(
    folderName: String,
    clinicId: String,
    patientId: Int,
    imageFiles: Map<String, ByteArray>,
    excelBytes: ByteArray,
    onRecaptureImage: (Int) -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    
    // Colors matching the design
    val primaryBlue = Color(0xFF4A8BBF)
    val darkBlue = Color(0xFF1E3A5F)
    val lightBlueBorder = Color(0xFFE3F2FD)
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    // Convert image bytes to bitmaps for display
    val imageBitmaps = remember(imageFiles) {
        imageFiles.mapValues { (_, bytes) ->
            ImageUtils.byteArrayToBitmap(bytes, maxWidth = 400)
        }
    }
    
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Section - Only instruction text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Instruction Text
                Text(
                    text = "Click on the image to capture your photo",
                    fontSize = 16.sp,
                    color = primaryBlue,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Image Grid - 2 columns, 4 rows (8 images total)
            val imageList = (1..8).map { index ->
                val fileName = "${clinicId}_${patientId}_$index.jpg"
                imageBitmaps[fileName]
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(8) { index ->
                    val bitmap = imageList[index]
                    val stepIndex = index
                    
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, lightBlueBorder, RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                onRecaptureImage(stepIndex)
                            }
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Image ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Placeholder if image not found
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Image ${index + 1}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        // Recapture icon in bottom-right corner
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(primaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Recapture",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Upload Button - Centered, Blue with Shadow
            Button(
                onClick = {
                    isUploading = true
                    uploadMessage = "Creating zip file..."
                    
                    // Create zip file first, then add to queue
                    scope.launch(Dispatchers.IO) {
                        try {
                            // Check notification permission for Android 13+ (required for notifications)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasNotificationPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (!hasNotificationPermission) {
                                    android.util.Log.w("ImageReviewGrid", "POST_NOTIFICATIONS permission not granted - notifications may not show")
                                    // Continue anyway, but log warning
                                }
                            }
                            
                            // Check storage permissions before creating zip
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                if (!android.os.Environment.isExternalStorageManager()) {
                                    withContext(Dispatchers.Main) {
                                        isUploading = false
                                        uploadMessage = "Storage permission required. Please grant access in settings."
                                    }
                                    return@launch
                                }
                            }
                            
                            val zipResult = ZipUtils.createZipFile(
                                context = context,
                                folderName = folderName,
                                imageFiles = imageFiles,
                                excelBytes = excelBytes,
                                clinicId = clinicId,
                                patientId = patientId
                            )
                            
                            zipResult.onSuccess {
                                // Zip created successfully, now add to queue
                                UploadQueueManager.addToQueue(
                                    context = context,
                                    folderName = folderName,
                                    clinicId = clinicId,
                                    patientId = patientId,
                                    imageFiles = imageFiles,
                                    excelBytes = excelBytes
                                )
                                
                                // Start foreground service for background upload with notification
                                val serviceIntent = Intent(context, UploadService::class.java).apply {
                                    action = UploadService.ACTION_START_UPLOAD
                                }
                                try {
                                    ContextCompat.startForegroundService(context, serviceIntent)
                                } catch (e: Exception) {
                                    android.util.Log.e("ImageReviewGrid", "Failed to start upload service", e)
                                    withContext(Dispatchers.Main) {
                                        isUploading = false
                                        uploadMessage = "Upload service error: ${e.message}"
                                    }
                                    return@launch
                                }
                                
                                // Update UI on main thread
                                withContext(Dispatchers.Main) {
                                    isUploading = false
                                    uploadMessage = "Upload started in background. Check notifications for progress."
                                    
                                    // Complete immediately - upload continues in background
                                    onComplete()
                                }
                            }.onFailure { error ->
                                // Update UI on main thread
                                withContext(Dispatchers.Main) {
                                    isUploading = false
                                    uploadMessage = "Error creating zip: ${error.message}. Please check storage permissions."
                                }
                            }
                        } catch (e: Exception) {
                            // Update UI on main thread
                            android.util.Log.e("ImageReviewGrid", "Upload error", e)
                            withContext(Dispatchers.Main) {
                                isUploading = false
                                uploadMessage = "Error: ${e.message}. Please check storage permissions."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                } else {
                    Text("Upload", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
            
            uploadMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 12.sp,
                    color = if (message.contains("success", ignoreCase = true)) Color(0xFF4CAF50) else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

