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
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.hstan.autoservify.DataSource.CloudinaryUploadHelper
import com.hstan.autoservify.ui.auth.LoginActivity
import com.hstan.autoservify.ui.shopkeeper.ShopkeeperDashboardFragment
import com.google.gson.Gson
import com.bumptech.glide.Glide

class AddShopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddShopBinding

    // ✅ Image picker for gallery
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.AddShopImage.setImageURI(uri)
            uploadImageToCloudinary(uri)
        }
    }

    private lateinit var viewModel: AddShopViewModel
    private var uploadedImageUrl: String? = null   // ✅ store image URL
    private var isImageUploading = false
    private var isEditMode = false
    private var existingShop: Shop? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Initialize Cloudinary
        CloudinaryUploadHelper.initializeCloudinary(this)

        viewModel = AddShopViewModel()

        lifecycleScope.launch {
            viewModel.isSuccessfullySaved.collect {
                it?.let {
                    if (it) {
                        val message = if (isEditMode) "Shop updated successfully!" else "Shop added successfully!"
                        Toast.makeText(this@AddShopActivity, message, Toast.LENGTH_SHORT).show()
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

        // Check if we're in edit mode
        val shopDataJson = intent.getStringExtra("shopData")
        if (shopDataJson != null) {
            isEditMode = true
            existingShop = Gson().fromJson(shopDataJson, Shop::class.java)
            loadExistingShopData()
        }

        // ✅ Pick image from gallery
        binding.AddShopImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, com.hstan.autoservify.ui.shopkeeper.ShopkeeperMainActivity::class.java)
            startActivity(intent)
            finish() // optional: closes current activity
        }

        // ✅ Submit button
        binding.submitButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            val description = binding.descript.text.toString().trim()
            val address = binding.addressInput.text.toString().trim()
            val city = binding.cityInput.text.toString().trim()
            val phone = binding.phoneInput.text.toString().trim()
            val email = binding.emailInput.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || address.isEmpty() || city.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isImageUploading) {
                Toast.makeText(this, "Please wait, image is uploading...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()

            if (currentUser != null) {
                if (isEditMode && existingShop != null) {
                    // Update existing shop
                    val shop = existingShop!!.apply {
                        this.title = title
                        this.description = description
                        this.address = address
                        this.city = city
                        this.phone = phone
                        this.email = email
                        // Update image URL only if a new image was uploaded
                        if (!uploadedImageUrl.isNullOrEmpty()) {
                            this.imageUrl = uploadedImageUrl!!
                        }
                    }
                    viewModel.updateShop(shop)
                } else {
                    // Create new shop
                    val shop = Shop().apply {
                        this.title = title
                        this.description = description
                        this.address = address
                        this.city = city
                        this.phone = phone
                        this.email = email
                        this.ownerId = currentUser.uid
                        this.ownerName = currentUser.displayName ?: ""
                        // Use uploaded image URL if available, otherwise use empty string
                        // The app will show a default placeholder for shops without images
                        this.imageUrl = uploadedImageUrl ?: ""
                    }
                    viewModel.saveShop(shop)
                }
            } else {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadExistingShopData() {
        existingShop?.let { shop ->
            // Update UI for edit mode
            binding.headerText.text = "Edit Your Shop"
            binding.submitButton.text = "Update Shop"

            // Pre-fill all fields
            binding.titleInput.setText(shop.title)
            binding.descript.setText(shop.description)
            binding.addressInput.setText(shop.address)
            binding.cityInput.setText(shop.city)
            binding.phoneInput.setText(shop.phone)
            binding.emailInput.setText(shop.email)

            // Load existing image
            uploadedImageUrl = shop.imageUrl
            if (shop.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(shop.imageUrl)
                    .placeholder(com.hstan.autoservify.R.drawable.addimage2)
                    .error(com.hstan.autoservify.R.drawable.addimage2)
                    .centerCrop()
                    .into(binding.AddShopImage)
            }
        }
    }

    // ✅ upload image to Cloudinary
    private fun uploadImageToCloudinary(uri: Uri) {
        val uploader = CloudinaryUploadHelper()
        isImageUploading = true
        
        // Show uploading state
        binding.uploadProgressText.visibility = android.view.View.VISIBLE
        binding.uploadProgressBar.visibility = android.view.View.VISIBLE
        binding.uploadSuccessIcon.visibility = android.view.View.GONE
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        uploader.uploadFile(uri.toString()) { success, result ->
            runOnUiThread {
                isImageUploading = false
                binding.uploadProgressText.visibility = android.view.View.GONE
                binding.uploadProgressBar.visibility = android.view.View.GONE
                
                if (success) {
                    uploadedImageUrl = result
                    Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                    binding.uploadSuccessIcon.visibility = android.view.View.VISIBLE
                } else {
                    // Upload failed - but shop can still be created without image
                    uploadedImageUrl = null
                    Toast.makeText(this, "Image upload failed. You can continue without an image or try again.", Toast.LENGTH_LONG).show()
                    binding.AddShopImage.setImageResource(com.hstan.autoservify.R.drawable.addimage2)
                    binding.uploadSuccessIcon.visibility = android.view.View.GONE
                }
            }
        }
    }
}
