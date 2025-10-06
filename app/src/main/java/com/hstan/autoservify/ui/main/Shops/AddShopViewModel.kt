package com.hstan.autoservify.ui.main.Shops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.ui.main.Shops.Shop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class AddShopViewModel : ViewModel() {

    private val shopRepository = ShopRepository()
    private val authRepository = AuthRepository()

    val isSuccessfullySaved = MutableStateFlow<Boolean?>(null)
    val failureMessage = MutableStateFlow<String?>(null)

    fun saveShop(shop: Shop) {
        viewModelScope.launch {
            try {
                // Save the shop
                val result = shopRepository.saveShop(shop)
                if (result.isSuccess) {
                    // Update user profile with shopId
                    val updateResult = authRepository.updateUserShopId(shop.ownerId, shop.id)
                    if (updateResult.isSuccess) {
                        isSuccessfullySaved.value = true
                    } else {
                        failureMessage.value = "Shop saved but failed to link to user profile"
                    }
                } else {
                    failureMessage.value = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                failureMessage.value = e.message
            }
        }
    }
}