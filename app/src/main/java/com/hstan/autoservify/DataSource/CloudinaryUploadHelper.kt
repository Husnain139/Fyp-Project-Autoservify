package com.hstan.autoservify.DataSource

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class CloudinaryUploadHelper {

    companion object {
        private var isInitialized = false

        fun initializeCloudinary(context: Context) {
            if (isInitialized) {
                Log.d("Cloudinary", "MediaManager already initialized")
                return
            }
            
            try {
                val config = mapOf(
                    "cloud_name" to "dlhixad3n",
                    "api_key" to "742872245591719",
                    "api_secret" to "5wTKcbrF0wyWhmO9FQIQtxxYWPI"
                )
                MediaManager.init(context, config)
                isInitialized = true
                Log.d("Cloudinary", "MediaManager initialized successfully")
            } catch (e: Exception) {
                Log.e("Cloudinary", "Error initializing MediaManager: ${e.message}")
            }
        }
    }

    fun uploadFile(
        fileUri: String, // Content URI as string
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (fileUri.isEmpty()) {
            Log.e("Cloudinary", "Upload failed: File URI is empty.")
            onComplete(false, "File URI is empty.")
            return
        }

        try {
            // Handle content:// URIs properly by using the URI directly
            Log.d("Cloudinary", "Starting upload for URI: $fileUri")
            
            MediaManager.get().upload(android.net.Uri.parse(fileUri))
                .option("resource_type", "auto")
                .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("Cloudinary", "Upload started: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    val progress = (bytes.toDouble() / totalBytes * 100).toInt()
                    Log.d("Cloudinary", "Upload progress: $progress%")
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val fileUrl = resultData["secure_url"] as? String
                    if (fileUrl != null) {
                        Log.d("Cloudinary", "Upload successful. URL: $fileUrl")
                        onComplete(true, fileUrl)
                    } else {
                        Log.e("Cloudinary", "Upload succeeded but URL missing.")
                        onComplete(false, "Upload succeeded but no URL returned.")
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "Upload failed: ${error?.description}")
                    onComplete(false, error?.description)
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "Upload rescheduled: ${error?.description}")
                }
            }).dispatch()
        } catch (e: Exception) {
            Log.e("Cloudinary", "Exception during upload setup: ${e.message}")
            onComplete(false, "Upload setup failed: ${e.message}")
        }
    }
}