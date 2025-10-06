package com.hstan.autoservify.ui.main.Shops.SpareParts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.PartsCraftRepository
import com.hstan.autoservify.model.repositories.StorageRepository
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AddpartcraftViewModel : ViewModel() {
    val handCraftsRepository = PartsCraftRepository()
    val storageRepository = StorageRepository()

    val isSuccessfullySaved = MutableStateFlow<Boolean?>(null)
    val failureMessage = MutableStateFlow<String?>(null)

    fun uploadImageAndSaveHandCraft(imagePath: String, partCraft: PartsCraft) {
        storageRepository.uploadFile(imagePath, onComplete = { success, result ->
            if (success) {
                partCraft.image=result!!
                saveHandCraft(partCraft)
            }
            else failureMessage.value=result
        })
    }
    fun saveHandCraft(partCraft: PartsCraft) {
        viewModelScope.launch {
            val result = handCraftsRepository.savePartsCraft(partCraft)
            if (result.isSuccess) {
                isSuccessfullySaved.value = true
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

}