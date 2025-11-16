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

class AddShopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddShopBinding

    // ✅ Declare here — top level inside Activity class
   // private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
     //   uri?.let {
         //   binding.AddShopImage.setImageURI(uri)
           // uploadImageToCloudinary(uri)
        //}
    //}

    private lateinit var viewModel: AddShopViewModel
    private var uploadedImageUrl: String? = null   // ✅ store image URL



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
                        Toast.makeText(this@AddShopActivity, "Shop added successfully!", Toast.LENGTH_SHORT).show()
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

        // ✅ Pick image from gallery
       // binding.AddShopImage.setOnClickListener {
         //   imagePicker.launch("image/*")
        //}

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, ShopkeeperDashboardFragment::class.java)
            startActivity(intent)
            finish() // optional: closes current activity
        }

        // ✅ Submit button
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

            if (uploadedImageUrl.isNullOrEmpty()) {
                Toast.makeText(this, "Please upload a shop image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                    this.imageUrl = uploadedImageUrl ?: ""   // ✅ save Cloudinary image URL
                }

                viewModel.saveShop(shop)
            } else {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ upload image to Cloudinary
    //private fun uploadImageToCloudinary(uri: Uri) {
      //  val uploader = CloudinaryUploadHelper()
        //Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

        //uploader.uploadFile(uri.toString()) { success, result ->
          //  runOnUiThread {
            //    if (success) {
              //      uploadedImageUrl = result
                //    Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                //} else {
                  //  Toast.makeText(this, "Upload failed: $result", Toast.LENGTH_SHORT).show()
                //}
            //}
        //}
    //}
}
