package com.hstan.autoservify.ui.main.Shops.SpareParts

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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ActivityAddpartscraftBinding
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class Addpartscraft : AppCompatActivity() {
    private var uri: Uri? = null
    lateinit var binding: ActivityAddpartscraftBinding
    lateinit var addViewModel: AddpartcraftViewModel
    lateinit var updateViewModel: PartsCraftViewModel
    private var isEditMode = false
    private var partsCraftToEdit: PartsCraft? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddpartscraftBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addViewModel = AddpartcraftViewModel()
        updateViewModel = ViewModelProvider(this).get(PartsCraftViewModel::class.java)

        // Check if we're in edit mode
        val partsCraftJson = intent.getStringExtra("partscraft_data")
        if (partsCraftJson != null) {
            isEditMode = true
            partsCraftToEdit = Gson().fromJson(partsCraftJson, PartsCraft::class.java)
            
            // Update UI for edit mode
            binding.textView6.text = "Edit Spare Part"
            binding.submitButton.text = "Update Spare Part"
            
            // Pre-fill the form
            partsCraftToEdit?.let { part ->
                binding.ename.setText(part.title)
                binding.editTextText3.setText(part.description)
                binding.editTextText4.setText(part.price.toString())
                
                // Pre-fill inventory fields
                binding.manageInventoryCheckbox.isChecked = part.manageInventory
                if (part.manageInventory) {
                    binding.inventoryFieldsContainer.visibility = View.VISIBLE
                    binding.quantityInput.setText(part.quantity.toString())
                    binding.lowStockLimitInput.setText(part.lowStockLimit.toString())
                }
                
                // Load existing image
                if (part.image.isNotEmpty()) {
                    Glide.with(this)
                        .load(part.image)
                        .into(binding.spImage)
                }
            }
        }

        // Setup checkbox listener for inventory management
        binding.manageInventoryCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.inventoryFieldsContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, PartsCraftActivity::class.java)
            startActivity(intent)
            finish() // optional: closes current activity
        }
        // Observe add operations
        lifecycleScope.launch {
            addViewModel.isSuccessfullySaved.collect {
                it?.let {
                    binding.loadingOverlay.visibility = View.GONE
                    if (it == true) {
                        Toast.makeText(
                            this@Addpartscraft,
                            "Spare part saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }

        // Observe update operations
        lifecycleScope.launch {
            updateViewModel.isSuccessfullyUpdated.collect {
                it?.let {
                    if (it == true) {
                        Toast.makeText(
                            this@Addpartscraft,
                            "Successfully updated",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }

        // Observe failure messages
        lifecycleScope.launch {
            addViewModel.failureMessage.collect {
                it?.let {
                    binding.loadingOverlay.visibility = View.GONE
                    Toast.makeText(this@Addpartscraft, "Error: $it", Toast.LENGTH_LONG).show()
                }
            }
        }

        lifecycleScope.launch {
            updateViewModel.failureMessage.collect {
                it?.let {
                    Toast.makeText(this@Addpartscraft, it, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.submitButton.setOnClickListener {
            val title = binding.ename.text.toString().trim()
            val description = binding.editTextText3.text.toString().trim()
            val priceText = binding.editTextText4.text.toString().trim()

            // Validate the input fields
            if (title.isEmpty() || description.isEmpty() || priceText.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceText.toIntOrNull()

            if (price == null) {
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get inventory management values
            val manageInventory = binding.manageInventoryCheckbox.isChecked
            var quantity = 0
            var lowStockLimit = 10

            if (manageInventory) {
                val quantityText = binding.quantityInput.text.toString().trim()
                val lowStockLimitText = binding.lowStockLimitInput.text.toString().trim()

                if (quantityText.isEmpty() || lowStockLimitText.isEmpty()) {
                    Toast.makeText(this, "Please fill in inventory fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                quantity = quantityText.toIntOrNull() ?: 0
                lowStockLimit = lowStockLimitText.toIntOrNull() ?: 10

                if (quantity < 0 || lowStockLimit < 0) {
                    Toast.makeText(this, "Quantity and low stock limit must be non-negative", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (isEditMode && partsCraftToEdit != null) {
                // Update existing parts craft
                val updatedPartsCraft = PartsCraft().apply {
                    id = partsCraftToEdit!!.id
                    this.title = title
                    this.price = price
                    this.description = description
                    this.shopId = partsCraftToEdit!!.shopId  // Keep existing shopId
                    // Keep existing image if no new image selected
                    this.image = if (uri != null) "" else partsCraftToEdit!!.image
                    // Update inventory fields
                    this.manageInventory = manageInventory
                    this.quantity = quantity
                    this.lowStockLimit = lowStockLimit
                }

                if (uri == null) {
                    updateViewModel.updatePartsCraft(updatedPartsCraft)
                } else {
                    // TODO: Handle image update - would need to modify repository to handle image updates
                    // For now, just update without changing image
                    Toast.makeText(this, "Image update not yet implemented", Toast.LENGTH_SHORT).show()
                    updateViewModel.updatePartsCraft(updatedPartsCraft)
                }
            } else {
                // Create new parts craft
                val partsCraft = PartsCraft()
                partsCraft.title = title
                partsCraft.price = price
                partsCraft.description = description
                // Set inventory fields
                partsCraft.manageInventory = manageInventory
                partsCraft.quantity = quantity
                partsCraft.lowStockLimit = lowStockLimit
                
                // Set shopId from current user's profile
                lifecycleScope.launch {
                    val authRepository = AuthRepository()
                    val currentUser = authRepository.getCurrentUser()
                    if (currentUser != null) {
                        val result = authRepository.getUserProfile(currentUser.uid)
                        if (result.isSuccess) {
                            val userProfile = result.getOrThrow()
                            if (userProfile.userType == "shop_owner" && !userProfile.shopId.isNullOrEmpty()) {
                                partsCraft.shopId = userProfile.shopId!!
                                
                                // Save the parts craft with shopId
                                if (uri == null) {
                                    binding.loadingOverlay.visibility = View.VISIBLE
                                    binding.loadingText.text = "Saving spare part..."
                                    addViewModel.saveHandCraft(partsCraft)
                                } else {
                                    binding.loadingOverlay.visibility = View.VISIBLE
                                    binding.loadingText.text = "Uploading image..."
                                    addViewModel.uploadImageAndSaveHandCraft(uri.toString(), partsCraft)
                                }
                            } else {
                                Toast.makeText(this@Addpartscraft, "Only shop owners can add spare parts", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@Addpartscraft, "Unable to verify user profile", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@Addpartscraft, "Please login first", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.spImage.setOnClickListener {
            chooseImageFromGallery()
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
            uri = result.data?.data
            if (uri != null) {
                binding.spImage.setImageURI(uri)
            } else {
                Log.e("Gallery", "No image selected")
            }
        }
    }

}
