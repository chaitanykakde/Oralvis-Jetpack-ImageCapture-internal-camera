package com.chaitany.oralvisjetpack.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chaitany.oralvisjetpack.MainActivity
import com.chaitany.oralvisjetpack.R
import com.chaitany.oralvisjetpack.utils.UploadQueueManager
import kotlinx.coroutines.*

class UploadService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var uploadJob: Job? = null
    
    companion object {
        private const val CHANNEL_ID = "upload_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_UPLOAD = "com.chaitany.oralvisjetpack.START_UPLOAD"
        const val ACTION_STOP_UPLOAD = "com.chaitany.oralvisjetpack.STOP_UPLOAD"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    private var isForegroundService = false
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_UPLOAD -> {
                isForegroundService = false
                try {
                    // Try to start foreground service
                    // For Android 15+ (API 35+), this may fail if app is not in foreground
                    startForeground(NOTIFICATION_ID, createNotification(0, "Starting upload..."))
                    isForegroundService = true
                    android.util.Log.d("UploadService", "Foreground service started successfully")
                } catch (e: SecurityException) {
                    android.util.Log.e("UploadService", "SecurityException: Missing FOREGROUND_SERVICE_DATA_SYNC permission", e)
                    // Show regular notification instead
                    showRegularNotification(0, "Starting upload...")
                    android.util.Log.w("UploadService", "Using regular notification instead of foreground")
                } catch (e: RuntimeException) {
                    // Check if it's ForegroundServiceStartNotAllowedException (Android 15+)
                    if (e.message?.contains("ForegroundServiceStartNotAllowedException") == true ||
                        e.message?.contains("not allowed due to mAllowStartForeground") == true) {
                        android.util.Log.e("UploadService", "Cannot start foreground service - app may not be in foreground (Android 15+)", e)
                        // Show regular notification instead
                        showRegularNotification(0, "Starting upload...")
                        android.util.Log.w("UploadService", "Using regular notification instead of foreground")
                    } else {
                        android.util.Log.e("UploadService", "RuntimeException starting foreground service", e)
                        // Try regular notification as fallback
                        showRegularNotification(0, "Starting upload...")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("UploadService", "Exception starting foreground service", e)
                    // Show regular notification as fallback
                    showRegularNotification(0, "Starting upload...")
                }
                
                // Start upload regardless of foreground service status
                startUpload()
            }
            ACTION_STOP_UPLOAD -> {
                stopUpload()
                if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Use IMPORTANCE_DEFAULT for Android 16+ to ensure notifications are visible
            // IMPORTANCE_LOW might not show notifications on newer Android versions
            val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                NotificationManager.IMPORTANCE_DEFAULT
            } else {
                NotificationManager.IMPORTANCE_LOW
            }
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Upload Progress",
                importance
            ).apply {
                description = "Shows upload progress for patient data"
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
                // Enable lights and lockscreen visibility for better visibility
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            
            // Check if notifications are enabled for this channel
            val channelSettings = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channelSettings?.importance == NotificationManager.IMPORTANCE_NONE) {
                android.util.Log.w("UploadService", "Notification channel is disabled by user")
            }
        }
    }
    
    private fun createNotification(progress: Int, status: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Use higher priority for Android 16+ to ensure visibility
        val priority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            NotificationCompat.PRIORITY_DEFAULT
        } else {
            NotificationCompat.PRIORITY_LOW
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Uploading to Cloud")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .apply {
                // Make it non-cancellable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setChannelId(CHANNEL_ID)
                }
            }
            .build()
    }
    
    private fun showRegularNotification(progress: Int, status: String) {
        // Check if notifications are enabled (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.areNotificationsEnabled()) {
                android.util.Log.w("UploadService", "Notifications are disabled by user")
                return
            }
        }
        
        val notification = createNotification(progress, status)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check channel importance (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                android.util.Log.w("UploadService", "Notification channel is disabled")
                return
            }
        }
        
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            android.util.Log.d("UploadService", "Regular notification shown: $status")
        } catch (e: SecurityException) {
            android.util.Log.e("UploadService", "SecurityException showing notification - POST_NOTIFICATIONS permission may be missing", e)
        } catch (e: Exception) {
            android.util.Log.e("UploadService", "Exception showing notification", e)
        }
    }
    
    private fun updateNotification(progress: Int, status: String) {
        // Check if notifications are enabled (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.areNotificationsEnabled()) {
                android.util.Log.w("UploadService", "Notifications are disabled by user")
                return
            }
        }
        
        val notification = createNotification(progress, status)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check channel importance (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
                android.util.Log.w("UploadService", "Notification channel is disabled")
                return
            }
        }
        
        try {
            if (isForegroundService) {
                // Update foreground notification
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)
            } else {
                // Update regular notification
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
            android.util.Log.d("UploadService", "Notification updated: $status (progress: $progress%)")
        } catch (e: SecurityException) {
            android.util.Log.e("UploadService", "SecurityException updating notification - POST_NOTIFICATIONS permission may be missing", e)
        } catch (e: Exception) {
            android.util.Log.e("UploadService", "Exception updating notification", e)
        }
    }
    
    private fun startUpload() {
        android.util.Log.d("UploadService", "=== Starting upload service ===")
        uploadJob = serviceScope.launch {
            try {
                android.util.Log.d("UploadService", "Updating notification: Preparing upload...")
                updateNotification(0, "Preparing upload...")
                
                android.util.Log.d("UploadService", "Calling processQueueWithProgress")
                UploadQueueManager.processQueueWithProgress(
                    context = applicationContext,
                    onProgress = { progress, total, message ->
                        android.util.Log.d("UploadService", "Progress: $progress/$total - $message")
                        // progress is already 0-100, total is always 100
                        updateNotification(progress, message)
                    },
                    onComplete = { success, message ->
                        android.util.Log.d("UploadService", "Upload complete callback: success=$success, message=$message")
                        launch(Dispatchers.Main) {
                            try {
                                if (success) {
                                    android.util.Log.d("UploadService", "Upload successful, showing success notification")
                                    updateNotification(100, "All uploads completed successfully")
                                    delay(2000) // Show success for 2 seconds
                                } else {
                                    android.util.Log.e("UploadService", "Upload failed: $message")
                                    updateNotification(0, "Upload failed: $message")
                                    delay(3000) // Show error for 3 seconds
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("UploadService", "Error updating notification", e)
                                e.printStackTrace()
                            } finally {
                                // Always stop service and dismiss notification
                                android.util.Log.d("UploadService", "Stopping foreground service and dismissing notification")
                                try {
                                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    // Cancel notification first
                                    notificationManager.cancel(NOTIFICATION_ID)
                                    android.util.Log.d("UploadService", "Notification cancelled")
                                    
                                    // Stop foreground service
                                    if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                                        stopForeground(STOP_FOREGROUND_REMOVE)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        stopForeground(true)
                                    }
                                    android.util.Log.d("UploadService", "Foreground service stopped")
                                    
                                    // Stop self
                                    stopSelf()
                                    android.util.Log.d("UploadService", "Service stopped successfully")
                                    
                                    // Double-check: cancel notification again after a short delay
                                    launch {
                                        delay(500)
                                        notificationManager.cancel(NOTIFICATION_ID)
                                        android.util.Log.d("UploadService", "Notification cancelled again (double-check)")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("UploadService", "Error stopping service", e)
                                    e.printStackTrace()
                                    // Force cancel notification even if service stop fails
                                    try {
                                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                        notificationManager.cancel(NOTIFICATION_ID)
                                    } catch (cancelError: Exception) {
                                        android.util.Log.e("UploadService", "Error cancelling notification", cancelError)
                                    }
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("UploadService", "Upload service error", e)
                e.printStackTrace()
                try {
                    updateNotification(0, "Error: ${e.message}")
                    launch {
                        delay(3000)
                        try {
                            android.util.Log.d("UploadService", "Stopping service after error")
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(NOTIFICATION_ID)
                            if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                            } else {
                                @Suppress("DEPRECATION")
                                stopForeground(true)
                            }
                            stopSelf()
                            android.util.Log.d("UploadService", "Service stopped after error")
                        } catch (stopError: Exception) {
                            android.util.Log.e("UploadService", "Error stopping service", stopError)
                            stopError.printStackTrace()
                            // Force cancel notification
                            try {
                                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                notificationManager.cancel(NOTIFICATION_ID)
                            } catch (cancelError: Exception) {
                                android.util.Log.e("UploadService", "Error cancelling notification", cancelError)
                            }
                        }
                    }
                } catch (notifError: Exception) {
                    android.util.Log.e("UploadService", "Error updating notification", notifError)
                    notifError.printStackTrace()
                    // Still try to stop service
                    try {
                        android.util.Log.d("UploadService", "Force stopping service")
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(NOTIFICATION_ID)
                        if (Build.VERSION.SDK_INT >= VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(true)
                        }
                        stopSelf()
                    } catch (stopError: Exception) {
                        android.util.Log.e("UploadService", "Error stopping service", stopError)
                        stopError.printStackTrace()
                        // Force cancel notification
                        try {
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.cancel(NOTIFICATION_ID)
                        } catch (cancelError: Exception) {
                            android.util.Log.e("UploadService", "Error cancelling notification", cancelError)
                        }
                    }
                }
            }
        }
    }
    
    private fun stopUpload() {
        uploadJob?.cancel()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        uploadJob?.cancel()
        serviceScope.cancel()
    }
}

