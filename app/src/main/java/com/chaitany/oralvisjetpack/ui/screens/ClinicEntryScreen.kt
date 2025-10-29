package com.chaitany.oralvisjetpack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaitany.oralvisjetpack.data.database.OralVisDatabase
import com.chaitany.oralvisjetpack.data.repository.ClinicRepository
import com.chaitany.oralvisjetpack.viewmodel.ClinicEntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicEntryScreen(
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Clinic Info") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = clinicName,
                onValueChange = { viewModel.updateClinicName(it) },
                label = { Text("Clinic Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = clinicId,
                onValueChange = { viewModel.updateClinicId(it) },
                label = { Text("Clinic ID") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveClinicInfo { name, id ->
                        onClinicSaved(name, id)
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
                    Text("Save & Proceed")
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

