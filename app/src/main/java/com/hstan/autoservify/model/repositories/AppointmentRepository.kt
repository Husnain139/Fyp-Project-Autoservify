package com.hstan.autoservify.model.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AppointmentRepository {

    private val appointmentCollection =
        FirebaseFirestore.getInstance().collection("Appointments")

    suspend fun saveAppointment(appointment: Appointment): Result<Boolean> {
        return try {
            val document = appointmentCollection.document()
            appointment.appointmentId = document.id
            document.set(appointment).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAppointment(appointment: Appointment): Result<Boolean> {
        return try {
            val document = appointmentCollection.document(appointment.appointmentId)
            document.set(appointment).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAppointmentStatus(appointmentId: String, status: String): Result<Boolean> {
        return try {
            val document = appointmentCollection.document(appointmentId)
            document.update("status", status).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAppointments() =
        appointmentCollection.snapshots().map { it.toObjects(Appointment::class.java) }

    // 🆕 Get appointments for a specific shop
    fun getShopAppointments(shopId: String) =
        appointmentCollection
            .whereEqualTo("shopId", shopId)
            .snapshots()
            .map { it.toObjects(Appointment::class.java) }

    // 🆕 Get recent appointments for a specific shop (for dashboard)
    fun getRecentShopAppointments(shopId: String, limit: Int = 3) =
        appointmentCollection
            .whereEqualTo("shopId", shopId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .snapshots()
            .map { it.toObjects(Appointment::class.java) }
}
