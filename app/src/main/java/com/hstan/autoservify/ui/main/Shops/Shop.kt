package com.hstan.autoservify.ui.main.Shops

class Shop {
    var id: String = ""
    var title: String = ""
    var description: String = ""
    var address: String = ""
    var phone: String = ""
    var email: String = ""
    var ownerId: String = "" // The user ID who owns this shop
    var ownerName: String = ""
    var createdAt: Long = System.currentTimeMillis()
}