package com.hstan.autoservify.ui.main.Cart

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.AppointmentRepository
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.utils.NotificationSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class BookAppointmentViewModel : ViewModel() {

    private val appointmentRepository = AppointmentRepository()
    private val authRepository = AuthRepository()
    private var context: Context? = null

    val isSaving = MutableStateFlow(false)
    val isSaved = MutableStateFlow<Boolean?>(null)
    val failureMessage = MutableStateFlow<String?>(null)

    fun setContext(context: Context) {
        this.context = context
    }

    fun getCurrentUser() = authRepository.getCurrentUser()

    fun saveAppointment(appointment: Appointment) {
        viewModelScope.launch {
            isSaving.value = true
            val result = appointmentRepository.saveAppointment(appointment)
            isSaving.value = false

            if (result.isSuccess) {
                isSaved.value = true
                
                // Send notification to shopkeeper when appointment is booked
                if (appointment.shopId.isNotEmpty() && context != null) {
                    val notificationSender = NotificationSender(context!!)
                    val appointmentId = appointment.id.ifEmpty { appointment.appointmentId }
                    notificationSender.sendAppointmentNotificationToShopkeeper(
                        shopId = appointment.shopId,
                        appointmentId = appointmentId,
                        customerName = appointment.userName.ifEmpty { "Customer" },
                        serviceName = appointment.serviceName,
                        appointmentDate = appointment.appointmentDate,
                        appointmentTime = appointment.appointmentTime,
                        onSuccess = {},
                        onFailure = { error ->
                            android.util.Log.e("BookAppointmentViewModel", "Failed to send notification: $error")
                        }
                    )
                }
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }
}
