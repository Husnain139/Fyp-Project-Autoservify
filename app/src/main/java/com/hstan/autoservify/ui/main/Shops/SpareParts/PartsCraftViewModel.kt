package com.hstan.autoservify.ui.main.Shops.SpareParts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.PartsCraftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class PartsCraftViewModel : ViewModel() {

    private val repository = PartsCraftRepository()

    private val _partsCrafts = MutableLiveData<List<PartsCraft>>()
    val partsCrafts: LiveData<List<PartsCraft>> = _partsCrafts

    private val _isSuccessfullyDeleted = MutableStateFlow<Boolean?>(null)
    val isSuccessfullyDeleted = _isSuccessfullyDeleted

    private val _isSuccessfullyUpdated = MutableStateFlow<Boolean?>(null)
    val isSuccessfullyUpdated = _isSuccessfullyUpdated

    private val _failureMessage = MutableStateFlow<String?>(null)
    val failureMessage = _failureMessage

    fun loadPartsCrafts() {
        viewModelScope.launch {
            try {
                repository.getPartsCrafts().collect { partsCraftsList ->
                    _partsCrafts.value = partsCraftsList
                }
            } catch (e: Exception) {
                // Handle error
                _partsCrafts.value = emptyList()
                _failureMessage.value = e.message
            }
        }
    }

    // 🆕 Load parts for a specific shop (role-based access)
    fun loadPartsCraftsByShopId(shopId: String) {
        viewModelScope.launch {
            try {
                repository.getPartsCraftsByShopId(shopId).collect { partsCraftsList ->
                    _partsCrafts.value = partsCraftsList
                }
            } catch (e: Exception) {
                // Handle error
                _partsCrafts.value = emptyList()
                _failureMessage.value = e.message
            }
        }
    }

    fun deletePartsCraft(partId: String) {
        viewModelScope.launch {
            val result = repository.deletePartsCraft(partId)
            if (result.isSuccess) {
                _isSuccessfullyDeleted.value = true
                // Note: loadPartsCrafts() will automatically refresh due to Flow collection
            } else {
                _failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun clearDeleteStatus() {
        _isSuccessfullyDeleted.value = null
    }

    fun updatePartsCraft(partsCraft: PartsCraft) {
        viewModelScope.launch {
            val result = repository.updatePartsCraft(partsCraft)
            if (result.isSuccess) {
                _isSuccessfullyUpdated.value = true
                // Note: loadPartsCrafts() will automatically refresh due to Flow collection
            } else {
                _failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun clearUpdateStatus() {
        _isSuccessfullyUpdated.value = null
    }
}
