package com.hstan.autoservify.utils

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val authRepository = AuthRepository()
    private val notificationUtil = NotificationUtil()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        
        // Save token to user profile
        CoroutineScope(Dispatchers.IO).launch {
            saveTokenToProfile(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // Handle notification data
            val title = remoteMessage.data["title"] ?: "New Notification"
            val body = remoteMessage.data["body"] ?: ""
            val type = remoteMessage.data["type"] ?: ""
            val orderId = remoteMessage.data["orderId"] ?: ""
            val appointmentId = remoteMessage.data["appointmentId"] ?: ""

            // Show notification
            notificationUtil.showNotification(
                context = this,
                title = title,
                body = body,
                type = type,
                orderId = orderId,
                appointmentId = appointmentId
            )
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            notificationUtil.showNotification(
                context = this,
                title = it.title ?: "New Notification",
                body = it.body ?: "",
                type = remoteMessage.data["type"] ?: "",
                orderId = remoteMessage.data["orderId"] ?: "",
                appointmentId = remoteMessage.data["appointmentId"] ?: ""
            )
        }
    }

    private suspend fun saveTokenToProfile(token: String) {
        val currentUser = authRepository.getCurrentUser()
        currentUser?.let { user ->
            val result = authRepository.getUserProfile(user.uid)
            if (result.isSuccess) {
                val userProfile = result.getOrThrow()
                userProfile.fcmToken = token
                authRepository.saveUserProfile(userProfile)
                Log.d(TAG, "FCM token saved to user profile")
            }
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}

