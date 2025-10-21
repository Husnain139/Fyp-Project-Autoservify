package com.hstan.autoservify.ui.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.AppointmentRepository
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class OrderFragmentViewModel : ViewModel() {

    private val ordersRepository = OrderRepository()
    private val appointmentRepository = AppointmentRepository()

    val failureMessage = MutableStateFlow<String?>(null)
    val orders = MutableStateFlow<List<Order>?>(null)
    val appointments = MutableStateFlow<List<Appointment>?>(null)
    val isUpdating = MutableStateFlow(false)

    init {
        // Don't load all orders by default - will be loaded based on user type
    }

    fun readOrders() {
        viewModelScope.launch {
            println("ViewModel: Starting to load all orders")
            ordersRepository.getOrders()
                .catch { 
                    println("ViewModel: Error loading orders: ${it.message}")
                    failureMessage.value = it.message 
                }
                .collect { 
                    println("ViewModel: Received ${it.size} orders")
                    orders.value = it 
                }
        }
    }

    // 🆕 Load appointments for a specific shop
    fun loadShopAppointments(shopId: String) {
        viewModelScope.launch {
            println("ViewModel: Loading appointments for shop: $shopId")
            appointmentRepository.getShopAppointments(shopId)
                .catch { 
                    println("ViewModel: Error loading shop appointments: ${it.message}")
                    failureMessage.value = it.message 
                }
                .collect { 
                    println("ViewModel: Received ${it.size} shop appointments")
                    appointments.value = it 
                }
        }
    }

    fun readAppointments() {
        viewModelScope.launch {
            println("ViewModel: Starting to load all appointments")
            appointmentRepository.getAppointments()
                .catch { 
                    println("ViewModel: Error loading appointments: ${it.message}")
                    failureMessage.value = it.message 
                }
                .collect { 
                    println("ViewModel: Received ${it.size} appointments")
                    appointments.value = it 
                }
        }
    }

    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            try {
                isUpdating.value = true
                order.status = "Canceled"
                val result = ordersRepository.updateOrder(order)
                if (result.isFailure) {
                    failureMessage.value = result.exceptionOrNull()?.message
                }
            } finally {
                isUpdating.value = false
            }
        }
    }

    fun deleteOrder(order: Order) {
        viewModelScope.launch {
            try {
                isUpdating.value = true
                val result = ordersRepository.deleteOrder(order.id)
                if (result.isFailure) {
                    failureMessage.value = result.exceptionOrNull()?.message
                } else {
                    // Order deleted successfully - the Flow will auto-update
                    println("ViewModel: Order ${order.id} deleted successfully")
                }
            } finally {
                isUpdating.value = false
            }
        }
    }

    fun cancelAppointment(appointment: Appointment) {
        viewModelScope.launch {
            try {
                isUpdating.value = true
                appointment.status = "cancelled"
                val result = appointmentRepository.updateAppointment(appointment)
                if (result.isFailure) {
                    failureMessage.value = result.exceptionOrNull()?.message
                }
            } finally {
                isUpdating.value = false
            }
        }
    }

    fun loadCustomerOrders(customerId: String) {
        viewModelScope.launch {
            println("ViewModel: Loading orders for customer: $customerId")
            ordersRepository.getCustomerOrders(customerId)
                .catch { 
                    println("ViewModel: Error loading customer orders: ${it.message}")
                    failureMessage.value = it.message 
                }
                .collect { 
                    println("ViewModel: Received ${it.size} customer orders")
                    orders.value = it 
                }
        }
    }

    fun loadShopOrders(shopId: String) {
        viewModelScope.launch {
            println("ViewModel: Loading orders for shop: $shopId")
            ordersRepository.getShopOrders(shopId)
                .catch { 
                    println("ViewModel: Error loading shop orders: ${it.message}")
                    failureMessage.value = it.message 
                }
                .collect { 
                    println("ViewModel: Received ${it.size} shop orders")
                    orders.value = it 
                }
        }
    }
}
