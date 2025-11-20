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

    suspend fun updateShop(shop: Shop): Result<Boolean> {
        return try {
            if (shop.id.isEmpty()) {
                return Result.failure(Exception("Shop ID is required for update"))
            }
            shopCollection.document(shop.id).set(shop).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getShops(): Flow<List<Shop>> {
        return shopCollection.snapshots().map { it.toObjects(Shop::class.java) }
    }
}
