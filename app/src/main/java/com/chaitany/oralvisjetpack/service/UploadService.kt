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
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_UPLOAD -> {
                startForeground(NOTIFICATION_ID, createNotification(0, "Starting upload..."))
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Upload Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows upload progress for patient data"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
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
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Uploading to Cloud")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .apply {
                // Make it non-cancellable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setChannelId(CHANNEL_ID)
                }
            }
            .build()
    }
    
    private fun updateNotification(progress: Int, status: String) {
        val notification = createNotification(progress, status)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
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

