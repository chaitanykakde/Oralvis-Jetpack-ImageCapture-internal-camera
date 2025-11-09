package com.chaitany.oralvisjetpack

import android.app.Application
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.regions.Regions

class OralVisApplication : Application() {
    companion object {
        // AWS Cognito Identity Pool ID for OralVis_IdentityPool
        // Region: ap-south-1 (Asia Pacific - Mumbai)
        private const val IDENTITY_POOL_ID = "ap-south-1:0142bd0e-3276-4a40-a71d-a5bcaa047798"
        
        lateinit var credentialsProvider: CognitoCachingCredentialsProvider
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize credentials provider
        credentialsProvider = CognitoCachingCredentialsProvider(
            applicationContext,
            IDENTITY_POOL_ID,
            Regions.AP_SOUTH_1
        )
        
        // Initialize background upload queue processing
        com.chaitany.oralvisjetpack.utils.UploadQueueManager.initializeBackgroundUpload(applicationContext)
    }
}

