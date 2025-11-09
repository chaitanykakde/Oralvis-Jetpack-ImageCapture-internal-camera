package com.chaitany.oralvisjetpack.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.core.content.ContextCompat
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
    clinicId: Int,
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
    val grayButton = Color(0xFF9E9E9E)
    
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
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thin blue bar at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(primaryBlue)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // OralVis Logo
                val logoResId = context.resources.getIdentifier("oralvis_logo", "drawable", context.packageName)
                if (logoResId != 0) {
                    Image(
                        painter = painterResource(id = logoResId),
                        contentDescription = "OralVis Logo",
                        modifier = Modifier.size(60.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Instruction Text
                Text(
                    text = "Click on the image to capture your photo",
                    fontSize = 14.sp,
                    color = darkBlue,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, lightBlueBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                onRecaptureImage(stepIndex)
                            }
                            .background(Color.White)
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
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Upload Button
            Button(
                onClick = {
                    isUploading = true
                    uploadMessage = "Creating zip file..."
                    
                    // Create zip file first, then add to queue
                    scope.launch(Dispatchers.IO) {
                        try {
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
                                ContextCompat.startForegroundService(context, serviceIntent)
                                
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
                                    uploadMessage = "Error creating zip: ${error.message}"
                                }
                            }
                        } catch (e: Exception) {
                            // Update UI on main thread
                            withContext(Dispatchers.Main) {
                                isUploading = false
                                uploadMessage = "Error: ${e.message}"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = grayButton),
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
                    Text("Uploading...", color = Color.White, fontSize = 16.sp)
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

