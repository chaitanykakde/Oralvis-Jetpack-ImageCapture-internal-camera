package com.chaitany.oralvisjetpack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.chaitany.oralvisjetpack.ui.screens.LoginScreen
import com.chaitany.oralvisjetpack.ui.screens.HistoryScreen
import com.chaitany.oralvisjetpack.ui.screens.ImageSequenceScreen
import com.chaitany.oralvisjetpack.ui.screens.PatientEntryScreen
import com.chaitany.oralvisjetpack.ui.screens.SessionDetailScreen
import com.chaitany.oralvisjetpack.ui.screens.WelcomeScreen
import com.chaitany.oralvisjetpack.utils.PreferencesManager
import androidx.compose.ui.platform.LocalContext

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Welcome.createRoute()) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onProceed = {
                    val clinicId = preferencesManager.getClinicId() ?: ""
                    navController.navigate(Screen.PatientEntry.createRoute(clinicId))
                },
                onHistoryClick = {
                    val clinicId = preferencesManager.getClinicId() ?: ""
                    navController.navigate(Screen.History.createRoute(clinicId))
                },
                onLogout = {
                    // Clear session and navigate to login
                    preferencesManager.clearClinicSession()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PatientEntry.route,
            arguments = listOf(
                navArgument("clinicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val clinicId = backStackEntry.arguments?.getString("clinicId") ?: ""
            
            PatientEntryScreen(
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
                navArgument("clinicId") { type = NavType.StringType },
                navArgument("patientId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val folderName = backStackEntry.arguments?.getString("folderName") ?: ""
            val clinicId = backStackEntry.arguments?.getString("clinicId") ?: ""
            val patientId = backStackEntry.arguments?.getInt("patientId") ?: 0
            
            val scope = rememberCoroutineScope()
            
            ImageSequenceScreen(
                folderName = folderName,
                clinicId = clinicId,
                patientId = patientId,
                onComplete = {
                    // Navigate back to Welcome screen after session completion
                    navController.navigate(Screen.Welcome.createRoute()) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
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
                navArgument("clinicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val clinicId = backStackEntry.arguments?.getString("clinicId") ?: ""
            
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
                navArgument("clinicId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            val clinicIdParam = backStackEntry.arguments?.getString("clinicId") ?: ""
            
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

