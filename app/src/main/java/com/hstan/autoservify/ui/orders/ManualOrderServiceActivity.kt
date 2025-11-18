package com.hstan.autoservify.ui.orders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hstan.autoservify.databinding.ActivityManualOrderServiceBinding
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.AppointmentRepository
import com.hstan.autoservify.model.repositories.PartsCraftRepository
import com.hstan.autoservify.model.repositories.ServiceRepository
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import com.hstan.autoservify.ui.main.Shops.Services.Service
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ManualOrderServiceActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY_MODE = "ENTRY_MODE"
        const val MODE_ORDER = "ORDER"
        const val MODE_SERVICE = "SERVICE"
    }

    private lateinit var binding: ActivityManualOrderServiceBinding
    private val authRepository = AuthRepository()
    private val orderRepository = OrderRepository()
    private val appointmentRepository = AppointmentRepository()
    private val partsCraftRepository = PartsCraftRepository()
    private val serviceRepository = ServiceRepository()
    
    private var shopId: String = ""
    private var entryMode: String = MODE_ORDER
    private val availableParts = mutableListOf<PartsCraft>()
    private val availableServices = mutableListOf<Service>()
    private val selectedParts = mutableListOf<Pair<PartsCraft, Int>>() // List of (part, quantity)
    private var selectedService: Service? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManualOrderServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Read entry mode from intent
        entryMode = intent.getStringExtra(EXTRA_ENTRY_MODE) ?: MODE_ORDER

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = when (entryMode) {
            MODE_ORDER -> "Add Manual Order"
            MODE_SERVICE -> "Add Manual Service"
            else -> "Create Manual Entry"
        }

        // Setup UI based on entry mode
        setupUIForMode()

        // Get shopId from current user
        loadShopId()

        // Setup listeners
        setupListeners()
    }

    private fun setupUIForMode() {
        // Hide radio group since mode is pre-determined
        binding.entryTypeRadioGroup.visibility = View.GONE

        when (entryMode) {
            MODE_ORDER -> {
                binding.orderDetailsSection.visibility = View.VISIBLE
                binding.serviceDetailsSection.visibility = View.GONE
            }
            MODE_SERVICE -> {
                binding.orderDetailsSection.visibility = View.GONE
                binding.serviceDetailsSection.visibility = View.VISIBLE
            }
        }
    }

    private fun loadShopId() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        shopId = userProfile.shopId ?: ""
                        
                        if (shopId.isEmpty()) {
                            Toast.makeText(this@ManualOrderServiceActivity, "No shop found for your account", Toast.LENGTH_SHORT).show()
                            finish()
                            return@launch
                        }
                        
                        // Load shop data
                        loadSpareParts()
                        loadServices()
                    } else {
                        Toast.makeText(this@ManualOrderServiceActivity, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@ManualOrderServiceActivity, "Please log in", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManualOrderServiceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadSpareParts() {
        lifecycleScope.launch {
            try {
                partsCraftRepository.getPartsCraftsByShopId(shopId).collect { parts ->
                    availableParts.clear()
                    availableParts.addAll(parts)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManualOrderServiceActivity, "Error loading spare parts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadServices() {
        lifecycleScope.launch {
            try {
                val services = serviceRepository.getServicesByShopId(shopId)
                availableServices.clear()
                availableServices.addAll(services)
                setupServicesSpinner()
            } catch (e: Exception) {
                Toast.makeText(this@ManualOrderServiceActivity, "Error loading services", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupServicesSpinner() {
        if (availableServices.isEmpty()) {
            binding.serviceSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("No services available")
            )
            return
        }

        val serviceNames = availableServices.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.serviceSpinner.adapter = adapter

        binding.serviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedService = availableServices[position]
                updateServicePrice()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedService = null
                updateServicePrice()
            }
        }
    }

    private fun setupListeners() {
        // Add part button
        binding.addPartButton.setOnClickListener {
            showAddPartDialog()
        }

        // Date picker
        binding.appointmentDateInput.setOnClickListener {
            showDatePicker()
        }

        // Time picker
        binding.appointmentTimeInput.setOnClickListener {
            showTimePicker()
        }

        // Save button
        binding.saveButton.setOnClickListener {
            saveEntry()
        }

        // Set default date and time
        binding.appointmentDateInput.setText(getCurrentDate())
        binding.appointmentTimeInput.setText(getCurrentTime())
    }

    private fun showAddPartDialog() {
        if (availableParts.isEmpty()) {
            Toast.makeText(this, "No spare parts available in your shop", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(com.hstan.autoservify.R.layout.dialog_add_spare_part, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val partSpinner = dialogView.findViewById<android.widget.Spinner>(com.hstan.autoservify.R.id.partSpinner)
        val qtyMinus = dialogView.findViewById<android.widget.ImageView>(com.hstan.autoservify.R.id.dialogQtyMinus)
        val qtyValue = dialogView.findViewById<android.widget.TextView>(com.hstan.autoservify.R.id.dialogQtyValue)
        val qtyPlus = dialogView.findViewById<android.widget.ImageView>(com.hstan.autoservify.R.id.dialogQtyPlus)
        val priceText = dialogView.findViewById<android.widget.TextView>(com.hstan.autoservify.R.id.dialogPartPrice)
        val cancelButton = dialogView.findViewById<android.widget.Button>(com.hstan.autoservify.R.id.dialogCancelButton)
        val addButton = dialogView.findViewById<android.widget.Button>(com.hstan.autoservify.R.id.dialogAddButton)

        var dialogQuantity = 1
        var selectedPart: PartsCraft? = null

        // Setup spinner
        val partNames = availableParts.map { it.title }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, partNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        partSpinner.adapter = adapter

        partSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPart = availableParts[position]
                updateDialogPrice(priceText, selectedPart, dialogQuantity)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set initial selection
        if (availableParts.isNotEmpty()) {
            selectedPart = availableParts[0]
            updateDialogPrice(priceText, selectedPart, dialogQuantity)
        }

        // Quantity controls
        qtyMinus.setOnClickListener {
            if (dialogQuantity > 1) {
                dialogQuantity--
                qtyValue.text = dialogQuantity.toString()
                updateDialogPrice(priceText, selectedPart, dialogQuantity)
            }
        }

        qtyPlus.setOnClickListener {
            dialogQuantity++
            qtyValue.text = dialogQuantity.toString()
            updateDialogPrice(priceText, selectedPart, dialogQuantity)
        }

        // Button listeners
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        addButton.setOnClickListener {
            selectedPart?.let { part ->
                selectedParts.add(Pair(part, dialogQuantity))
                updateSelectedPartsDisplay()
                updateOrderPrice()
                dialog.dismiss()
                Toast.makeText(this, "Part added", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun updateDialogPrice(priceText: android.widget.TextView, part: PartsCraft?, quantity: Int) {
        val price = part?.price ?: 0
        val total = price * quantity
        priceText.text = "Price: Rs. $total"
    }

    private fun updateSelectedPartsDisplay() {
        binding.selectedPartsContainer.removeAllViews()

        if (selectedParts.isEmpty()) {
            binding.selectedPartsLabel.visibility = View.GONE
            return
        }

        binding.selectedPartsLabel.visibility = View.VISIBLE

        for ((index, partQtyPair) in selectedParts.withIndex()) {
            val (part, qty) = partQtyPair
            val partView = layoutInflater.inflate(android.R.layout.simple_list_item_2, binding.selectedPartsContainer, false)
            
            val text1 = partView.findViewById<android.widget.TextView>(android.R.id.text1)
            val text2 = partView.findViewById<android.widget.TextView>(android.R.id.text2)
            
            text1.text = "${part.title} x $qty"
            text2.text = "Rs. ${part.price * qty}"

            // Add remove button
            partView.setOnLongClickListener {
                selectedParts.removeAt(index)
                updateSelectedPartsDisplay()
                updateOrderPrice()
                Toast.makeText(this, "Part removed", Toast.LENGTH_SHORT).show()
                true
            }

            binding.selectedPartsContainer.addView(partView)
        }
    }

    private fun updateOrderPrice() {
        val totalPrice = selectedParts.sumOf { (part, qty) -> part.price * qty }
        binding.orderPriceText.text = "Total: Rs. $totalPrice"
    }

    private fun updateServicePrice() {
        val price = selectedService?.price ?: 0.0
        binding.servicePriceText.text = "Service Price: Rs. ${price.toInt()}"
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getCurrentTime(): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(Date())
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                    Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.time
                )
                binding.appointmentDateInput.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }.time
                )
                binding.appointmentTimeInput.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }

    private fun saveEntry() {
        // Validate customer details
        val customerName = binding.customerNameInput.text.toString().trim()
        val customerEmail = binding.customerEmailInput.text.toString().trim()
        val customerPhone = binding.customerPhoneInput.text.toString().trim()
        val customerAddress = binding.customerAddressInput.text.toString().trim()

        if (customerName.isEmpty() || customerEmail.isEmpty() || customerPhone.isEmpty() || customerAddress.isEmpty()) {
            Toast.makeText(this, "Please fill in all customer details", Toast.LENGTH_SHORT).show()
            return
        }

        when (entryMode) {
            MODE_ORDER -> saveOrder(customerName, customerEmail, customerPhone, customerAddress)
            MODE_SERVICE -> saveAppointment(customerName, customerEmail, customerPhone)
        }
    }

    private fun saveOrder(name: String, email: String, phone: String, address: String) {
        if (selectedParts.isEmpty()) {
            Toast.makeText(this, "Please add at least one spare part", Toast.LENGTH_SHORT).show()
            return
        }

        if (availableParts.isEmpty()) {
            Toast.makeText(this, "No spare parts available in your shop", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                val orderDate = dateFormat.format(System.currentTimeMillis())
                
                // Create separate order for each part
                var allSuccess = true
                for ((part, quantity) in selectedParts) {
                    val order = Order().apply {
                        this.item = part
                        this.quantity = quantity
                        this.userName = name
                        this.userEmail = email
                        this.userContact = phone
                        this.postalAddress = address
                        this.shopId = this@ManualOrderServiceActivity.shopId
                        this.userId = "manual_entry"
                        this.status = "Order Placed"
                        this.orderDate = orderDate
                        this.isManualEntry = true
                        this.specialRequirements = "Manual entry by shopkeeper"
                    }
                    
                    val result = orderRepository.saveOrder(order)
                    if (result.isFailure) {
                        allSuccess = false
                        break
                    }
                }

                binding.loadingOverlay.visibility = View.GONE

                if (allSuccess) {
                    Toast.makeText(this@ManualOrderServiceActivity, "Order created successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@ManualOrderServiceActivity, "Failed to create order", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.loadingOverlay.visibility = View.GONE
                Toast.makeText(this@ManualOrderServiceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAppointment(name: String, email: String, phone: String) {
        if (selectedService == null) {
            Toast.makeText(this, "Please select a service", Toast.LENGTH_SHORT).show()
            return
        }

        if (availableServices.isEmpty()) {
            Toast.makeText(this, "No services available in your shop", Toast.LENGTH_SHORT).show()
            return
        }

        val appointmentDate = binding.appointmentDateInput.text.toString()
        val appointmentTime = binding.appointmentTimeInput.text.toString()

        if (appointmentDate.isEmpty() || appointmentTime.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }

        binding.loadingOverlay.visibility = View.VISIBLE

        val appointment = Appointment(
            userId = "manual_entry",
            userName = name,
            userEmail = email,
            userContact = phone,
            serviceId = selectedService?.id ?: "",
            serviceName = selectedService?.name ?: "",
            serviceImageUrl = selectedService?.imageUrl ?: "", // Include service image URL
            shopId = this.shopId,
            status = "Pending",
            appointmentDate = appointmentDate,
            appointmentTime = appointmentTime,
            bill = selectedService?.price?.toInt()?.toString() ?: "0",
            isManualEntry = true
        )

        lifecycleScope.launch {
            try {
                val result = appointmentRepository.saveAppointment(appointment)
                binding.loadingOverlay.visibility = View.GONE

                if (result.isSuccess) {
                    Toast.makeText(this@ManualOrderServiceActivity, "Appointment created successfully!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@ManualOrderServiceActivity, "Failed to create appointment", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.loadingOverlay.visibility = View.GONE
                Toast.makeText(this@ManualOrderServiceActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}


