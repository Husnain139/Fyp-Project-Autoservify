package com.hstan.autoservify.ui.main

import android.app.Application
import com.hstan.autoservify.DataSource.CloudinaryUploadHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp: Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize Cloudinary for image uploads
        CloudinaryUploadHelper.initializeCloudinary(this)
    }
}