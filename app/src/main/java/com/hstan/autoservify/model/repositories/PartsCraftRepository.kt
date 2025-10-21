package com.hstan.autoservify.model.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PartsCraftRepository {
    val PartsCraftCollection = FirebaseFirestore.getInstance().collection("Partscrafts")


    suspend fun savePartsCraft(PartsCraft: PartsCraft): Result<Boolean> {
        try {
            val document = PartsCraftCollection.document()
            PartsCraft.id = document.id
            document.set(PartsCraft).await()
            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun getPartsCrafts() =
        PartsCraftCollection.snapshots().map { it.toObjects(PartsCraft::class.java) }

    fun getPartsCraftsByShopId(shopId: String) =
        PartsCraftCollection.whereEqualTo("shopId", shopId).snapshots().map { it.toObjects(PartsCraft::class.java) }

    suspend fun deletePartsCraft(partId: String): Result<Boolean> {
        return try {
            PartsCraftCollection.document(partId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePartsCraft(partsCraft: PartsCraft): Result<Boolean> {
        return try {
            partsCraft.id?.let { id ->
                PartsCraftCollection.document(id).set(partsCraft).await()
                Result.success(true)
            } ?: Result.failure(Exception("Part ID is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🆕 Get recent spare parts for a specific shop (for dashboard)
    fun getRecentPartsCraftsByShopId(shopId: String, limit: Int = 3) =
        PartsCraftCollection
            .whereEqualTo("shopId", shopId)
            .orderBy("id", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots()
            .map { it.toObjects(PartsCraft::class.java) }

    // Update part quantity (for inventory management)
    suspend fun updatePartQuantity(partId: String, newQuantity: Int): Result<Boolean> {
        return try {
            PartsCraftCollection.document(partId)
                .update("quantity", newQuantity)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Decrease part quantity atomically (for order processing)
    suspend fun decreasePartQuantity(partId: String, decreaseBy: Int): Result<Boolean> {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.runTransaction { transaction ->
                val docRef = PartsCraftCollection.document(partId)
                val snapshot = transaction.get(docRef)
                val currentQuantity = snapshot.getLong("quantity")?.toInt() ?: 0
                val newQuantity = maxOf(0, currentQuantity - decreaseBy) // Prevent negative
                transaction.update(docRef, "quantity", newQuantity)
            }.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
