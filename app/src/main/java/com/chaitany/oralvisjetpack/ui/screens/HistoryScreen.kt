package com.chaitany.oralvisjetpack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.chaitany.oralvisjetpack.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    clinicId: Int,
    onBack: () -> Unit,
    onSessionClick: (patientId: String, clinicId: Int) -> Unit
) {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = remember { HistoryViewModel(context) }
    
    val patients by viewModel.patients.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Colors matching the design
    val primaryBlue = Color(0xFF4A8BBF)
    val darkBlue = Color(0xFF1E3A5F)
    val lightBlueBorder = Color(0xFFE3F2FD)
    
    LaunchedEffect(clinicId) {
        viewModel.loadPatientsForClinic(clinicId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Patient History",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryBlue
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryBlue)
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "Error loading history",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadPatientsForClinic(clinicId) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else if (patients.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No patient records found",
                        color = darkBlue,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(patients) { patient ->
                        SessionCard(
                            patient = patient,
                            onClick = {
                                patient.patientId?.let { patientId ->
                                    onSessionClick(patientId, clinicId)
                                }
                            },
                            primaryBlue = primaryBlue,
                            darkBlue = darkBlue,
                            lightBlueBorder = lightBlueBorder
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(
    patient: com.chaitany.oralvisjetpack.data.model.PatientData,
    onClick: () -> Unit,
    primaryBlue: Color,
    darkBlue: Color,
    lightBlueBorder: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                text = patient.name ?: "Unknown Patient",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = darkBlue
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (patient.age != null) {
                        Text(
                            text = "Age: ${patient.age}",
                            fontSize = 14.sp,
                            color = darkBlue.copy(alpha = 0.7f)
                        )
                    }
                    if (patient.gender != null) {
                        Text(
                            text = "Gender: ${patient.gender}",
                            fontSize = 14.sp,
                            color = darkBlue.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Patient ID: ${patient.patientId ?: "N/A"}",
                        fontSize = 12.sp,
                        color = darkBlue.copy(alpha = 0.6f)
                    )
                    val imagePaths = patient.imagePaths
                    if (imagePaths != null) {
                        Text(
                            text = "${imagePaths.size} images",
                            fontSize = 12.sp,
                            color = primaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            val phone = patient.phone
            if (phone != null && phone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phone: $phone",
                    fontSize = 14.sp,
                    color = darkBlue.copy(alpha = 0.7f)
                )
            }
        }
    }
}

