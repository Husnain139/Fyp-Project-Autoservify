package com.hstan.autoservify.ui.main.Shops.Services

class Appointment(
    var id: String = "",
    var userId: String = "",
    var userName: String = "",
    var userContact: String = "",
    var serviceId: String = "",
    var serviceName: String = "", //
    var serviceImageUrl: String = "", // Service image URL
    var shopId: String = "",  // 🆕 Link appointment to specific shop
    var status: String = "Pending", // "Pending", "Confirmed", "Completed"
    var appointmentId: String = "",
    var appointmentTime: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var appointmentDate: String = "",
    var userEmail: String = "",
    var userFCMToken: String = "",
    var bill: String = "",
    var isManualEntry: Boolean = false  // Flag to indicate if this is a manual entry by shopkeeper
)
