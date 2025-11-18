package com.hstan.autoservify.ui.main.Shops.Services

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.hstan.autoservify.DataSource.CloudinaryUploadHelper
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ActivityAddServiceBinding
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class Add_Service_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityAddServiceBinding
    private val viewModel: ServiceViewModel by viewModels()
    private var isEditMode = false
    private var serviceToEdit: Service? = null
    private var imageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private var isImageUploading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if we're in edit mode
        val serviceJson = intent.getStringExtra("service_data")
        if (serviceJson != null) {
            isEditMode = true
            serviceToEdit = Gson().fromJson(serviceJson, Service::class.java)
            
            // Update UI for edit mode
            binding.textView6.text = "Edit Service"
            binding.AddServiceButton.text = "Update Service"
            
            // Pre-fill the form
            serviceToEdit?.let { service ->
                binding.name.setText(service.name)
                binding.descript.setText(service.description)
                binding.price1.setText(service.price.toString())
            }
        }

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, ServicesActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Image upload click listener
        binding.srvImage.setOnClickListener {
            chooseImageFromGallery()
        }

        binding.AddServiceButton.setOnClickListener {
            if (binding.name.text.isNullOrBlank() || 
                binding.descript.text.isNullOrBlank() || 
                binding.price1.text.isNullOrBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isImageUploading) {
                Toast.makeText(this, "Please wait, image is uploading...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEditMode && serviceToEdit != null) {
                // Update existing service
                val updatedService = serviceToEdit!!.copy(
                    name = binding.name.text.toString(),
                    description = binding.descript.text.toString(),
                    price = binding.price1.text.toString().toDoubleOrNull() ?: 0.0,
                    imageUrl = uploadedImageUrl ?: serviceToEdit!!.imageUrl // Keep existing image if not changed
                )
                binding.loadingOverlay.visibility = View.VISIBLE
                binding.loadingText.text = "Updating service..."
                viewModel.updateService(updatedService)
            } else {
                // Create new service - get shopId from user profile
                lifecycleScope.launch {
                    val authRepository = AuthRepository()
                    val currentUser = authRepository.getCurrentUser()
                    
                    if (currentUser != null) {
                        val result = authRepository.getUserProfile(currentUser.uid)
                        if (result.isSuccess) {
                            val userProfile = result.getOrThrow()
                            val shopId = userProfile.shopId ?: ""
                            
                            val service = Service(
                                id = FirebaseFirestore.getInstance().collection("services").document().id,
                                name = binding.name.text.toString(),
                                description = binding.descript.text.toString(),
                                price = binding.price1.text.toString().toDoubleOrNull() ?: 0.0,
                                rating = 0.0,
                                shopId = shopId,
                                imageUrl = uploadedImageUrl ?: "" // Save uploaded image URL
                            )
                            
                            binding.loadingOverlay.visibility = View.VISIBLE
                            binding.loadingText.text = "Adding service..."
                            viewModel.addService(service)
                        } else {
                            Toast.makeText(this@Add_Service_Activity, "Unable to get user profile", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@Add_Service_Activity, "Please login first", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Observe save result
        viewModel.isSuccessfullySaved.observe(this) { success ->
            binding.loadingOverlay.visibility = View.GONE
            if (success == true) {
                Toast.makeText(this, "Service Added Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else if (success == false) {
                Toast.makeText(this, "Failed to add service", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe update result
        viewModel.isSuccessfullyUpdated.observe(this) { success ->
            binding.loadingOverlay.visibility = View.GONE
            if (success == true) {
                Toast.makeText(this, "Service Updated Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else if (success == false) {
                Toast.makeText(this, "Failed to update service", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe errors
        viewModel.failureMessage.observe(this) { error ->
            binding.loadingOverlay.visibility = View.GONE
            error?.let {
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun chooseImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageUri = result.data?.data
            if (imageUri != null) {
                binding.srvImage.setImageURI(imageUri)
                uploadImageToCloudinary(imageUri!!)
            } else {
                Log.e("Gallery", "No image selected")
            }
        }
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        val uploader = CloudinaryUploadHelper()
        isImageUploading = true
        
        // Show uploading state
        binding.uploadProgressOverlay.visibility = View.VISIBLE
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        uploader.uploadFile(uri.toString()) { success, result ->
            runOnUiThread {
                isImageUploading = false
                binding.uploadProgressOverlay.visibility = View.GONE
                
                if (success) {
                    uploadedImageUrl = result
                    Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    // Upload failed - but service can still be created without image
                    uploadedImageUrl = null
                    Toast.makeText(this, "Image upload failed. You can continue without an image.", Toast.LENGTH_LONG).show()
                    binding.srvImage.setImageResource(R.drawable.addimage)
                }
            }
        }
    }
}
