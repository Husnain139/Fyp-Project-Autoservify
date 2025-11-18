package com.hstan.autoservify.ui.orders

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ActivityAppointmentDetailBinding
import com.hstan.autoservify.model.repositories.AppointmentRepository
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.ReviewRepository
import com.hstan.autoservify.model.repositories.ServiceRepository
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.reviews.ReviewDialog
import kotlinx.coroutines.launch

class AppointmentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppointmentDetailBinding
    private lateinit var appointment: Appointment
    private val orderRepository = OrderRepository()
    private val authRepository = AuthRepository()
    private val serviceRepository = ServiceRepository()
    private val appointmentRepository = AppointmentRepository()
    private val reviewRepository = ReviewRepository()
    private lateinit var sparePartsAdapter: AppointmentSparePartsAdapter
    private val spareParts = mutableListOf<Order>()
    private var servicePrice: Double = 0.0
    private var currentUserId: String = ""
    private var currentUserType: String = ""
    private var hasReviewed: Boolean = false

    companion object {
        const val EXTRA_APPOINTMENT = "extra_appointment"
        const val REQUEST_ADD_PARTS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppointmentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set title and back button
        title = "Appointment Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get appointment from intent
        val appointmentJson = intent.getStringExtra(EXTRA_APPOINTMENT)
        if (appointmentJson == null) {
            Toast.makeText(this, "Error: No appointment data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        appointment = Gson().fromJson(appointmentJson, Appointment::class.java)

        setupUI()
        setupRecyclerView()
        loadServicePrice()
        loadSpareParts()
        checkUserType()

        binding.createOrderButton.setOnClickListener {
            openAddPartsActivity()
        }

        binding.confirmButton.setOnClickListener {
            updateAppointmentStatus("Confirmed")
        }

        binding.markDeliveredButton.setOnClickListener {
            updateAppointmentStatus("Completed")
        }
        
        binding.leaveReviewBtn.setOnClickListener {
            showReviewDialog()
        }
    }

    private fun setupUI() {
        binding.apply {
            serviceNameText.text = appointment.serviceName
            appointmentDateText.text = appointment.appointmentDate
            appointmentTimeText.text = appointment.appointmentTime
            customerNameText.text = appointment.userName
            
            // Set status with color
            statusText.text = appointment.status
            when (appointment.status.lowercase()) {
                "pending" -> {
                    statusText.setBackgroundColor(Color.parseColor("#FF9800"))
                    statusText.setTextColor(Color.WHITE)
                }
                "confirmed" -> {
                    statusText.setBackgroundColor(Color.parseColor("#4CAF50"))
                    statusText.setTextColor(Color.WHITE)
                }
                "completed" -> {
                    statusText.setBackgroundColor(Color.parseColor("#2196F3"))
                    statusText.setTextColor(Color.WHITE)
                }
                else -> {
                    statusText.setBackgroundColor(Color.parseColor("#9E9E9E"))
                    statusText.setTextColor(Color.WHITE)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        sparePartsAdapter = AppointmentSparePartsAdapter(spareParts)
        binding.sparePartsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AppointmentDetailActivity)
            adapter = sparePartsAdapter
        }
    }

    private fun loadServicePrice() {
        lifecycleScope.launch {
            try {
                // Try to get price from appointment.bill first
                if (appointment.bill.isNotEmpty()) {
                    servicePrice = appointment.bill.toDoubleOrNull() ?: 0.0
                    updatePrices()
                } else {
                    // Otherwise fetch from service
                    val service = serviceRepository.getServiceById(appointment.serviceId)
                    if (service != null) {
                        servicePrice = service.price
                    } else {
                        // Default to 0 if service not found
                        servicePrice = 0.0
                    }
                    updatePrices()
                }
            } catch (e: Exception) {
                println("Error loading service price: ${e.message}")
                servicePrice = 0.0
                updatePrices()
            }
        }
    }

    private fun loadSpareParts() {
        lifecycleScope.launch {
            try {
                // Use appointment.id as the bookingId
                val bookingId = appointment.id.ifEmpty { appointment.appointmentId }
                orderRepository.getOrdersByBookingId(bookingId).collect { orders ->
                    spareParts.clear()
                    spareParts.addAll(orders)
                    sparePartsAdapter.notifyDataSetChanged()
                    
                    // Show/hide empty state
                    if (orders.isEmpty()) {
                        binding.sparePartsRecyclerView.visibility = View.GONE
                        binding.noPartsText.visibility = View.VISIBLE
                    } else {
                        binding.sparePartsRecyclerView.visibility = View.VISIBLE
                        binding.noPartsText.visibility = View.GONE
                    }
                    
                    updatePrices()
                }
            } catch (e: Exception) {
                println("AppointmentDetailActivity: Error loading spare parts: ${e.message}")
                // Silently handle error - no toast message
            }
        }
    }

    private fun updatePrices() {
        val sparePartsTotal = spareParts.sumOf { 
            (it.item?.price ?: 0) * it.quantity.toDouble()
        }
        val totalAmount = servicePrice + sparePartsTotal

        binding.apply {
            serviceAmountText.text = "Rs. ${String.format("%.2f", servicePrice)}"
            sparePartsAmountText.text = "Rs. ${String.format("%.2f", sparePartsTotal)}"
            totalAmountText.text = "Rs. ${String.format("%.2f", totalAmount)}"
        }
    }

    private fun checkUserType() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    currentUserId = currentUser.uid
                    val result = authRepository.getUserProfile(currentUser.uid)
                    result.onSuccess { userProfile ->
                        currentUserType = userProfile.userType ?: ""
                        if (userProfile.userType == "shop_owner") {
                            // Shopkeeper view
                            binding.createOrderButton.visibility = View.VISIBLE
                            updateStatusButtonsVisibility()
                            binding.leaveReviewBtn.visibility = View.GONE
                        } else {
                            // Customer view
                            binding.createOrderButton.visibility = View.GONE
                            binding.statusButtonsContainer.visibility = View.GONE
                            checkAndShowReviewButton()
                        }
                    }.onFailure {
                        binding.createOrderButton.visibility = View.GONE
                        binding.statusButtonsContainer.visibility = View.GONE
                        binding.leaveReviewBtn.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                println("Error checking user type: ${e.message}")
                binding.createOrderButton.visibility = View.GONE
                binding.statusButtonsContainer.visibility = View.GONE
                binding.leaveReviewBtn.visibility = View.GONE
            }
        }
    }
    
    private fun checkAndShowReviewButton() {
        val status = appointment.status.lowercase()
        if (status.contains("completed") || status.contains("delivered")) {
            lifecycleScope.launch {
                try {
                    val result = reviewRepository.getReviewForItem(appointment.id, currentUserId)
                    if (result.isSuccess) {
                        hasReviewed = result.getOrNull() != null
                        if (!hasReviewed) {
                            binding.leaveReviewBtn.visibility = View.VISIBLE
                        } else {
                            binding.leaveReviewBtn.text = "Review Submitted ✓"
                            binding.leaveReviewBtn.isEnabled = false
                            binding.leaveReviewBtn.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    println("AppointmentDetail: Error checking review status: ${e.message}")
                }
            }
        }
    }
    
    private fun showReviewDialog() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                val userProfile = authRepository.getUserProfile(currentUser?.uid ?: "").getOrNull()
                
                val dialog = ReviewDialog.newInstance(
                    itemId = appointment.id,
                    itemType = "APPOINTMENT",
                    shopId = appointment.shopId ?: "",
                    userId = currentUserId,
                    userName = userProfile?.name ?: "Customer"
                ) {
                    // On review submitted
                    binding.leaveReviewBtn.text = "Review Submitted ✓"
                    binding.leaveReviewBtn.isEnabled = false
                    hasReviewed = true
                }
                dialog.show(supportFragmentManager, "ReviewDialog")
            } catch (e: Exception) {
                Toast.makeText(this@AppointmentDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatusButtonsVisibility() {
        // Show status buttons based on current status
        val status = appointment.status.lowercase()
        binding.statusButtonsContainer.visibility = View.VISIBLE
        
        when {
            status.contains("pending") || status.isBlank() -> {
                // Show only confirm button
                binding.confirmButton.visibility = View.VISIBLE
                binding.markDeliveredButton.visibility = View.GONE
            }
            status.contains("confirmed") || status.contains("in progress") -> {
                // Show only mark as delivered button
                binding.confirmButton.visibility = View.GONE
                binding.markDeliveredButton.visibility = View.VISIBLE
            }
            status.contains("completed") || status.contains("delivered") -> {
                // Hide both buttons
                binding.statusButtonsContainer.visibility = View.GONE
            }
            else -> {
                // Show both by default
                binding.confirmButton.visibility = View.VISIBLE
                binding.markDeliveredButton.visibility = View.VISIBLE
            }
        }
    }

    private fun updateAppointmentStatus(newStatus: String) {
        lifecycleScope.launch {
            try {
                appointment.status = newStatus
                // Use appointment.id if available, otherwise use appointmentId
                val id = appointment.id.ifEmpty { appointment.appointmentId }
                val result = appointmentRepository.updateAppointmentStatus(id, newStatus)
                
                result.onSuccess {
                    Toast.makeText(
                        this@AppointmentDetailActivity,
                        "Status updated to $newStatus",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Update UI
                    setupUI()
                    updateStatusButtonsVisibility()
                }.onFailure { exception ->
                    Toast.makeText(
                        this@AppointmentDetailActivity,
                        "Failed to update status: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                println("Error updating appointment status: ${e.message}")
                Toast.makeText(
                    this@AppointmentDetailActivity,
                    "Error updating status",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun openAddPartsActivity() {
        val intent = Intent(this, AddAppointmentPartsActivity::class.java)
        intent.putExtra("appointment", Gson().toJson(appointment))
        startActivityForResult(intent, REQUEST_ADD_PARTS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_PARTS && resultCode == RESULT_OK) {
            // Refresh the spare parts list
            loadSpareParts()
            Toast.makeText(this, "Spare parts added successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

