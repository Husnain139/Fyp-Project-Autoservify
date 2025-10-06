package com.hstan.autoservify.model.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hstan.autoservify.ui.main.Shops.Services.Service
import kotlinx.coroutines.tasks.await

class ServiceRepository {
    private val serviceCollection = FirebaseFirestore.getInstance().collection("Services")

    suspend fun saveService(service: Service): Boolean {
        return try {
            val doc = serviceCollection.document()
            service.id = doc.id
            doc.set(service).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getServices(): List<Service> {
        return try {
            val snapshot = serviceCollection.get().await()
            snapshot.toObjects(Service::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getServicesByShopId(shopId: String): List<Service> {
        return try {
            val snapshot = serviceCollection.whereEqualTo("shopId", shopId).get().await()
            snapshot.toObjects(Service::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    //  New: Get single service by ID
    suspend fun getServiceById(serviceId: String): Service? {
        return try {
            val doc = serviceCollection.document(serviceId).get().await()
            doc.toObject(Service::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Delete service
    suspend fun deleteService(serviceId: String): Boolean {
        return try {
            serviceCollection.document(serviceId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Update service
    suspend fun updateService(service: Service): Boolean {
        return try {
            service.id?.let { id ->
                serviceCollection.document(id).set(service).await()
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    // 🆕 Get recent services for a specific shop (for dashboard)
    suspend fun getRecentServicesByShopId(shopId: String, limit: Int = 3): List<Service> {
        return try {
            val snapshot = serviceCollection
                .whereEqualTo("shopId", shopId)
                .orderBy("id", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            snapshot.toObjects(Service::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
