package com.chaitany.oralvisjetpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.chaitany.oralvisjetpack.data.database.OralVisDatabase
import com.chaitany.oralvisjetpack.data.repository.ClinicRepository
import com.chaitany.oralvisjetpack.navigation.NavGraph
import com.chaitany.oralvisjetpack.navigation.Screen
import com.chaitany.oralvisjetpack.ui.theme.OralVisJetpackTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OralVisJetpackTheme {
                val navController = rememberNavController()
                val database = remember { OralVisDatabase.getDatabase(this) }
                val clinicRepository = remember { ClinicRepository(database.clinicDao()) }
                
                var startDestination by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    scope.launch {
                        val clinic = clinicRepository.getClinic()
                        startDestination = if (clinic != null) {
                            Screen.Welcome.createRoute(clinic.name, clinic.clinicId)
                        } else {
                            Screen.ClinicEntry.route
                        }
                    }
                }

                if (startDestination != null) {
                    NavGraph(
                        navController = navController,
                        clinicRepository = clinicRepository,
                        startDestination = startDestination!!
                    )
                } else {
                    // Show loading while determining start destination
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}