package com.hstan.autoservify.ui.main.Shops

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hstan.autoservify.databinding.ActivityAddShopBinding
import com.hstan.autoservify.ui.main.Shops.Shop
import com.hstan.autoservify.ui.main.home.MainActivity
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch
import android.content.Intent

class AddShopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddShopBinding
    private lateinit var viewModel: AddShopViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = AddShopViewModel()

        lifecycleScope.launch {
            viewModel.isSuccessfullySaved.collect {
                it?.let {
                    if (it) {
                        Toast.makeText(this@AddShopActivity, "Shop added successfully!", Toast.LENGTH_SHORT).show()
                        // Navigate to ShopkeeperMainActivity to show dashboard
                        startActivity(Intent(this@AddShopActivity, com.hstan.autoservify.ui.shopkeeper.ShopkeeperMainActivity::class.java))
                        finish()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.failureMessage.collect {
                it?.let {
                    Toast.makeText(this@AddShopActivity, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.submitButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val description = binding.descriptionInput.text.toString().trim()
            val address = binding.addressInput.text.toString().trim()
            val phone = binding.phoneInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || address.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get current user and link shop to owner
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val shop = Shop().apply {
                    this.title = title
                    this.description = description
                    this.address = address
                    this.phone = phone
                    this.email = email
                    this.ownerId = currentUser.uid
                    this.ownerName = currentUser.displayName ?: ""
                }

                viewModel.saveShop(shop)
            } else {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}