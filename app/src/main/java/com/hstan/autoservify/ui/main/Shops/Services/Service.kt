package com.hstan.autoservify.ui.main.Shops.Services

// Data Model for a Service
data class Service(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var price: Double = 0.0,
    var rating: Double = 0.0,
    var shopId: String = "" // Shop this service belongs to
)
