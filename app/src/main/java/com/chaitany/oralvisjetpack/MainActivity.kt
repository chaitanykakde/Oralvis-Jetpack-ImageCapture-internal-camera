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
import com.chaitany.oralvisjetpack.navigation.NavGraph
import com.chaitany.oralvisjetpack.navigation.Screen
import com.chaitany.oralvisjetpack.ui.theme.OralVisJetpackTheme
import com.chaitany.oralvisjetpack.utils.PreferencesManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OralVisJetpackTheme {
                val navController = rememberNavController()
                val preferencesManager = remember { PreferencesManager(this) }
                
                // Check if user is logged in
                val startDestination = if (preferencesManager.isLoggedIn()) {
                    Screen.Welcome.createRoute()
                } else {
                    Screen.Login.route
                }

                NavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}