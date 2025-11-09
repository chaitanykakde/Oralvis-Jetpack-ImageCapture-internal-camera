package com.chaitany.oralvisjetpack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chaitany.oralvisjetpack.data.repository.ClinicRepository
import com.chaitany.oralvisjetpack.ui.screens.ClinicLoginScreen
import com.chaitany.oralvisjetpack.ui.screens.HistoryScreen
import com.chaitany.oralvisjetpack.ui.screens.ImageSequenceScreen
import com.chaitany.oralvisjetpack.ui.screens.PatientEntryScreen
import com.chaitany.oralvisjetpack.ui.screens.SessionDetailScreen
import com.chaitany.oralvisjetpack.ui.screens.WelcomeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    clinicRepository: ClinicRepository,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.ClinicEntry.route) {
            ClinicLoginScreen(
                onClinicSaved = { clinicName, clinicId ->
                    navController.navigate(Screen.Welcome.createRoute(clinicName, clinicId)) {
                        popUpTo(Screen.ClinicEntry.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Welcome.route,
            arguments = listOf(
                navArgument("clinicName") { type = NavType.StringType },
                navArgument("clinicId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val clinicName = backStackEntry.arguments?.getString("clinicName") ?: ""
            val clinicId = backStackEntry.arguments?.getInt("clinicId") ?: 0
            
            WelcomeScreen(
                clinicName = clinicName,
                clinicId = clinicId,
                onProceed = {
                    navController.navigate(Screen.PatientEntry.createRoute(clinicName, clinicId))
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.createRoute(clinicId))
                }
            )
        }

        composable(
            route = Screen.PatientEntry.route,
            arguments = listOf(
                navArgument("clinicName") { type = NavType.StringType },
                navArgument("clinicId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val clinicName = backStackEntry.arguments?.getString("clinicName") ?: ""
            val clinicId = backStackEntry.arguments?.getInt("clinicId") ?: 0
            
            PatientEntryScreen(
                clinicName = clinicName,
                clinicId = clinicId,
                onNavigateToImageCapture = { folderName, patientId ->
                    navController.navigate(
                        Screen.ImageSequence.createRoute(folderName, clinicId, patientId)
                    )
                }
            )
        }

        composable(
            route = Screen.ImageSequence.route,
            arguments = listOf(
                navArgument("folderName") { type = NavType.StringType },
                navArgument("clinicId") { type = NavType.IntType },
                navArgument("patientId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
            val clinicId = backStackEntry.arguments?.getInt("clinicId") ?: 0
            val patientId = backStackEntry.arguments?.getInt("patientId") ?: 0
            
            ImageSequenceScreen(
                folderName = folderName,
                clinicId = clinicId,
                patientId = patientId,
                onComplete = {
                    // Navigate back to PatientEntry, clearing the image sequence screen
                    navController.navigate(
                        Screen.PatientEntry.createRoute("Unknown", clinicId)
                    ) {
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.History.route,
            arguments = listOf(
                navArgument("clinicId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val clinicId = backStackEntry.arguments?.getInt("clinicId") ?: 0
            
            HistoryScreen(
                clinicId = clinicId,
                onBack = {
                    navController.popBackStack()
                },
                onSessionClick = { patientId, clinicIdParam ->
                    navController.navigate(
                        Screen.SessionDetail.createRoute(patientId, clinicIdParam)
                    )
                }
            )
        }
        
        composable(
            route = Screen.SessionDetail.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("clinicId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            val clinicIdParam = backStackEntry.arguments?.getInt("clinicId") ?: 0
            
            SessionDetailScreen(
                patientId = patientId,
                clinicId = clinicIdParam,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

