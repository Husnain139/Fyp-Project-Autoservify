package com.hstan.autoservify.ui.main.Shops.Services

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.ServiceRepository
import kotlinx.coroutines.launch

class ServiceViewModel : ViewModel() {

    private val repository = ServiceRepository()

    private val _services = MutableLiveData<List<Service>>()
    val services: LiveData<List<Service>> = _services

    private val _isSuccessfullySaved = MutableLiveData<Boolean?>()
    val isSuccessfullySaved: LiveData<Boolean?> = _isSuccessfullySaved

    private val _failureMessage = MutableLiveData<String?>()
    val failureMessage: LiveData<String?> = _failureMessage

    private val _isSuccessfullyDeleted = MutableLiveData<Boolean?>()
    val isSuccessfullyDeleted: LiveData<Boolean?> = _isSuccessfullyDeleted

    private val _isSuccessfullyUpdated = MutableLiveData<Boolean?>()
    val isSuccessfullyUpdated: LiveData<Boolean?> = _isSuccessfullyUpdated

    // 🔥 Single service
    private val _selectedService = MutableLiveData<Service?>()
    val selectedService: LiveData<Service?> = _selectedService

    fun addService(service: Service) {
        viewModelScope.launch {
            try {
                val success = repository.saveService(service)
                _isSuccessfullySaved.value = success
                if (success) {
                    loadServices()
                } else {
                    _failureMessage.value = "Failed to save service"
                }
            } catch (e: Exception) {
                Log.e("ServiceViewModel", "Error saving service: ${e.message}")
                _failureMessage.value = e.message
            }
        }
    }

    fun loadServices() {
        viewModelScope.launch {
            try {
                val result = repository.getServices()
                _services.value = result
            } catch (e: Exception) {
                _failureMessage.value = e.message
            }
        }
    }

    // 🆕 Load services for a specific shop (role-based access)
    fun loadServicesByShopId(shopId: String) {
        viewModelScope.launch {
            try {
                val result = repository.getServicesByShopId(shopId)
                _services.value = result
            } catch (e: Exception) {
                _failureMessage.value = e.message
            }
        }
    }

    //  Fetch single service by ID
    fun loadServiceById(serviceId: String) {
        viewModelScope.launch {
            try {
                val result = repository.getServiceById(serviceId)
                _selectedService.value = result
            } catch (e: Exception) {
                _failureMessage.value = e.message
            }
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            try {
                val success = repository.deleteService(serviceId)
                _isSuccessfullyDeleted.value = success
                if (success) {
                    loadServices() // Refresh the list
                } else {
                    _failureMessage.value = "Failed to delete service"
                }
            } catch (e: Exception) {
                Log.e("ServiceViewModel", "Error deleting service: ${e.message}")
                _failureMessage.value = e.message
            }
        }
    }

    fun clearDeleteStatus() {
        _isSuccessfullyDeleted.value = null
    }

    fun updateService(service: Service) {
        viewModelScope.launch {
            try {
                val success = repository.updateService(service)
                _isSuccessfullyUpdated.value = success
                if (success) {
                    loadServices() // Refresh the list
                } else {
                    _failureMessage.value = "Failed to update service"
                }
            } catch (e: Exception) {
                Log.e("ServiceViewModel", "Error updating service: ${e.message}")
                _failureMessage.value = e.message
            }
        }
    }

    fun clearUpdateStatus() {
        _isSuccessfullyUpdated.value = null
    }
}
