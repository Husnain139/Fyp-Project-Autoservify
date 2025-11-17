package com.hstan.autoservify.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hstan.autoservify.databinding.ActivitySignupBinding
import com.hstan.autoservify.ui.main.home.MainActivity
import com.hstan.autoservify.ui.main.Shops.AddShopActivity
import com.hstan.autoservify.model.AppUser
import com.hstan.autoservify.model.repositories.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = AuthViewModel()
        viewModel.checkUser()

        // Handle failure messages
        lifecycleScope.launch {
            viewModel.failureMessage.collect { message ->
                hideLoading()
                if (message != null) {
                    Toast.makeText(this@SignupActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle successful signup
        lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                if (user != null) {
                    hideLoading()
                    // Save user profile after successful signup
                    saveUserProfileAndNavigate(user)
                }
            }
        }

        // Setup account type card selection
        setupAccountTypeSelection()

        // Go to login
        binding.signupTxt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // optional: closes current activity
        }

        // Sign up button click
        binding.loginbutton.setOnClickListener {
            val email = binding.email.editText?.text.toString()
            val password = binding.password.editText?.text.toString()
            val confirmPassword = binding.confirmPassword.editText?.text.toString()
            val name = binding.name.editText?.text.toString()

            when {
                name.trim().isEmpty() -> {
                    Toast.makeText(this, "Enter name", Toast.LENGTH_SHORT).show()
                }
                !email.contains("@") -> {
                    Toast.makeText(this, "Invalid Email", Toast.LENGTH_SHORT).show()
                }
                password.length < 6 -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
                password != confirmPassword -> {
                    Toast.makeText(this, "Confirm password does not match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    showLoading()
                    viewModel.signUp(email, password, name)
                }
            }
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun saveUserProfileAndNavigate(user: FirebaseUser) {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val selectedUserType = getSelectedUserType()
            
            val userProfile = AppUser(
                uid = user.uid,
                email = user.email ?: "",
                name = user.displayName ?: "",
                userType = selectedUserType
            )
            
            val result = authRepository.saveUserProfile(userProfile)
            if (result.isSuccess) {
                // Navigate based on user type
                when (selectedUserType) {
                    "shop_owner" -> {
                        // New shopkeeper - go to AddShop
                        startActivity(Intent(this@SignupActivity, AddShopActivity::class.java))
                    }
                    else -> {
                        // Customer - go to CustomerMainActivity
                        startActivity(Intent(this@SignupActivity, com.hstan.autoservify.ui.customer.CustomerMainActivity::class.java))
                    }
                }
                finish()
            } else {
                Toast.makeText(this@SignupActivity, "Profile creation failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSelectedUserType(): String {
        return if (binding.shopOwnerRadio.isChecked) {
            "shop_owner"
        } else {
            "customer"
        }
    }

    private fun setupAccountTypeSelection() {
        // Customer card click
        binding.customerCard.setOnClickListener {
            selectCustomerAccount()
        }

        // Shop owner card click
        binding.shopOwnerCard.setOnClickListener {
            selectShopOwnerAccount()
        }

        // Radio button clicks
        binding.customerRadio.setOnClickListener {
            selectCustomerAccount()
        }

        binding.shopOwnerRadio.setOnClickListener {
            selectShopOwnerAccount()
        }

        // Initial selection
        selectCustomerAccount()
    }

    private fun selectCustomerAccount() {
        binding.customerRadio.isChecked = true
        binding.shopOwnerRadio.isChecked = false
        
        // Update card styles
        binding.customerCard.strokeWidth = 6
        binding.customerCard.strokeColor = getColor(com.hstan.autoservify.R.color.Primary_color)
        binding.customerCard.cardElevation = 4f
        
        binding.shopOwnerCard.strokeWidth = 2
        binding.shopOwnerCard.strokeColor = getColor(com.hstan.autoservify.R.color.light_gray)
        binding.shopOwnerCard.cardElevation = 2f
    }

    private fun selectShopOwnerAccount() {
        binding.customerRadio.isChecked = false
        binding.shopOwnerRadio.isChecked = true
        
        // Update card styles
        binding.shopOwnerCard.strokeWidth = 6
        binding.shopOwnerCard.strokeColor = getColor(com.hstan.autoservify.R.color.Primary_color)
        binding.shopOwnerCard.cardElevation = 4f
        
        binding.customerCard.strokeWidth = 2
        binding.customerCard.strokeColor = getColor(com.hstan.autoservify.R.color.light_gray)
        binding.customerCard.cardElevation = 2f
    }
}
