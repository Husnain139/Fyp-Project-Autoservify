package com.hstan.autoservify.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ActivityAddAppointmentPartsBinding
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.PartsCraftRepository
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import com.hstan.autoservify.ui.main.ViewModels.Order
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AddAppointmentPartsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAppointmentPartsBinding
    private lateinit var appointment: Appointment
    private val partsCraftRepository = PartsCraftRepository()
    private val orderRepository = OrderRepository()
    private val availableParts = mutableListOf<PartsCraft>()
    private val selectedParts = mutableListOf<SelectedPartItem>()
    private lateinit var selectedPartsAdapter: SelectedPartsAdapter
    private var currentQuantity = 1
    private var selectedPart: PartsCraft? = null

    data class SelectedPartItem(
        val part: PartsCraft,
        val quantity: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAppointmentPartsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set title and back button
        title = "Add Spare Parts"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get appointment from intent
        val appointmentJson = intent.getStringExtra("appointment")
        if (appointmentJson == null) {
            Toast.makeText(this, "Error: No appointment data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        appointment = Gson().fromJson(appointmentJson, Appointment::class.java)

        setupUI()
        setupRecyclerView()
        loadSpareParts()
        setupListeners()
    }

    private fun setupUI() {
        binding.serviceNameText.text = appointment.serviceName
        binding.qtyValue.text = currentQuantity.toString()
    }

    private fun setupRecyclerView() {
        selectedPartsAdapter = SelectedPartsAdapter(selectedParts) { position ->
            // Handle remove item
            selectedParts.removeAt(position)
            selectedPartsAdapter.notifyItemRemoved(position)
            updateSelectedPartsVisibility()
        }
        
        binding.selectedPartsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AddAppointmentPartsActivity)
            adapter = selectedPartsAdapter
        }
    }

    private fun loadSpareParts() {
        lifecycleScope.launch {
            try {
                partsCraftRepository.getPartsCraftsByShopId(appointment.shopId).collect { parts ->
                    availableParts.clear()
                    availableParts.addAll(parts)
                    setupSpinner()
                }
            } catch (e: Exception) {
                println("AddAppointmentPartsActivity: Error loading spare parts: ${e.message}")
                // Silently handle error - no toast message
            }
        }
    }

    private fun setupSpinner() {
        if (availableParts.isEmpty()) {
            Toast.makeText(this, "No spare parts available for this shop", Toast.LENGTH_SHORT).show()
            return
        }

        val partNames = availableParts.map { it.title }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, partNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sparePartSpinner.adapter = adapter

        binding.sparePartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPart = availableParts[position]
                updatePrice()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedPart = null
                binding.priceText.text = "Rs. 0"
            }
        }
    }

    private fun setupListeners() {
        binding.qtyMinus.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                binding.qtyValue.text = currentQuantity.toString()
                updatePrice()
            }
        }

        binding.qtyPlus.setOnClickListener {
            currentQuantity++
            binding.qtyValue.text = currentQuantity.toString()
            updatePrice()
        }

        binding.addMoreButton.setOnClickListener {
            addPartToList()
        }

        binding.confirmOrderButton.setOnClickListener {
            confirmOrder()
        }
    }

    private fun updatePrice() {
        val price = selectedPart?.price ?: 0
        val totalPrice = price * currentQuantity
        binding.priceText.text = "Rs. $totalPrice"
    }

    private fun addPartToList() {
        if (selectedPart == null) {
            Toast.makeText(this, "Please select a spare part", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedItem = SelectedPartItem(selectedPart!!, currentQuantity)
        selectedParts.add(selectedItem)
        selectedPartsAdapter.notifyItemInserted(selectedParts.size - 1)
        
        updateSelectedPartsVisibility()
        
        // Reset quantity
        currentQuantity = 1
        binding.qtyValue.text = currentQuantity.toString()
        updatePrice()
        
        Toast.makeText(this, "Part added to list", Toast.LENGTH_SHORT).show()
    }

    private fun updateSelectedPartsVisibility() {
        if (selectedParts.isEmpty()) {
            binding.selectedPartsCard.visibility = View.GONE
        } else {
            binding.selectedPartsCard.visibility = View.VISIBLE
        }
    }

    private fun confirmOrder() {
        if (selectedParts.isEmpty()) {
            Toast.makeText(this, "Please add at least one spare part", Toast.LENGTH_SHORT).show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Create and save an order for each selected part
                val bookingId = appointment.id.ifEmpty { appointment.appointmentId }
                
                for (selectedItem in selectedParts) {
                    val order = Order()
                    order.item = selectedItem.part
                    order.quantity = selectedItem.quantity
                    order.status = "Completed" // Parts already used
                    order.userId = appointment.userId
                    order.userName = appointment.userName
                    order.userEmail = appointment.userEmail
                    order.userContact = appointment.userContact
                    order.shopId = appointment.shopId
                    order.bookingId = bookingId // Link to appointment
                    order.orderDate = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                        .format(System.currentTimeMillis())
                    order.postalAddress = "" // Not needed for appointment parts
                    order.specialRequirements = "Used in service: ${appointment.serviceName}"

                    val result = orderRepository.saveOrder(order)
                    if (result.isFailure) {
                        throw result.exceptionOrNull() ?: Exception("Failed to save order")
                    }
                }

                binding.loadingOverlay.visibility = View.GONE
                Toast.makeText(this@AddAppointmentPartsActivity, "Spare parts added successfully!", Toast.LENGTH_LONG).show()
                
                // Return success to previous activity
                setResult(RESULT_OK)
                finish()

            } catch (e: Exception) {
                binding.loadingOverlay.visibility = View.GONE
                println("Error saving orders: ${e.message}")
                Toast.makeText(this@AddAppointmentPartsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

