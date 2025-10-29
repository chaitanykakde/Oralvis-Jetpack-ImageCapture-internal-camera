package com.chaitany.oralvisjetpack.utils

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.File
import java.io.FileOutputStream

object ZipUtils {
    
    suspend fun createZipFile(
        context: Context,
        folderName: String,
        imageFiles: Map<String, ByteArray>,
        excelBytes: ByteArray,
        clinicId: Int,
        patientId: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create Documents/myapp directory
            val documentsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "myapp"
            )
            
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }
            
            val zipFile = File(documentsDir, "$folderName.zip")
            val zipOutputStream = ZipArchiveOutputStream(FileOutputStream(zipFile))
            
            // Add all images
            imageFiles.forEach { (fileName, bytes) ->
                val entry = ZipArchiveEntry(fileName)
                entry.size = bytes.size.toLong()
                zipOutputStream.putArchiveEntry(entry)
                zipOutputStream.write(bytes)
                zipOutputStream.closeArchiveEntry()
            }
            
            // Add CSV file with naming convention: clinicId_patientId.csv
            val csvFileName = "${clinicId}_$patientId.csv"
            val csvEntry = ZipArchiveEntry(csvFileName)
            csvEntry.size = excelBytes.size.toLong()
            zipOutputStream.putArchiveEntry(csvEntry)
            zipOutputStream.write(excelBytes)
            zipOutputStream.closeArchiveEntry()
            
            zipOutputStream.close()
            
            Result.success(zipFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

