package com.example.brain.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // We track the download progress for the UI
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadState>> = _downloadProgress

    data class DownloadState(
        val modelName: String,
        val progress: Float,
        val isComplete: Boolean,
        val isDownloading: Boolean,
        val hasFailed: Boolean,
        val localFile: File?
    )

    fun isModelDownloaded(modelName: String): Boolean {
        val file = getModelFile(modelName)
        return file.exists() && file.length() > 0
    }

    fun getModelFile(modelName: String): File {
        val modelsDir = File(context.filesDir, "nous_models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        return File(modelsDir, modelName)
    }

    fun startDownload(modelName: String, url: String) {
        if (isModelDownloaded(modelName)) {
            Log.i("ModelDownloader", "Model $modelName already downloaded.")
            updateState(modelName, 1f, true, false, false, getModelFile(modelName))
            return
        }

        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri).apply {
            setTitle("Downloading $modelName")
            setDescription("NOUS Core Asset")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            // Download to app-specific external files so DownloadManager can write to it, then we move it
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, modelName)
        }

        val downloadId = downloadManager.enqueue(request)
        updateState(modelName, 0f, false, true, false, null)

        // Launch a coroutine to monitor the download
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            monitorDownload(downloadId, modelName)
        }
    }

    private suspend fun monitorDownload(downloadId: Long, modelName: String) {
        var downloading = true
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            
            if (cursor.moveToFirst()) {
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIdx)

                val bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                
                val bytesDownloaded = cursor.getLong(bytesDownloadedIdx)
                val bytesTotal = cursor.getLong(bytesTotalIdx)

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    downloading = false
                    val uriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val localUri = cursor.getString(uriIdx)
                    
                    // Move file from external to internal
                    moveFileToInternalStorage(Uri.parse(localUri), modelName)
                } else if (status == DownloadManager.STATUS_FAILED) {
                    downloading = false
                    Log.e("ModelDownloader", "Download failed for $modelName")
                    // Handle failure state (reset progress)
                    updateState(modelName, 0f, false, false, true, null)
                } else {
                    val progress = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal.toFloat() else 0f
                    updateState(modelName, progress, false, true, false, null)
                }
            }
            cursor.close()
            if (downloading) delay(1000)
        }
    }

    suspend fun importModelFromUri(uri: Uri, modelName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val destFile = getModelFile(modelName)
                if (destFile.exists()) destFile.delete()
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                updateState(modelName, 1f, true, false, false, destFile)
                Log.i("ModelDownloader", "✅ Model imported successfully: $modelName")
                return@withContext true
            } catch (e: Exception) {
                Log.e("ModelDownloader", "❌ Import failed: ${e.message}")
                updateState(modelName, 0f, false, false, true, null)
                return@withContext false
            }
        }
    }

    private suspend fun moveFileToInternalStorage(sourceUri: Uri, modelName: String) {
        withContext(Dispatchers.IO) {
            try {
                val destFile = getModelFile(modelName)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Try to delete the original downloaded file from external downloads
                try {
                    if (sourceUri.scheme == "file") {
                        File(sourceUri.path!!).delete()
                    } else {
                        // For content URI, we can try to use contentResolver.delete
                        context.contentResolver.delete(sourceUri, null, null)
                    }
                } catch (e: Exception) {
                    Log.w("ModelDownloader", "Could not delete source file: ${e.message}")
                }
                
                updateState(modelName, 1f, true, false, false, destFile)
            } catch (e: Exception) {
                Log.e("ModelDownloader", "Error moving file: ${e.message}")
                updateState(modelName, 0f, false, false, true, null)
            }
        }
    }

    private fun updateState(
        modelName: String, 
        progress: Float, 
        isComplete: Boolean, 
        isDownloading: Boolean,
        hasFailed: Boolean,
        file: File?
    ) {
        val currentState = _downloadProgress.value.toMutableMap()
        currentState[modelName] = DownloadState(modelName, progress, isComplete, isDownloading, hasFailed, file)
        _downloadProgress.value = currentState
    }
}
