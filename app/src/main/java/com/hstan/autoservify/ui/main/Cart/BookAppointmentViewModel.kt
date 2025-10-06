package com.hstan.autoservify.ui.main.Cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.AppointmentRepository
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class BookAppointmentViewModel : ViewModel() {

    private val appointmentRepository = AppointmentRepository()
    private val authRepository = AuthRepository()

    val isSaving = MutableStateFlow(false)
    val isSaved = MutableStateFlow<Boolean?>(null)
    val failureMessage = MutableStateFlow<String?>(null)

    fun getCurrentUser() = authRepository.getCurrentUser()

    fun saveAppointment(appointment: Appointment) {
        viewModelScope.launch {
            isSaving.value = true
            val result = appointmentRepository.saveAppointment(appointment)
            isSaving.value = false

            if (result.isSuccess) {
                isSaved.value = true
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }
}
