package com.chaitany.oralvisjetpack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaitany.oralvisjetpack.data.database.OralVisDatabase
import com.chaitany.oralvisjetpack.data.repository.ClinicRepository
import com.chaitany.oralvisjetpack.viewmodel.ClinicEntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicLoginScreen(
    onClinicSaved: (String, Int) -> Unit
) {
    val context = LocalContext.current
    val database = remember { OralVisDatabase.getDatabase(context) }
    val repository = remember { ClinicRepository(database.clinicDao()) }
    
    val viewModel: ClinicEntryViewModel = remember {
        ClinicEntryViewModel(repository)
    }

    val clinicName by viewModel.clinicName.collectAsState()
    val clinicId by viewModel.clinicId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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
            // OralVis Logo at top
            val logoResId = context.resources.getIdentifier("oralvis_logo", "drawable", context.packageName)
            if (logoResId != 0) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = "OralVis Logo",
                    modifier = Modifier.size(80.dp)
                )
            } else {
                Text(
                    text = "OralVis",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Main Title
            Text(
                text = "Clinic data collection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = darkBlue,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Instructional Text
            Text(
                text = "Please enter your clinic details to\nbegin patient data collection",
                fontSize = 14.sp,
                color = darkBlue,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Clinic Name Field
            OutlinedTextField(
                value = clinicName,
                onValueChange = { viewModel.updateClinicName(it) },
                placeholder = { Text("Clinic name", color = Color.Black.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = lightBlueBorder,
                    focusedBorderColor = primaryBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                enabled = !isLoading,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Clinic ID Field
            OutlinedTextField(
                value = clinicId,
                onValueChange = { viewModel.updateClinicId(it) },
                placeholder = { Text("Clinic ID", color = Color.Black.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = lightBlueBorder,
                    focusedBorderColor = primaryBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                enabled = !isLoading,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Save and Next Button
            Button(
                onClick = {
                    viewModel.saveClinicInfo { name, id ->
                        onClinicSaved(name, id)
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
}

