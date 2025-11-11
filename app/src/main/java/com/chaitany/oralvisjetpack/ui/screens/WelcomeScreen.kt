package com.chaitany.oralvisjetpack.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaitany.oralvisjetpack.utils.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onProceed: () -> Unit,
    onHistoryClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val clinicName = remember { preferencesManager.getClinicName() ?: "Clinic" }
    val density = LocalDensity.current
    
    // Colors matching the design
    val primaryBlue = Color(0xFF4A8BBF)
    val darkBlue = Color(0xFF1E3A5F)
    
    // Calculate logo position: 40% more upper from center
    // If center is at 50%, then 40% more upper means 50% - 40% = 10% from top
    val screenHeight = context.resources.displayMetrics.heightPixels
    val logoTopPadding = with(density) { (screenHeight * 0.1f).toDp() } // 10% from top (40% above center)
    
    // Background image
    val backgroundResId = context.resources.getIdentifier("backgroundhomepage", "drawable", context.packageName)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Welcome, $clinicName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    // Logout button
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Filled.ExitToApp,
                            contentDescription = "Logout",
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
        // Background image in center, 50% of screen height, with 10dp margins on left and right
        if (backgroundResId != 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .padding(horizontal = 10.dp)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = backgroundResId),
                    contentDescription = "Home Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds // Fill the entire container
                )
            }
        }
        
        // Content overlay
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Oravis Collect Small Logo positioned 40% more upper from center
            val smallLogoResId = context.resources.getIdentifier("oralvissmalllogo", "drawable", context.packageName)
            if (smallLogoResId != 0) {
                Image(
                    painter = painterResource(id = smallLogoResId),
                    contentDescription = "Oravis Collect Logo",
                    modifier = Modifier
                        .size(120.dp) // Small logo size
                        .align(Alignment.TopCenter)
                        .padding(top = logoTopPadding)
                )
            } else {
                // Fallback text logo
                Text(
                    text = "Oravis Collect",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = logoTopPadding)
                )
            }
            
            // Rest of the content (background image, welcome text, button) centered
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // The background image already contains the tooth graphic, so we don't need a separate placeholder
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Proceed Button
                Button(
                    onClick = onProceed,
                    modifier = Modifier
                        .width(280.dp)
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Proceed to patient entry",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // History Button
                Button(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .width(280.dp)
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = darkBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
        }
    }
}

