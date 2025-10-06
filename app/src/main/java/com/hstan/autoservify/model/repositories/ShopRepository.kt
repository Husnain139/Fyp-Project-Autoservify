package com.hstan.autoservify.model.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.hstan.autoservify.ui.main.Shops.Shop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ShopRepository {
    private val shopCollection = FirebaseFirestore.getInstance().collection("shops")

    suspend fun saveShop(shop: Shop): Result<Boolean> {
        return try {
            val docRef = shopCollection.document()
            shop.id = docRef.id
            docRef.set(shop).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getShops(): Flow<List<Shop>> {
        return shopCollection.snapshots().map { it.toObjects(Shop::class.java) }
    }
}
