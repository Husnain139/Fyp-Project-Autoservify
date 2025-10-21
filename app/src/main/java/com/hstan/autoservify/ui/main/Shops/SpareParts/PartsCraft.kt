package com.hstan.autoservify.ui.main.Shops.SpareParts

class PartsCraft {
    var id:String=""
    var title:String=""
    var description:String=""
    var image:String=""
    var price:Int=0
    var shopId:String=""  // The shop this spare part belongs to
    var manageInventory: Boolean = false  // Flag to enable inventory management
    var quantity: Int = 0  // Available stock quantity
    var lowStockLimit: Int = 10  // Global default threshold for low stock alert
    
    // Empty constructor for Firebase
    constructor()
    
    // Constructor with parameters
    constructor(id: String, title: String, description: String, image: String, price: Int, shopId: String) {
        this.id = id
        this.title = title
        this.description = description
        this.image = image
        this.price = price
        this.shopId = shopId
    }
}