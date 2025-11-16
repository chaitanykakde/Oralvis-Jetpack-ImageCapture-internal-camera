# CSV Upload Utility (Optional)

## 📋 Overview

This is an optional utility to upload the CSV file from Android assets to S3 programmatically. You can use this instead of manual upload.

## 🚀 Implementation

Add this function to `CsvAuthUtils.kt`:

```kotlin
/**
 * Uploads the clinics_2025.csv file from assets to S3
 * Call this once during app setup or admin panel
 */
suspend fun uploadCsvToS3(context: Context): Boolean {
    return try {
        val credentialsProvider = OralVisApplication.credentialsProvider
        
        val clientConfig = ClientConfiguration().apply {
            connectionTimeout = 30000
            socketTimeout = 60000
            maxErrorRetry = 3
        }
        
        val region = com.amazonaws.regions.Region.getRegion(Regions.AP_SOUTH_1)
        val s3Client = AmazonS3Client(credentialsProvider, clientConfig)
        s3Client.setRegion(region)
        
        // Read CSV from assets
        val inputStream = context.assets.open("clinics_2025.csv")
        val csvBytes = inputStream.readBytes()
        inputStream.close()
        
        Log.d(TAG, "Uploading CSV to S3: s3://$S3_BUCKET_NAME/$S3_CSV_KEY (${csvBytes.size} bytes)")
        
        // Upload to S3
        val metadata = com.amazonaws.services.s3.model.ObjectMetadata().apply {
            contentLength = csvBytes.size.toLong()
            contentType = "text/csv"
        }
        
        val putObjectRequest = com.amazonaws.services.s3.model.PutObjectRequest(
            S3_BUCKET_NAME,
            S3_CSV_KEY,
            java.io.ByteArrayInputStream(csvBytes),
            metadata
        )
        
        s3Client.putObject(putObjectRequest)
        Log.d(TAG, "✓ Successfully uploaded CSV to S3")
        true
    } catch (e: Exception) {
        Log.e(TAG, "Error uploading CSV to S3", e)
        false
    }
}
```

## 📱 Usage

You can call this from an admin screen or during app initialization:

```kotlin
// In a ViewModel or Activity
viewModelScope.launch {
    val success = withContext(Dispatchers.IO) {
        CsvAuthUtils.uploadCsvToS3(context)
    }
    if (success) {
        // Show success message
    } else {
        // Show error message
    }
}
```

## ⚠️ Note

This is optional. You can also upload the CSV manually via AWS Console (see `CSV_UPLOAD_INSTRUCTIONS.md`).

