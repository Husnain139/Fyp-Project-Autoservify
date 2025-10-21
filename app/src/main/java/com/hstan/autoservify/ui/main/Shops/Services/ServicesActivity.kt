package com.hstan.autoservify.ui.main.Shops.Services

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.gson.Gson
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.Adapters.ServiceAdapter
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class ServicesActivity : AppCompatActivity() {

    private lateinit var adapter: ServiceAdapter
    private lateinit var viewModel: ServiceViewModel
    private var specificShopId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        // Check if we're viewing services for a specific shop
        specificShopId = intent.getStringExtra("shop_id")
        val shopName = intent.getStringExtra("shop_name")
        
        // Update title if we're viewing a specific shop
        if (!specificShopId.isNullOrEmpty() && !shopName.isNullOrEmpty()) {
            title = "$shopName - Services"
            println("ServicesActivity: Viewing services for shop: $specificShopId ($shopName)")
        } else {
            println("ServicesActivity: Viewing all services")
        }

        val recyclerView = findViewById<RecyclerView>(R.id.services_recyclerview)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter immediately with default values and click listeners
        adapter = ServiceAdapter(
            items = mutableListOf(),
            onItemClick = { service ->
                val intent = Intent(this, Service_Detail_Activity::class.java)
                intent.putExtra("data", Gson().toJson(service))
                startActivity(intent)
            },
            showEditDeleteButtons = false, // Default to customer view
            onEditClick = { service ->
                // Navigate to edit activity
                val intent = Intent(this, Add_Service_Activity::class.java)
                intent.putExtra("service_data", Gson().toJson(service))
                startActivity(intent)
            },
            onDeleteClick = { service ->
                // Show delete confirmation dialog
                showDeleteConfirmationDialog(service)
            }
        )
        recyclerView.adapter = adapter

        // Then update based on user role
        setupAdapterBasedOnUserRole()

        viewModel = ViewModelProvider(this).get(ServiceViewModel::class.java)

        // Observe list of services
        viewModel.services.observe(this) { services ->
            services?.let { adapter.updateData(it) }
        }

        // Observe delete status
        viewModel.isSuccessfullyDeleted.observe(this) { isDeleted ->
            isDeleted?.let {
                if (it) {
                    Toast.makeText(this, "Service deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete service", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearDeleteStatus()
            }
        }

        // Observe failure messages
        viewModel.failureMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // Load data based on user role
        loadDataBasedOnUserRole()

        // FAB → go to Add Service screen
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
            val addButton = findViewById<ExtendedFloatingActionButton>(R.id.add_Service)
            
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
                        startActivity(Intent(this@ServicesActivity, Add_Service_Activity::class.java))
                    }
                } else {
                    addButton.visibility = android.view.View.GONE
                }
            } else {
                addButton.visibility = android.view.View.GONE
            }
        }
    }

    private fun showDeleteConfirmationDialog(service: Service) {
        AlertDialog.Builder(this)
            .setTitle("Delete Service")
            .setMessage("Are you sure you want to delete '${service.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                service.id?.let { viewModel.deleteService(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 🆕 Load data based on user role (role-based access control)
    private fun loadDataBasedOnUserRole() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            // If a specific shop ID is provided, load services for that shop only
            if (!specificShopId.isNullOrEmpty()) {
                println("Loading services for specific shop: $specificShopId")
                viewModel.loadServicesByShopId(specificShopId!!)
                return@launch
            }
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    when (userProfile.userType) {
                        "shop_owner" -> {
                            // Shop owner should only see their own services
                            val shopId = userProfile.shopId
                            if (!shopId.isNullOrEmpty()) {
                                println("Loading services for shop owner's shop: $shopId")
                                viewModel.loadServicesByShopId(shopId)
                            } else {
                                println("Shop owner with no shopId, loading all services")
                                viewModel.loadServices()
                            }
                        }
                        else -> {
                            // Customer should see all services from all shops (only when not viewing specific shop)
                            println("Loading all services for customer")
                            viewModel.loadServices()
                        }
                    }
                } else {
                    // Profile not found, load all services
                    println("Profile not found, loading all services")
                    viewModel.loadServices()
                }
            } else {
                // No user logged in, load all services
                println("No user logged in, loading all services")
                viewModel.loadServices()
            }
        }
    }
}
