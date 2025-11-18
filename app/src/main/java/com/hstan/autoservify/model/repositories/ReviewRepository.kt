package com.hstan.autoservify.model.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.hstan.autoservify.model.Review
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ReviewRepository {
    private val reviewCollection = FirebaseFirestore.getInstance().collection("reviews")

    suspend fun saveReview(review: Review): Result<Boolean> {
        return try {
            if (review.id.isEmpty()) {
                val document = reviewCollection.document()
                review.id = document.id
            }
            reviewCollection.document(review.id).set(review).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getReviewsForShop(shopId: String): Flow<List<Review>> {
        return reviewCollection
            .whereEqualTo("shopId", shopId)
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Review::class.java) }
    }

    suspend fun getReviewForItem(itemId: String, userId: String): Result<Review?> {
        return try {
            val snapshot = reviewCollection
                .whereEqualTo("itemId", itemId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            if (snapshot.documents.isNotEmpty()) {
                Result.success(snapshot.documents[0].toObject(Review::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateAverageRating(shopId: String): Result<Float> {
        return try {
            val snapshot = reviewCollection
                .whereEqualTo("shopId", shopId)
                .get()
                .await()
            
            val reviews = snapshot.toObjects(Review::class.java)
            if (reviews.isEmpty()) {
                Result.success(0f)
            } else {
                val average = reviews.map { it.rating }.average().toFloat()
                Result.success(average)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAverageRatingFlow(shopId: String): Flow<Float> {
        return getReviewsForShop(shopId).map { reviews ->
            if (reviews.isEmpty()) {
                0f
            } else {
                reviews.map { it.rating }.average().toFloat()
            }
        }
    }
}

