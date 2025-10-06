package com.hstan.autoservify.DataSource

import android.content.Context
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class CloudinaryUploadHelper {

    companion object {
        fun initializeCloudinary(context: Context) {
            val config = mapOf(
                "cloud_name" to "dxte8zkjp", // Replace with your Cloudinary cloud name
                "api_key" to System.getenv("CLOUDINARY_API_KEY"),
                "api_secret" to System.getenv("CLOUDINARY_API_SECRET")
            )
            MediaManager.init(context, config)
        }
    }

    fun uploadFile(
        fileUri: String, // Changed from filePath to URI
        onComplete: (Boolean, String?) -> Unit
    ) {
        if (fileUri.isEmpty()) {
            Log.e("Cloudinary", "Upload failed: File URI is empty.")
            onComplete(false, "File URI is empty.")
            return
        }

        MediaManager.get().upload(fileUri)
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
    }
}