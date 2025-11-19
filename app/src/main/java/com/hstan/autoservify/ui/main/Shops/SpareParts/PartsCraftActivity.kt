package com.hstan.autoservify.ui.main.Shops.SpareParts

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.Gson
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.Adapters.PartsCraftAdapter
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class PartsCraftActivity : AppCompatActivity() {

    private lateinit var adapter: PartsCraftAdapter
    private lateinit var viewModel: PartsCraftViewModel
    private var specificShopId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partscraft)

        // Setup back button
        findViewById<android.widget.ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }

        // Check if we're viewing parts for a specific shop
        specificShopId = intent.getStringExtra("shop_id")
        val shopName = intent.getStringExtra("shop_name")
        
        // Update title if we're viewing a specific shop
        if (!specificShopId.isNullOrEmpty() && !shopName.isNullOrEmpty()) {
            title = "$shopName - Spare Parts"
            println("PartsCraftActivity: Viewing parts for shop: $specificShopId ($shopName)")
        } else {
            println("PartsCraftActivity: Viewing all parts")
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)


        // Initialize adapter immediately with default values and click listeners
        adapter = PartsCraftAdapter(
            items = mutableListOf(),
            showEditDeleteButtons = false, // Default to customer view
            onEditClick = { partsCraft ->
                // Navigate to edit activity
                val intent = Intent(this, Addpartscraft::class.java)
                intent.putExtra("partscraft_data", Gson().toJson(partsCraft))
                startActivity(intent)
            },
            onDeleteClick = { partsCraft ->
                // Show delete confirmation dialog
                showDeleteConfirmationDialog(partsCraft)
            }
        )
        recyclerView.adapter = adapter
        
        // Then update based on user role
        setupAdapterBasedOnUserRole()

        // Init ViewModel
        viewModel = ViewModelProvider(this).get(PartsCraftViewModel::class.java)

        // Observe LiveData
        viewModel.partsCrafts.observe(this) { partsCrafts ->
            adapter.updateData(partsCrafts)
        }

        // Observe delete status
        lifecycleScope.launch {
            viewModel.isSuccessfullyDeleted.collect { isDeleted ->
                isDeleted?.let {
                    if (it) {
                        Toast.makeText(this@PartsCraftActivity, "Part deleted successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@PartsCraftActivity, "Failed to delete part", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.clearDeleteStatus()
                }
            }
        }

        // Observe failure messages
        lifecycleScope.launch {
            viewModel.failureMessage.collect { message ->
                message?.let {
                    Toast.makeText(this@PartsCraftActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Load data based on user role
        loadDataBasedOnUserRole()

        // Add new spare part
        // Setup Add button based on user role
        setupAddButtonBasedOnUserRole()
    }

    private fun setupAdapterBasedOnUserRole() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                val isShopOwner = if (result.isSuccess) {
                    result.getOrThrow().userType == "shop_owner"
                } else {
                    false // Default to customer if profile not found
                }
                
                // Update the existing adapter permissions without losing data
                adapter.updatePermissions(isShopOwner)
            }
        }
    }

    private fun setupAddButtonBasedOnUserRole() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            val addButton = findViewById<ExtendedFloatingActionButton>(R.id.add_SpareParts)
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                val isShopOwner = if (result.isSuccess) {
                    result.getOrThrow().userType == "shop_owner"
                } else {
                    false
                }
                
                if (isShopOwner) {
                    addButton.visibility = android.view.View.VISIBLE
                    addButton.setOnClickListener {
                        startActivity(Intent(this@PartsCraftActivity, Addpartscraft::class.java))
                    }
                } else {
                    addButton.visibility = android.view.View.GONE
                }
            } else {
                addButton.visibility = android.view.View.GONE
            }
        }
    }

    private fun showDeleteConfirmationDialog(partsCraft: PartsCraft) {
        AlertDialog.Builder(this)
            .setTitle("Delete Spare Part")
            .setMessage("Are you sure you want to delete '${partsCraft.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                partsCraft.id?.let { viewModel.deletePartsCraft(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 🆕 Load data based on user role (role-based access control)
    private fun loadDataBasedOnUserRole() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            // If a specific shop ID is provided, load parts for that shop only
            if (!specificShopId.isNullOrEmpty()) {
                println("Loading parts for specific shop: $specificShopId")
                viewModel.loadPartsCraftsByShopId(specificShopId!!)
                return@launch
            }
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    when (userProfile.userType) {
                        "shop_owner" -> {
                            // Shop owner should only see their own parts
                            val shopId = userProfile.shopId
                            if (!shopId.isNullOrEmpty()) {
                                println("Loading parts for shop owner's shop: $shopId")
                                viewModel.loadPartsCraftsByShopId(shopId)
                            } else {
                                println("Shop owner with no shopId, loading all parts")
                                viewModel.loadPartsCrafts()
                            }
                        }
                        else -> {
                            // Customer should see all parts from all shops (only when not viewing specific shop)
                            println("Loading all parts for customer")
                            viewModel.loadPartsCrafts()
                        }
                    }
                } else {
                    // Profile not found, load all parts
                    println("Profile not found, loading all parts")
                    viewModel.loadPartsCrafts()
                }
            } else {
                // No user logged in, load all parts
                println("No user logged in, loading all parts")
                viewModel.loadPartsCrafts()
            }
        }
    }
}