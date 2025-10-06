package com.hstan.autoservify.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hstan.autoservify.databinding.ActivityLoginBinding
import com.hstan.autoservify.ui.main.home.MainActivity
import com.hstan.autoservify.ui.customer.CustomerMainActivity
import com.hstan.autoservify.ui.shopkeeper.ShopkeeperMainActivity
import com.hstan.autoservify.ui.main.Shops.AddShopActivity
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = AuthViewModel()
        // Only check for existing user if we're launching fresh (not from explicit login attempt)
        if (!intent.hasExtra("skip_auto_login")) {
            viewModel.checkUser()
        }

        lifecycleScope.launch {
            viewModel.failureMessage.collect { message ->
                println("LoginActivity: Failure message collected: $message")
                hideLoading()
                if (message != null) {
                    Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                    // Clear the error message after showing
                    viewModel.failureMessage.value = null
                }
            }
        }

        lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                println("LoginActivity: currentUser collected: ${user?.uid}")
                if (user != null) {
                    hideLoading()
                    // Check user type and navigate to appropriate activity
                    navigateBasedOnUserType(user.uid)
                }
            }
        }

        binding.signupTxt.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }

        binding.forgetPassword.setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }

        binding.loginbutton.setOnClickListener {
            val email = binding.email.editText?.text.toString()
            val password = binding.password.editText?.text.toString()

            println("LoginActivity: Login button clicked with email: $email")
            
            if (!email.contains("@")) {
                Toast.makeText(this, "Invalid Email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Clear any previous state before attempting login
            viewModel.clearUserState()
            
            showLoading()
            println("LoginActivity: Calling viewModel.login")
            // Call ViewModel function to login here
            viewModel.login(email, password)
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }

    private fun navigateBasedOnUserType(uid: String) {
        lifecycleScope.launch {
            try {
                println("LoginActivity: Navigating based on user type for UID: $uid")
                val authRepository = AuthRepository()
                val result = authRepository.getUserProfile(uid)
                
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    println("LoginActivity: User profile retrieved - Type: ${userProfile.userType}, ShopId: ${userProfile.shopId}")
                    
                    when (userProfile.userType) {
                        "shop_owner" -> {
                            // Check if shopkeeper has shop details
                            if (userProfile.shopId.isNullOrEmpty()) {
                                // No shop details - go to AddShop
                                println("LoginActivity: Navigating shop owner to AddShop (no shopId)")
                                startActivity(Intent(this@LoginActivity, AddShopActivity::class.java))
                            } else {
                                // Has shop details - go to ShopkeeperMainActivity
                                println("LoginActivity: Navigating shop owner to ShopkeeperMainActivity")
                                startActivity(Intent(this@LoginActivity, ShopkeeperMainActivity::class.java))
                            }
                        }
                        else -> {
                            // Customer - go to CustomerMainActivity
                            println("LoginActivity: Navigating customer to CustomerMainActivity")
                            startActivity(Intent(this@LoginActivity, CustomerMainActivity::class.java))
                        }
                    }
                } else {
                    // If profile not found, default to customer
                    println("LoginActivity: Profile not found, defaulting to CustomerMainActivity")
                    startActivity(Intent(this@LoginActivity, CustomerMainActivity::class.java))
                }
                finish()
            } catch (e: Exception) {
                println("LoginActivity: Navigation exception: ${e.message}")
                e.printStackTrace()
                // Fallback to customer activity
                startActivity(Intent(this@LoginActivity, CustomerMainActivity::class.java))
                finish()
            }
        }
    }
}
