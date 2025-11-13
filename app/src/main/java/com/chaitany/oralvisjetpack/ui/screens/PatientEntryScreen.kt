package com.chaitany.oralvisjetpack.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaitany.oralvisjetpack.data.database.OralVisDatabase
import com.chaitany.oralvisjetpack.data.repository.PatientCounterRepository
import com.chaitany.oralvisjetpack.utils.PatientMetadataUtils
import com.chaitany.oralvisjetpack.utils.PermissionUtils
import com.chaitany.oralvisjetpack.viewmodel.PatientEntryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PatientEntryScreen(
    clinicId: Int,
    onNavigateToImageCapture: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val database = remember { OralVisDatabase.getDatabase(context) }
    val repository = remember { PatientCounterRepository(database.patientCounterDao()) }
    
    val viewModel: PatientEntryViewModel = remember {
        PatientEntryViewModel(repository)
    }

    val patientName by viewModel.patientName.collectAsState()
    val age by viewModel.age.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val patientCounter by viewModel.patientCounter.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Reload patient counter when screen is displayed to ensure it's up to date
    LaunchedEffect(Unit) {
        viewModel.loadPatientCounter()
    }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingNavigation by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // Permission state
    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionUtils.getAllPermissions()
    )

    // Manage storage permission launcher (Android 11+)
    val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionUtils.hasManageStoragePermission()) {
            pendingNavigation?.let { (folder, patientId) ->
                onNavigateToImageCapture(folder, patientId)
                pendingNavigation = null
            }
        } else {
            showPermissionDialog = true
        }
    }

    // Colors matching the design
    val primaryBlue = Color(0xFF4A8BBF)
    val darkBlue = Color(0xFF1E3A5F)
    val lightBlueBorder = Color(0xFFE3F2FD)
    
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Small Oralvis Horizontal Logo at top
            val smallLogoResId = context.resources.getIdentifier("oralvissmalllogo", "drawable", context.packageName)
            if (smallLogoResId != 0) {
                Image(
                    painter = painterResource(id = smallLogoResId),
                    contentDescription = "Oralvis Logo",
                    modifier = Modifier.size(80.dp)
                )
            } else {
                Text(
                    text = "Oralvis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main Title
            Text(
                text = "Patient data collection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = darkBlue,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructional Text
            Text(
                text = "Please enter the patient's details to help us\ncreate an accurate dental report and\npersonalized treatment plan",
                fontSize = 14.sp,
                color = darkBlue,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Patient ID Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = primaryBlue.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Patient ID:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = darkBlue
                    )
                    Text(
                        text = "#${patientCounter}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryBlue
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Patient Name Field
            OutlinedTextField(
                value = patientName,
                onValueChange = { viewModel.updatePatientName(it) },
                placeholder = { Text("Patient name", color = Color.Black.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = lightBlueBorder,
                    focusedBorderColor = primaryBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                enabled = !isLoading,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Age Field
            OutlinedTextField(
                value = age,
                onValueChange = { viewModel.updateAge(it) },
                placeholder = { Text("Age", color = Color.Black.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = lightBlueBorder,
                    focusedBorderColor = primaryBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                enabled = !isLoading,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Gender Dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = if (gender.isEmpty()) "" else gender,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Gender", color = Color.Black.copy(alpha = 0.6f)) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = darkBlue
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = lightBlueBorder,
                        focusedBorderColor = primaryBlue,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                    enabled = !isLoading
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Male", "Female", "Other").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = darkBlue) },
                            onClick = {
                                viewModel.updateGender(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Phone Number Field (Optional)
            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.updatePhone(it) },
                placeholder = { Text("Phone number (Optional)", color = Color.Black.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = lightBlueBorder,
                    focusedBorderColor = primaryBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedTextColor = Color.Black,
                    focusedTextColor = Color.Black
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                enabled = !isLoading,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Save and Next Button
            Button(
                onClick = {
                    viewModel.savePatientData { folderName, patientId, excelBytes, timestamp ->
                        // Store excelBytes in shared preferences
                        context.getSharedPreferences("patient_data", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString("excel_bytes_${clinicId}_$patientId", 
                                android.util.Base64.encodeToString(excelBytes, android.util.Base64.DEFAULT))
                            .apply()
                        
                        // Store patient metadata for AWS upload with timestamp
                        PatientMetadataUtils.savePatientMetadata(
                            context = context,
                            clinicId = clinicId,
                            patientId = patientId,
                            name = patientName,
                            age = age,
                            gender = gender,
                            phone = phone,
                            timestamp = timestamp
                        )
                        
                        // Check permissions before navigating
                        if (permissionsState.allPermissionsGranted) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
                                !PermissionUtils.hasManageStoragePermission()) {
                                pendingNavigation = Pair(folderName, patientId)
                                PermissionUtils.requestManageStoragePermission(context, manageStorageLauncher)
                            } else {
                                onNavigateToImageCapture(folderName, patientId)
                            }
                        } else {
                            pendingNavigation = Pair(folderName, patientId)
                            permissionsState.launchMultiplePermissionRequest()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Save and Next",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearError()
                }
            }
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissions Required") },
            text = { Text("Storage and camera permissions are required to capture and save images.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Handle permission result
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted && pendingNavigation != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (PermissionUtils.hasManageStoragePermission()) {
                    pendingNavigation?.let { (folder, patientId) ->
                        onNavigateToImageCapture(folder, patientId)
                        pendingNavigation = null
                    }
                } else {
                    PermissionUtils.requestManageStoragePermission(context, manageStorageLauncher)
                }
            } else {
                pendingNavigation?.let { (folder, patientId) ->
                    onNavigateToImageCapture(folder, patientId)
                    pendingNavigation = null
                }
            }
        }
    }
}

