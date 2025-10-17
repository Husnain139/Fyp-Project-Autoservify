package com.hstan.autoservify.ui.shopkeeper

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.main.Shops.AddShopActivity
import com.hstan.autoservify.ui.main.Shops.Shop
import kotlinx.coroutines.launch
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.AppointmentRepository
import com.hstan.autoservify.ui.Adapters.DashboardOrderAdapter
import com.hstan.autoservify.ui.Adapters.DashboardAppointmentAdapter
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.databinding.FragmentShopkeeperDashboardBinding

class ShopkeeperDashboardFragment : Fragment() {

    lateinit var dashboardOrderAdapter: DashboardOrderAdapter
    lateinit var dashboardAppointmentAdapter: DashboardAppointmentAdapter
    lateinit var binding: FragmentShopkeeperDashboardBinding
    
    private var currentShop: Shop? = null
    
    // Data loading state management
    private var isDataLoaded = false
    private var isDataLoading = false
    
    // Sales calculation variables
    private var todayOrdersAmount = 0
    private var todayAppointmentsAmount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentShopkeeperDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize adapters
        dashboardOrderAdapter = DashboardOrderAdapter(emptyList())
        dashboardAppointmentAdapter = DashboardAppointmentAdapter(emptyList())

        // Setup RecyclerViews for orders and appointments
        binding.recentOrdersRecyclerview.adapter = dashboardOrderAdapter
        binding.recentOrdersRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        binding.recentAppointmentsRecyclerview.adapter = dashboardAppointmentAdapter
        binding.recentAppointmentsRecyclerview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Setup click listeners
        setupClickListeners()

