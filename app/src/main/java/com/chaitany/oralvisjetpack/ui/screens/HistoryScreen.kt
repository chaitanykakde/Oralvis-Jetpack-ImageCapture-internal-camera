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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.chaitany.oralvisjetpack.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryTab {
    History, Recent
}

enum class UploadStatus {
    Completed, Ongoing, NotStarted
}

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
    
    var selectedTab by remember { mutableStateOf(HistoryTab.History) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Colors matching the design
    val primaryBlue = Color(0xFF4A8BBF)
    val darkBlue = Color(0xFF1E3A5F)
    val lightBlueBorder = Color(0xFFE3F2FD)
    val lightGray = Color(0xFFE0E0E0)
    
    LaunchedEffect(clinicId) {
        viewModel.loadPatientsForClinic(clinicId)
    }
    
    // Filter patients based on search query
    // Note: ViewModel already filters to only patients with timestamps and sorts them
    val filteredPatients = remember(patients, searchQuery) {
        if (searchQuery.isBlank()) {
            patients // Already filtered and sorted by ViewModel
        } else {
            // Filter by search query, but patients are already sorted by timestamp from ViewModel
            patients.filter { patient ->
                patient.name?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }
    
    // Sort for both tabs (most recent first by timestamp)
    // ViewModel already provides sorted list, but we ensure consistency here
    val sortedPatients = remember(filteredPatients, selectedTab) {
        // Patients are already sorted by timestamp (descending) from ViewModel
        // Just ensure they're in correct order (should already be sorted)
        val sorted = filteredPatients.sortedByDescending { patient ->
            patient.timestamp ?: 0L
        }
        
        when (selectedTab) {
            HistoryTab.History -> {
                // All sessions, most recent first
                android.util.Log.d("HistoryScreen", "History tab: showing ${sorted.size} patients")
                sorted
            }
            HistoryTab.Recent -> {
                // Most recent session only (first item after descending sort)
                val recent = sorted.take(1)
                // Log for debugging
                recent.firstOrNull()?.let { patient ->
                    val dateStr = patient.timestamp?.let {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date(it))
                    } ?: "No timestamp"
                    android.util.Log.d("HistoryScreen", "Recent tab showing: Patient ${patient.patientId}, timestamp=${patient.timestamp} ($dateStr), name=${patient.name}")
                } ?: run {
                    android.util.Log.d("HistoryScreen", "Recent tab: No patients to show")
                }
                recent
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "History",
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
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Tab Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // History Tab
                        TabButton(
                            text = "History",
                            isSelected = selectedTab == HistoryTab.History,
                            onClick = { selectedTab = HistoryTab.History },
                            modifier = Modifier.weight(1f),
                            primaryBlue = primaryBlue,
                            lightGray = lightGray
                        )
                        
                        // Recent Tab
                        TabButton(
                            text = "Recent",
                            isSelected = selectedTab == HistoryTab.Recent,
                            onClick = { selectedTab = HistoryTab.Recent },
                            modifier = Modifier.weight(1f),
                            primaryBlue = primaryBlue,
                            lightGray = lightGray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by patients name", color = Color.Gray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = lightBlueBorder,
                            focusedBorderColor = primaryBlue,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedTextColor = Color.Black,
                            focusedTextColor = Color.Black
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Patient List
                    if (sortedPatients.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedTab == HistoryTab.Recent) "No recent sessions" else "No patient records found",
                                color = darkBlue,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sortedPatients) { patient ->
                                PatientCard(
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
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryBlue: Color,
    lightGray: Color
) {
    val darkBlue = Color(0xFF1E3A5F)
    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .then(
                if (isSelected) {
                    Modifier.shadow(2.dp, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) primaryBlue else Color.Transparent,
            contentColor = if (isSelected) Color.White else darkBlue
        ),
        border = if (!isSelected) {
            androidx.compose.foundation.BorderStroke(1.dp, lightGray)
        } else null
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PatientCard(
    patient: com.chaitany.oralvisjetpack.data.model.PatientData,
    onClick: () -> Unit,
    primaryBlue: Color,
    darkBlue: Color,
    lightBlueBorder: Color
) {
    val status = getUploadStatus(patient)
    val uploadDate = getUploadDate(patient)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, lightBlueBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Patient name: ${patient.name ?: "Unknown"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = darkBlue
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Age: ${patient.age ?: "N/A"}",
                    fontSize = 14.sp,
                    color = darkBlue.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "Gender: ${patient.gender ?: "N/A"}",
                    fontSize = 14.sp,
                    color = darkBlue.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "Photos uploaded: $uploadDate",
                    fontSize = 14.sp,
                    color = darkBlue.copy(alpha = 0.7f)
                )
            }
            
            // Status Indicator
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Status Dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = getStatusColor(status),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    
                    Text(
                        text = getStatusText(status),
                        fontSize = 12.sp,
                        color = darkBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun getUploadStatus(patient: com.chaitany.oralvisjetpack.data.model.PatientData): UploadStatus {
    val imagePaths = patient.imagePaths
    return when {
        imagePaths == null || imagePaths.isEmpty() -> UploadStatus.NotStarted
        imagePaths.size < 8 -> UploadStatus.Ongoing
        else -> UploadStatus.Completed
    }
}

fun getStatusColor(status: UploadStatus): Color {
    return when (status) {
        UploadStatus.Completed -> Color(0xFF4CAF50) // Green
        UploadStatus.Ongoing -> Color(0xFFFF9800) // Orange/Yellow
        UploadStatus.NotStarted -> Color(0xFFF44336) // Red
    }
}

fun getStatusText(status: UploadStatus): String {
    return when (status) {
        UploadStatus.Completed -> "Completed"
        UploadStatus.Ongoing -> "Ongoing"
        UploadStatus.NotStarted -> "Not started"
    }
}

fun getUploadDate(patient: com.chaitany.oralvisjetpack.data.model.PatientData): String {
    // Try to extract date from patientId or use current date format
    // For now, using a simple format. You can enhance this based on your data structure
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    return try {
        // If patientId contains timestamp or date info, parse it
        // Otherwise, use current date as fallback
        dateFormat.format(Date())
    } catch (e: Exception) {
        dateFormat.format(Date())
    }
}
