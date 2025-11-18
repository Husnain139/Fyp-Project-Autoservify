package com.hstan.autoservify.model

data class Review(
    var id: String = "",
    var userId: String = "",
    var userName: String = "",
    var shopId: String = "",
    var itemId: String = "",  // Order ID or Appointment ID
    var itemType: String = "",  // "ORDER" or "APPOINTMENT"
    var rating: Float = 0f,  // 1.0 to 5.0
    var comment: String = "",
    var timestamp: Long = System.currentTimeMillis()
) {
    // Empty constructor for Firebase
    constructor() : this("", "", "", "", "", "", 0f, "", 0L)
}