        // Load shopkeeper data only if not already loaded or loading
        if (!isDataLoaded && !isDataLoading) {
            // Show loading state immediately
            showLoadingState()
            loadShopkeeperData()
        } else if (isDataLoaded) {
            // Data is already loaded, show it immediately without animation delay
            showLoadedState()
        }
    }

    private fun setupClickListeners() {
        binding.viewAllOrdersText.setOnClickListener {
            val intent = Intent(requireContext(), com.hstan.autoservify.ui.shopkeeper.OrdersActivity::class.java)
            startActivity(intent)
        }

        binding.viewAllAppointmentsText.setOnClickListener {
            val intent = Intent(requireContext(), com.hstan.autoservify.ui.shopkeeper.AppointmentsActivity::class.java)
            startActivity(intent)
        }

        binding.editShopButton.setOnClickListener {
            currentShop?.let { shop ->
                val intent = Intent(requireContext(), AddShopActivity::class.java)
                intent.putExtra("shopData", com.google.gson.Gson().toJson(shop))
                startActivity(intent)
            }
        }
    }

    private fun setupMaterial3Animations() {
        // Simple, clean fade-in animation
        binding.root.alpha = 0f
        binding.root.animate()
            .alpha(1f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun showLoadingState() {
        // Hide content while loading to prevent flash of old data
        binding.root.alpha = 0.3f
        
        // Set loading placeholders
        binding.shopkeeperNameText.text = "Loading..."
        binding.shopNameText.text = "Loading shop info..."
        binding.shopLocationText.text = ""
        
        // Set loading stats
        binding.salesCount.text = "--"
        binding.ordersPendingCount.text = "--"
        binding.appointmentsPendingCount.text = "--"
        binding.averageReview.text = "--"
    }

    private fun showLoadedState() {
        // Show content with smooth animation
        binding.root.animate()
            .alpha(1f)
            .setDuration(150)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun loadShopkeeperData() {
        if (isDataLoading) return // Prevent multiple simultaneous loads
        
        isDataLoading = true
        lifecycleScope.launch {
            try {
                val authRepository = AuthRepository()
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        // Update shopkeeper name
                        updateShopkeeperName(userProfile.name ?: userProfile.email ?: "Shopkeeper")
                        
                        if (userProfile.userType == "shop_owner") {
                            userProfile.shopId?.let { shopId ->
                                // Load all data concurrently but wait for completion
                                loadShopData(shopId)
                                loadDashboardStats(shopId)
                                loadRecentOrders(shopId)
                                loadRecentAppointments(shopId)
                                
                                // Mark as loaded and show content
                                isDataLoaded = true
                                showLoadedState()
                            } ?: run {
                                println("ShopkeeperDashboard: ERROR - User has no shopId!")
                                showLoadedState() // Show even with error
                            }
                        } else {
                            println("ShopkeeperDashboard: ERROR - User is not a shop_owner, type: ${userProfile.userType}")
                            showLoadedState() // Show even with error
                        }
                    }
                }
            } catch (e: Exception) {
                println("ShopkeeperDashboard: Error loading data: ${e.message}")
                showLoadedState() // Show even with error
            } finally {
                isDataLoading = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from other fragments, but only if data was previously loaded
        if (isDataLoaded && !isDataLoading) {
            // Refresh data silently without showing loading state
            refreshDashboardData()
        }
    }

    private fun refreshDashboardData() {
        lifecycleScope.launch {
            try {
                val authRepository = AuthRepository()
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        if (userProfile.userType == "shop_owner") {
                            userProfile.shopId?.let { shopId ->
                                // Refresh data without showing loading state
                                loadDashboardStats(shopId)
                                loadRecentOrders(shopId)
                                loadRecentAppointments(shopId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("ShopkeeperDashboard: Error refreshing data: ${e.message}")
            }
        }
    }

    private fun loadShopData(shopId: String) {
        lifecycleScope.launch {
            val shopRepository = ShopRepository()
            shopRepository.getShops().collect { shops ->
                val userShop = shops.find { it.id == shopId }
                userShop?.let { shop ->
                    currentShop = shop
                    updateShopInfo(shop)
                }
            }
        }
    }

    private fun updateShopInfo(shop: Shop) {
        binding.shopNameText.text = shop.title
        binding.shopLocationText.text = shop.address
        // You can load shop image here if needed
        // Glide.with(this).load(shop.imageUrl).into(binding.shopImage)
    }
    
    private fun updateShopkeeperName(userName: String) {
        binding.shopkeeperNameText.text = userName.ifEmpty { "Shopkeeper" }
    }

    private fun loadDashboardStats(shopId: String) {
        lifecycleScope.launch {
            try {
                val orderRepository = OrderRepository()
                val appointmentRepository = AppointmentRepository()
                
                // Get today's date for filtering
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                
                // Load orders and calculate statistics
                orderRepository.getShopOrders(shopId).collect { orders ->
                    // Filter today's orders
                    val todayOrders = orders.filter { order ->
                        order.orderDate.contains(today)
                    }
                    
                    // Calculate total amount from today's orders
                    todayOrdersAmount = todayOrders.sumOf { order ->
                        (order.item?.price ?: 0) * order.quantity
                    }
                    
                    // Orders pending (orders with "pending" or "Order Placed" status)
                    val pendingOrders = orders.count { order ->
                        order.status.contains("pending", ignoreCase = true) || 
                        order.status.contains("Order Placed", ignoreCase = true)
                    }
                    
                    // Update UI with total sales amount
                    updateTotalSales()
                    binding.ordersPendingCount.text = pendingOrders.toString()
                }
                
                // Load appointments and calculate statistics
                appointmentRepository.getShopAppointments(shopId).collect { appointments ->
                    // Filter appointments that actually match our shopId (in case of data inconsistencies)
                    // Also handle null/empty shopId values from old data
                    val validAppointments = appointments.filter { appointment ->
                        val appointmentShopId = appointment.shopId?.trim() ?: ""
                        val currentShopId = shopId.trim()
                        appointmentShopId.isNotEmpty() && appointmentShopId == currentShopId
                    }
                    
                    // Filter today's appointments
                    val todayAppointments = validAppointments.filter { appointment ->
                        appointment.appointmentDate == today
                    }
                    
                    // Calculate total amount from today's appointments
                    todayAppointmentsAmount = todayAppointments.sumOf { appointment ->
                        appointment.bill.toIntOrNull() ?: 0
                    }
                    
                    // Update UI with today's appointment count
                    binding.appointmentsPendingCount.text = todayAppointments.size.toString()
                    
                    // Update total sales with appointments amount
                    updateTotalSales()
                }
                
                // For now, set a default review score (you can implement actual review system later)
                binding.averageReview.text = "4.8"
                
            } catch (e: Exception) {
                // Handle error, set default values
                binding.salesCount.text = "Rs 0"
                binding.ordersPendingCount.text = "0"
                binding.appointmentsPendingCount.text = "0"
                binding.averageReview.text = "4.8"
                println("Error loading dashboard stats: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun updateTotalSales() {
        val totalSales = todayOrdersAmount + todayAppointmentsAmount
        binding.salesCount.text = "Rs $totalSales"
    }

    private fun loadRecentOrders(shopId: String) {
        lifecycleScope.launch {
            val orderRepository = OrderRepository()
            orderRepository.getShopOrders(shopId).collect { orders ->
                val recentOrders = orders.take(3)
                dashboardOrderAdapter.updateData(recentOrders)
            }
        }
    }

    private fun loadRecentAppointments(shopId: String) {
        lifecycleScope.launch {
            val appointmentRepository = AppointmentRepository()
            appointmentRepository.getRecentShopAppointments(shopId, 3).collect { appointments ->
                dashboardAppointmentAdapter.updateData(appointments)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset loading states when view is destroyed
        isDataLoaded = false
        isDataLoading = false
    }
}
