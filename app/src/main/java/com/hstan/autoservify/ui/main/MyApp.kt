package com.hstan.autoservify.ui.main

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.hstan.autoservify.DataSource.CloudinaryUploadHelper
import com.hstan.autoservify.model.repositories.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class MyApp: Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize Cloudinary for image uploads
        CloudinaryUploadHelper.initializeCloudinary(this)
        
        // Initialize FCM token
        initializeFCMToken()
    }
    
    private fun initializeFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MyApp", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("MyApp", "FCM Registration Token: $token")

            // Save token to user profile
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null && token != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val authRepository = AuthRepository()
                    val result = authRepository.updateFCMToken(currentUser.uid, token)
                    if (result.isSuccess) {
                        Log.d("MyApp", "FCM token saved successfully")
                    } else {
                        Log.e("MyApp", "Failed to save FCM token: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
        }
    }
}