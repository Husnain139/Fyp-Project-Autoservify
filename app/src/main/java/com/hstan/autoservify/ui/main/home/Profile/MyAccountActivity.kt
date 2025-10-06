package com.hstan.autoservify.ui.main.home.Profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.hstan.autoservify.DataSource.CloudinaryUploadHelper
import com.hstan.autoservify.databinding.ActivityMyAccountBinding
import com.hstan.autoservify.model.AppUser
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class MyAccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyAccountBinding
    private val authRepo = AuthRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var currentUser: AppUser? = null
    private val cloudinaryHelper = CloudinaryUploadHelper()

    // Gallery image picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    uploadProfileImage(uri.toString())
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            loadUserProfile(uid)
        }

        // Save profile info
        binding.btnSave.setOnClickListener {
            saveChanges()
        }

        // Change picture (click on text OR image)
        binding.changePicture.setOnClickListener { openImagePicker() }
        binding.profileImage.setOnClickListener { openImagePicker() }

        // Back button
        binding.backBtn.setOnClickListener { finish() }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun uploadProfileImage(fileUri: String) {
        Toast.makeText(this, "Uploading profile image...", Toast.LENGTH_SHORT).show()

        cloudinaryHelper.uploadFile(fileUri) { success, url ->
            if (success && url != null) {
                val uid = firebaseAuth.currentUser?.uid ?: return@uploadFile
                lifecycleScope.launch {
                    val updateResult = authRepo.updateProfileImage(uid, url)
                    if (updateResult.isSuccess) {
                        Toast.makeText(this@MyAccountActivity, "Profile image updated", Toast.LENGTH_SHORT).show()
                        Glide.with(this@MyAccountActivity).load(url).into(binding.profileImage)
                        currentUser?.profileImageUrl = url
                    } else {
                        Toast.makeText(this@MyAccountActivity, "Failed to update image in Firestore", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Image upload failed: $url", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserProfile(uid: String) {
        lifecycleScope.launch {
            val result = authRepo.getUserProfile(uid)
            if (result.isSuccess) {
                currentUser = result.getOrNull()
                currentUser?.let { user ->
                    binding.editName.setText(user.name)
                    binding.editEmail.setText(user.email)
                    binding.editPhone.setText(user.phone)
                    binding.editPassword.setText("")

                    if (!user.profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@MyAccountActivity).load(user.profileImageUrl).into(binding.profileImage)
                    }
                }
            } else {
                Toast.makeText(this@MyAccountActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveChanges() {
        val name = binding.editName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()

        val uid = firebaseAuth.currentUser?.uid ?: return
        val updatedUser = AppUser(
            uid = uid,
            name = name,
            email = email,
            phone = phone,
            userType = currentUser?.userType ?: "customer",
            shopId = currentUser?.shopId,
            profileImageUrl = currentUser?.profileImageUrl
        )

        lifecycleScope.launch {
            val saveResult = authRepo.saveUserProfile(updatedUser)
            if (saveResult.isSuccess) {
                Toast.makeText(this@MyAccountActivity, "Profile updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MyAccountActivity, "Error saving profile", Toast.LENGTH_SHORT).show()
            }

            if (password.isNotEmpty()) {
                val passResult = authRepo.updatePassword(password)
                if (passResult.isSuccess) {
                    Toast.makeText(this@MyAccountActivity, "Password updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MyAccountActivity, "Error updating password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
