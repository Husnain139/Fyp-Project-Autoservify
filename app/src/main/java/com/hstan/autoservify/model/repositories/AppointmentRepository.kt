package com.hstan.autoservify.model.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll

class AppointmentRepository {

    private val appointmentCollection =
        FirebaseFirestore.getInstance().collection("Appointments")
    private val serviceRepository = ServiceRepository()
    
    // Helper function to enrich appointments with service images
    private suspend fun enrichAppointmentsWithServiceImages(appointments: List<Appointment>): List<Appointment> {
        return coroutineScope {
            appointments.map { appointment ->
                async {
                    if (appointment.serviceImageUrl.isEmpty() && appointment.serviceId.isNotEmpty()) {
                        try {
                            val service = serviceRepository.getServiceById(appointment.serviceId)
                            if (service != null) {
                                appointment.serviceImageUrl = service.imageUrl
                            }
                        } catch (e: Exception) {
                            println("Error fetching service image for appointment: ${e.message}")
                        }
                    }
                    appointment
                }
            }.awaitAll()
        }
    }

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

    fun getAppointments(): Flow<List<Appointment>> =
        appointmentCollection.snapshots()
            .transform { snapshot ->
                val appointments = snapshot.toObjects(Appointment::class.java)
                val enrichedAppointments = enrichAppointmentsWithServiceImages(appointments)
                emit(enrichedAppointments)
            }

    // 🆕 Get appointments for a specific shop
    fun getShopAppointments(shopId: String): Flow<List<Appointment>> =
        appointmentCollection
            .whereEqualTo("shopId", shopId)
            .snapshots()
            .transform { snapshot ->
                val appointments = snapshot.toObjects(Appointment::class.java)
                val enrichedAppointments = enrichAppointmentsWithServiceImages(appointments)
                emit(enrichedAppointments)
            }

    // 🆕 Get recent appointments for a specific shop (for dashboard)
    fun getRecentShopAppointments(shopId: String, limit: Int = 3): Flow<List<Appointment>> =
        appointmentCollection
            .whereEqualTo("shopId", shopId)
            .limit(limit.toLong())
            .snapshots()
            .transform { snapshot ->
                val appointments = snapshot.toObjects(Appointment::class.java)
                val enrichedAppointments = enrichAppointmentsWithServiceImages(appointments)
                emit(enrichedAppointments)
            }

    // 🆕 Get appointments for a specific customer
    fun getCustomerAppointments(userId: String): Flow<List<Appointment>> =
        appointmentCollection
            .whereEqualTo("userId", userId)
            .snapshots()
            .transform { snapshot ->
                val appointments = snapshot.toObjects(Appointment::class.java)
                val enrichedAppointments = enrichAppointmentsWithServiceImages(appointments)
                emit(enrichedAppointments)
            }

}
