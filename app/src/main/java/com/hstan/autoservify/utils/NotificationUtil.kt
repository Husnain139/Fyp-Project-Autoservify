package com.hstan.autoservify.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.orders.AppointmentDetailActivity
import com.hstan.autoservify.ui.orders.OrderDetailActivity
import com.hstan.autoservify.ui.main.home.MainActivity

class NotificationUtil {
    
    private val CHANNEL_ID = "autoservify_notifications"
    private val CHANNEL_NAME = "AutoServify Notifications"
    private val CHANNEL_DESCRIPTION = "Notifications for orders and appointments"

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        type: String = "",
        orderId: String = "",
        appointmentId: String = ""
    ) {
        createNotificationChannel(context)

        val intent = when {
            orderId.isNotEmpty() -> {
                Intent(context, OrderDetailActivity::class.java).apply {
                    putExtra("orderId", orderId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
            appointmentId.isNotEmpty() -> {
                Intent(context, AppointmentDetailActivity::class.java).apply {
                    putExtra("appointmentId", appointmentId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
            else -> {
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() / 1000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = (System.currentTimeMillis() / 1000).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "NotificationUtil"
    }
}

