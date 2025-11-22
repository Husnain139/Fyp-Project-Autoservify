# Notification Feature Setup Guide

This document explains how to set up and configure the notification feature in the AutoServify application.

## Overview

The notification system allows:
1. **Shopkeepers** to receive notifications when:
   - A new order is placed
   - A new appointment is booked

2. **Customers** to receive notifications when:
   - Their order status is updated
   - Their appointment status is updated

## Setup Instructions

### 1. Get Firebase Server Key

To enable push notifications, you need to configure the Firebase Server Key:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `autoservify-92eb3`
3. Navigate to **Project Settings** (gear icon)
4. Go to the **Cloud Messaging** tab
5. Copy the **Server key** (Legacy server key)

### 2. Configure Server Key

Open the file: `app/src/main/java/com/hstan/autoservify/utils/NotificationSender.kt`

Find this line (around line 25):
```kotlin
private val SERVER_KEY = "YOUR_FIREBASE_SERVER_KEY"
```

Replace `YOUR_FIREBASE_SERVER_KEY` with your actual Firebase Server Key:
```kotlin
private val SERVER_KEY = "AIzaSyC_your_actual_server_key_here"
```

### 3. Android Permissions

The app already includes notification permissions in `AndroidManifest.xml`. For Android 13+, users will be prompted to grant notification permissions when they first launch the app.

### 4. How It Works

1. **FCM Token Management**:
   - When a user logs in, the app automatically retrieves their FCM token
   - The token is saved to the user's profile in Firestore
   - Tokens are refreshed automatically when needed

2. **Order Notifications**:
   - When a customer places an order, the shopkeeper receives a notification
   - When a shopkeeper updates order status, the customer receives a notification

3. **Appointment Notifications**:
   - When a customer books an appointment, the shopkeeper receives a notification
   - When a shopkeeper updates appointment status, the customer receives a notification

### 5. Testing

1. Ensure both shopkeeper and customer accounts have logged in at least once (to generate FCM tokens)
2. Place a test order or book a test appointment
3. Check the notification tray on the receiving device
4. Verify that notifications appear correctly

### 6. Troubleshooting

**Notifications not working?**
- Verify the SERVER_KEY is correctly configured
- Check that both users have logged in and have FCM tokens stored in their profiles
- Check Logcat for error messages (look for "NotificationSender" tag)
- Verify Firebase Cloud Messaging is enabled in Firebase Console

**FCM Token Issues:**
- Tokens are automatically refreshed when the app is reinstalled or data is cleared
- If tokens are missing, users should log out and log back in

## Important Notes

1. **Security**: The current implementation uses FCM REST API with a server key. For production, consider:
   - Using Firebase Cloud Functions instead (more secure)
   - Storing the server key securely on a backend server
   - Never commit the server key to version control

2. **Manual Entries**: Manual orders/appointments created by shopkeepers do not trigger notifications (shopkeeper is already aware)

3. **Offline**: Notifications are sent via Firebase Cloud Messaging, which requires an internet connection

## Files Modified/Created

### New Files:
- `app/src/main/java/com/hstan/autoservify/utils/FirebaseMessagingService.kt` - Handles incoming FCM messages
- `app/src/main/java/com/hstan/autoservify/utils/NotificationUtil.kt` - Displays notifications
- `app/src/main/java/com/hstan/autoservify/utils/NotificationSender.kt` - Sends notifications via FCM API

### Modified Files:
- `app/build.gradle.kts` - Added Firebase Cloud Messaging dependency
- `app/src/main/AndroidManifest.xml` - Added FCM service and notification permissions
- `app/src/main/java/com/hstan/autoservify/model/AppUser.kt` - Added `fcmToken` field
- `app/src/main/java/com/hstan/autoservify/ui/main/MyApp.kt` - Initialize FCM token on app start
- `app/src/main/java/com/hstan/autoservify/model/repositories/AuthRepository.kt` - Added `updateFCMToken` method
- `app/src/main/java/com/hstan/autoservify/model/repositories/ShopRepository.kt` - Added `getShopById` method
- `app/src/main/java/com/hstan/autoservify/ui/orders/CreateOrderActivity.kt` - Send notification when order is placed
- `app/src/main/java/com/hstan/autoservify/ui/orders/OrderDetailActivity.kt` - Send notification when order status is updated
- `app/src/main/java/com/hstan/autoservify/ui/orders/AppointmentDetailActivity.kt` - Send notification when appointment status is updated
- `app/src/main/java/com/hstan/autoservify/ui/main/Cart/BookAppointment_Activity.kt` - Send notification when appointment is booked
- `app/src/main/java/com/hstan/autoservify/ui/main/Cart/BookAppointmentViewModel.kt` - Handle notification sending in ViewModel

