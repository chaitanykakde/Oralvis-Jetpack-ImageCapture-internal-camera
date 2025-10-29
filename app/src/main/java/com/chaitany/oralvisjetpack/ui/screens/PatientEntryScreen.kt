package com.chaitany.oralvisjetpack.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaitany.oralvisjetpack.data.database.OralVisDatabase
import com.chaitany.oralvisjetpack.data.repository.PatientCounterRepository
import com.chaitany.oralvisjetpack.utils.PermissionUtils
import com.chaitany.oralvisjetpack.viewmodel.PatientEntryViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PatientEntryScreen(
    clinicName: String,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Data Collection") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            // Clinic and Patient ID display
            Text(
                text = "Clinic ID: $clinicId",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Patient ID: ${patientCounter.toString().padStart(3, '0')}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = patientName,
                onValueChange = { viewModel.updatePatientName(it) },
                label = { Text("Patient Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { viewModel.updateAge(it) },
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gender Dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !isLoading
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Male", "Female", "Other").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateGender(option)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.updatePhone(it) },
                label = { Text("Phone Number (optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.savePatientData { folderName, patientId, excelBytes ->
                        // Store excelBytes in shared preferences or pass through ViewModel
                        context.getSharedPreferences("patient_data", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString("excel_bytes_${clinicId}_$patientId", 
                                android.util.Base64.encodeToString(excelBytes, android.util.Base64.DEFAULT))
                            .apply()
                        
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
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save and Next")
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

