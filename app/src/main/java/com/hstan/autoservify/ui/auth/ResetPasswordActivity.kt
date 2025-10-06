package com.hstan.autoservify.ui.auth

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hstan.autoservify.databinding.ActivityResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = AuthViewModel()

        // Handle failure messages
        lifecycleScope.launch {
            viewModel.failureMessage.collect { message ->
                hideLoading()
                if (message != null) {
                    Toast.makeText(this@ResetPasswordActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle reset password response
        lifecycleScope.launch {
            viewModel.resetResponse.collect { response ->
                hideLoading()
                if (response != null) {
                    MaterialAlertDialogBuilder(this@ResetPasswordActivity)
                        .setMessage("We have sent you a password reset email. Check your inbox and click the link to reset your password.")
                        .setCancelable(false)
                        .setPositiveButton("Ok") { _, _ ->
                            finish()
                        }
                        .show()
                }
            }
        }

        // Back to previous screen
        binding.signupTxt.setOnClickListener {
            finish()
        }

        // Reset password button
        binding.loginbtn.setOnClickListener {
            val email = binding.email.editText?.text.toString()

            if (!email.contains("@")) {
                Toast.makeText(this, "Invalid Email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading()
            viewModel.resetPassword(email)
        }
    }

    private fun showLoading() {
        binding.loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingOverlay.visibility = View.GONE
    }
}
