package com.hstan.autoservify.utils

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.ShopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Utility class to send FCM notifications
 * Note: This uses Firebase Cloud Messaging REST API
 * For production, consider using Firebase Cloud Functions instead
 */
class NotificationSender(private val context: Context) {

    private val authRepository = AuthRepository()
    private val shopRepository = ShopRepository()
    private val FCM_API_URL = "https://fcm.googleapis.com/fcm/send"
    
    // TODO: Replace with your Firebase Server Key from Firebase Console
    // Project Settings -> Cloud Messaging -> Server Key
    private val SERVER_KEY = "YOUR_FIREBASE_SERVER_KEY"

    /**
     * Send notification to shopkeeper when a new order is placed
     */
    fun sendOrderNotificationToShopkeeper(
        shopId: String,
        orderId: String,
        customerName: String,
        itemName: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get shop owner info
                val shopResult = shopRepository.getShopById(shopId)
                if (shopResult.isFailure || shopResult.getOrNull() == null) {
                    onFailure("Shop not found")
                    return@launch
                }

                val shop = shopResult.getOrNull()!!
                val ownerId = shop.ownerId

                if (ownerId.isEmpty()) {
                    onFailure("Shop owner not found")
                    return@launch
                }

                // Get shop owner's FCM token
                val userResult = authRepository.getUserProfile(ownerId)
                if (userResult.isFailure) {
                    onFailure("Shop owner profile not found")
                    return@launch
                }

                val owner = userResult.getOrThrow()
                if (owner.fcmToken.isEmpty()) {
                    onFailure("Shop owner FCM token not available")
                    return@launch
                }

                // Send notification
                sendFCMNotification(
                    fcmToken = owner.fcmToken,
                    title = "New Order Received",
                    body = "$customerName placed an order for $itemName",
                    data = mapOf(
                        "type" to "new_order",
                        "orderId" to orderId,
                        "shopId" to shopId
                    ),
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending order notification to shopkeeper", e)
                onFailure(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Send notification to shopkeeper when a new appointment is booked
     */
    fun sendAppointmentNotificationToShopkeeper(
        shopId: String,
        appointmentId: String,
        customerName: String,
        serviceName: String,
        appointmentDate: String,
        appointmentTime: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get shop owner info
                val shopResult = shopRepository.getShopById(shopId)
                if (shopResult.isFailure || shopResult.getOrNull() == null) {
                    onFailure("Shop not found")
                    return@launch
                }

                val shop = shopResult.getOrNull()!!
                val ownerId = shop.ownerId

                if (ownerId.isEmpty()) {
                    onFailure("Shop owner not found")
                    return@launch
                }

                // Get shop owner's FCM token
                val userResult = authRepository.getUserProfile(ownerId)
                if (userResult.isFailure) {
                    onFailure("Shop owner profile not found")
                    return@launch
                }

                val owner = userResult.getOrThrow()
                if (owner.fcmToken.isEmpty()) {
                    onFailure("Shop owner FCM token not available")
                    return@launch
                }

                // Send notification
                sendFCMNotification(
                    fcmToken = owner.fcmToken,
                    title = "New Appointment Booked",
                    body = "$customerName booked $serviceName on $appointmentDate at $appointmentTime",
                    data = mapOf(
                        "type" to "new_appointment",
                        "appointmentId" to appointmentId,
                        "shopId" to shopId
                    ),
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending appointment notification to shopkeeper", e)
                onFailure(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Send notification to customer when order status is updated
     */
    fun sendOrderStatusUpdateToCustomer(
        customerUserId: String,
        orderId: String,
        newStatus: String,
        itemName: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get customer's FCM token
                val userResult = authRepository.getUserProfile(customerUserId)
                if (userResult.isFailure) {
                    onFailure("Customer profile not found")
                    return@launch
                }

                val customer = userResult.getOrThrow()
                if (customer.fcmToken.isEmpty()) {
                    onFailure("Customer FCM token not available")
                    return@launch
                }

                // Send notification
                sendFCMNotification(
                    fcmToken = customer.fcmToken,
                    title = "Order Status Updated",
                    body = "Your order for $itemName is now: $newStatus",
                    data = mapOf(
                        "type" to "order_status_update",
                        "orderId" to orderId,
                        "status" to newStatus
                    ),
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending order status update to customer", e)
                onFailure(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Send notification to customer when appointment status is updated
     */
    fun sendAppointmentStatusUpdateToCustomer(
        customerUserId: String,
        appointmentId: String,
        newStatus: String,
        serviceName: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get customer's FCM token
                val userResult = authRepository.getUserProfile(customerUserId)
                if (userResult.isFailure) {
                    onFailure("Customer profile not found")
                    return@launch
                }

                val customer = userResult.getOrThrow()
                if (customer.fcmToken.isEmpty()) {
                    onFailure("Customer FCM token not available")
                    return@launch
                }

                // Send notification
                sendFCMNotification(
                    fcmToken = customer.fcmToken,
                    title = "Appointment Status Updated",
                    body = "Your appointment for $serviceName is now: $newStatus",
                    data = mapOf(
                        "type" to "appointment_status_update",
                        "appointmentId" to appointmentId,
                        "status" to newStatus
                    ),
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending appointment status update to customer", e)
                onFailure(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Send FCM notification using REST API
     */
    private fun sendFCMNotification(
        fcmToken: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        if (SERVER_KEY == "YOUR_FIREBASE_SERVER_KEY") {
            Log.e(TAG, "Firebase Server Key not configured. Please set SERVER_KEY in NotificationSender.kt")
            onFailure("Server key not configured")
            return
        }

        try {
            val requestQueue = Volley.newRequestQueue(context)

            val notification = JSONObject().apply {
                put("title", title)
                put("body", body)
            }

            val dataObject = JSONObject()
            data.forEach { (key, value) ->
                dataObject.put(key, value)
            }

            val payload = JSONObject().apply {
                put("to", fcmToken)
                put("notification", notification)
                put("data", dataObject)
                put("priority", "high")
            }

            val request = object : JsonObjectRequest(
                Request.Method.POST,
                FCM_API_URL,
                payload,
                { response ->
                    Log.d(TAG, "FCM notification sent successfully: $response")
                    onSuccess()
                },
                { error ->
                    val errorMsg = error.message ?: "Failed to send notification"
                    Log.e(TAG, "Error sending FCM notification: $errorMsg")
                    onFailure(errorMsg)
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] = "application/json"
                    headers["Authorization"] = "key=$SERVER_KEY"
                    return headers
                }
            }

            requestQueue.add(request)
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending FCM notification", e)
            onFailure(e.message ?: "Unknown error")
        }
    }

    companion object {
        private const val TAG = "NotificationSender"
    }
}

