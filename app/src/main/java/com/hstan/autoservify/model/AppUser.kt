package com.hstan.autoservify.model

data class AppUser(
    var uid: String = "",
    var email: String = "",
    var name: String = "",
    var phone: String = "",
    var userType: String = "",
    var shopId: String? = null,
    var profileImageUrl: String? = null
) {
    constructor() : this("", "", "", "", "", null, null)
}
