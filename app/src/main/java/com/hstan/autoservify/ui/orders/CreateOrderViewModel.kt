package com.hstan.autoservify.ui.orders
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.ui.main.ViewModels.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CreateOrderViewModel : ViewModel() {

    private val orderRepository = OrderRepository()
    private val authRepository = AuthRepository()

    val isSaving = MutableStateFlow(false)
    val isSaved = MutableStateFlow<Boolean?>(null)
    val failureMessage = MutableStateFlow<String?>(null)

    fun getCurrentUser() = authRepository.getCurrentUser()

    fun saveOrder(order: Order) {
        viewModelScope.launch {
            isSaving.value = true
            val result = orderRepository.saveOrder(order)
            isSaving.value = false
            if (result.isSuccess) {
                isSaved.value = true
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun resetState() {
        isSaved.value = null
        failureMessage.value = null
    }
}