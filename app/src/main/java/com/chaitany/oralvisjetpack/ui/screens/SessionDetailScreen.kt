package com.chaitany.oralvisjetpack.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.chaitany.oralvisjetpack.viewmodel.SessionDetailViewModel
import com.chaitany.oralvisjetpack.utils.ImageUtils
import com.chaitany.oralvisjetpack.utils.SessionDownloadUtils
import com.chaitany.oralvisjetpack.OralVisApplication
import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    patientId: String,
    clinicId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SessionDetailViewModel = remember { SessionDetailViewModel(context) }
    
    val patient by viewModel.patient.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    LaunchedEffect(patientId) {
        viewModel.loadPatient(patientId)
    }
    val scope = rememberCoroutineScope()
    val primaryBlue = Color(0xFF4A8BBF)
    val darkBlue = Color(0xFF1E3A5F)
    
    val imageBitmaps = remember { mutableStateMapOf<String, Bitmap?>() }
    val loadingImages = remember { mutableStateSetOf<String>() }
    var selectedImageForPreview by remember { mutableStateOf<Pair<String, Bitmap?>?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    
    // Load images from S3 URLs
    LaunchedEffect(patient?.imagePaths) {
        val imagePaths = patient?.imagePaths ?: emptyMap()
        imagePaths.forEach { (fileName, s3Path) ->
            if (!imageBitmaps.containsKey(fileName) && !loadingImages.contains(fileName)) {
                loadingImages.add(fileName)
                scope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            loadImageFromS3(s3Path)
                        }
                        imageBitmaps[fileName] = bitmap
                    } catch (e: Exception) {
                        android.util.Log.e("SessionDetail", "Failed to load image: $fileName", e)
                        imageBitmaps[fileName] = null
                    } finally {
                        loadingImages.remove(fileName)
                    }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = patient?.name ?: "Patient Details",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Download Button
                    IconButton(
                        onClick = {
                            val imagePaths = patient?.imagePaths ?: emptyMap()
                            if (imagePaths.isNotEmpty()) {
                                isDownloading = true
                                downloadMessage = "Downloading images..."
                                scope.launch {
                                    val result = SessionDownloadUtils.downloadAndCreateZip(
                                        context = context,
                                        clinicId = clinicId,
                                        patientId = patientId,
                                        imagePaths = imagePaths
                                    )
                                    withContext(Dispatchers.Main) {
                                        isDownloading = false
                                        result.onSuccess { zipFile ->
                                            downloadMessage = "Downloaded to: Documents/myapp/${zipFile.name}\nFull path: ${zipFile.absolutePath}"
                                        }.onFailure { error ->
                                            downloadMessage = "Download failed: ${error.message}"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading && (patient?.imagePaths?.isNotEmpty() == true)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = Color.White
                            )
                        }
                    }
                    
                    // Share Button
                    IconButton(
                        onClick = {
                            val imagePaths = patient?.imagePaths ?: emptyMap()
                            if (imagePaths.isNotEmpty()) {
                                isDownloading = true
                                downloadMessage = "Preparing share..."
                                scope.launch {
                                    val result = SessionDownloadUtils.downloadAndCreateZip(
                                        context = context,
                                        clinicId = clinicId,
                                        patientId = patientId,
                                        imagePaths = imagePaths
                                    )
                                    withContext(Dispatchers.Main) {
                                        isDownloading = false
                                        result.onSuccess { zipFile ->
                                            shareZipFile(context, zipFile)
                                            downloadMessage = null
                                        }.onFailure { error ->
                                            downloadMessage = "Share failed: ${error.message}"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading && (patient?.imagePaths?.isNotEmpty() == true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryBlue
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryBlue)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "Error loading patient",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(
                        onClick = { viewModel.loadPatient(patientId) },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else if (patient == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Patient not found",
                    color = darkBlue
                )
            }
        } else {
            val currentPatient = patient
            if (currentPatient != null) {
                val currentImagePaths = currentPatient.imagePaths ?: emptyMap()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Patient Information Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Patient Information",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = darkBlue
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                PatientInfoRow("Name", currentPatient.name ?: "N/A", darkBlue)
                                PatientInfoRow("Age", currentPatient.age ?: "N/A", darkBlue)
                                PatientInfoRow("Gender", currentPatient.gender ?: "N/A", darkBlue)
                                PatientInfoRow("Phone", currentPatient.phone ?: "N/A", darkBlue)
                                PatientInfoRow("Patient ID", currentPatient.patientId ?: "N/A", darkBlue)
                                PatientInfoRow("Clinic ID", currentPatient.clinicId?.toString() ?: "N/A", darkBlue)
                            }
                        }
                        
                        // Download Message
                        downloadMessage?.let { message ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (message.contains("failed", ignoreCase = true)) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    }
                                )
                            ) {
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 12.sp,
                                    color = if (message.contains("failed", ignoreCase = true)) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        Color(0xFF4CAF50)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Images Section
                        Text(
                            text = "Captured Images (${currentImagePaths.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkBlue,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        if (currentImagePaths.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No images available",
                                    color = darkBlue.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(currentImagePaths.toList().sortedBy { it.first }) { (fileName, s3Path) ->
                                    ImageCard(
                                        fileName = fileName,
                                        bitmap = imageBitmaps[fileName],
                                        isLoading = loadingImages.contains(fileName),
                                        primaryBlue = primaryBlue,
                                        onClick = {
                                            selectedImageForPreview = Pair(fileName, imageBitmaps[fileName])
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Full-screen Image Preview Dialog
        selectedImageForPreview?.let { (fileName, bitmap) ->
            FullScreenImageDialog(
                fileName = fileName,
                bitmap = bitmap,
                onDismiss = { selectedImageForPreview = null }
            )
        }
    }
}

@Composable
fun PatientInfoRow(label: String, value: String, darkBlue: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = darkBlue.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = darkBlue,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun ImageCard(
    fileName: String,
    bitmap: Bitmap?,
    isLoading: Boolean,
    primaryBlue: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = primaryBlue
                )
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "Failed to load",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Image number overlay
            Text(
                text = fileName.substringBeforeLast("_").substringAfterLast("_") ?: fileName,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(
                        color = primaryBlue.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(bottomEnd = 12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun FullScreenImageDialog(
    fileName: String,
    bitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Image not available",
                        color = Color.White
                    )
                }
            }
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // File name at bottom
            Text(
                text = fileName,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            )
        }
    }
}

suspend fun loadImageFromS3(s3Path: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val bucketName = "oralvis-patient-images"
            
            android.util.Log.d("SessionDetail", "Loading image from S3: $s3Path")
            
            val credentialsProvider = OralVisApplication.credentialsProvider
            
            val clientConfig = ClientConfiguration().apply {
                connectionTimeout = 30000
                socketTimeout = 60000
                maxErrorRetry = 3
            }
            
            val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
            val s3Client = AmazonS3Client(credentialsProvider, clientConfig)
            s3Client.setRegion(region)
            
            val s3Object = s3Client.getObject(bucketName, s3Path)
            val bytes = s3Object.objectContent.use { inputStream ->
                inputStream.readBytes()
            }
            
            android.util.Log.d("SessionDetail", "Successfully loaded image from S3: $s3Path (${bytes.size / 1024}KB)")
            
            ImageUtils.byteArrayToBitmap(bytes, maxWidth = 800)
        } catch (e: Exception) {
            android.util.Log.e("SessionDetail", "Error loading image from S3: $s3Path", e)
            null
        }
    }
}

fun shareZipFile(context: android.content.Context, zipFile: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            zipFile
        )
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/zip"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share ZIP file"))
    } catch (e: Exception) {
        android.util.Log.e("SessionDetail", "Error sharing file", e)
    }
}
