package com.hstan.autoservify.ui.main.Cart

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hstan.autoservify.databinding.ActivityBookAppointmentBinding
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.model.repositories.ServiceRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookAppointment_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBookAppointmentBinding
    private val viewModel: BookAppointmentViewModel by viewModels()

    private var serviceId: String = ""
    private var serviceName: String = ""
    private var serviceImageUrl: String = "" // Store service image URL
    private var shopId: String = "" // 🆕 Store shopId from service
    private lateinit var progressDialog: ProgressDialog
    private val calendar = Calendar.getInstance()
    private val serviceRepository = ServiceRepository() // 🆕 For fetching service details

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Get service info from intent
        serviceId = intent.getStringExtra("service_id") ?: ""
        serviceName = intent.getStringExtra("service_name") ?: ""
        
        // 🆕 Fetch service details to get shopId
        fetchServiceDetails()

        // ✅ Setup ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Booking your appointment...")
            setCancelable(false)
        }

        // ✅ Set default date and time
        binding.dateB.editText?.setText(getCurrentDate())
        binding.time.editText?.setText(getCurrentTime())

        // ✅ Date picker
        binding.dateB.editText?.setOnClickListener {
            showDatePicker()
        }

        // ✅ Time picker
        binding.time.editText?.setOnClickListener {
            showTimePicker()
        }

        // ✅ Book button click
        binding.BookAppointmentButton.setOnClickListener {
            val email = binding.bMail.editText?.text.toString()
            val name = binding.name.editText?.text.toString()
            val date = binding.dateB.editText?.text.toString()
            val time = binding.time.editText?.text.toString()

            if (email.isBlank() || name.isBlank() || date.isBlank() || time.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val appointment = Appointment(
                userId = viewModel.getCurrentUser()?.uid ?: "guest",
                userName = name,
                userEmail = email,
                appointmentDate = date,
                appointmentTime = time,
                status = "Pending",
                serviceId = serviceId,
                serviceName = serviceName,
                serviceImageUrl = serviceImageUrl, // Include service image URL
                shopId = shopId // 🆕 Include shopId from service
            )

            viewModel.saveAppointment(appointment)
        }

        // ✅ Observers
        lifecycleScope.launch {
            viewModel.isSaving.collect { saving ->
                if (saving == true) progressDialog.show() else progressDialog.dismiss()
            }
        }

        lifecycleScope.launch {
            viewModel.isSaved.collect { saved ->
                if (saved == true) {
                    Toast.makeText(
                        this@BookAppointment_Activity,
                        "Appointment booked for $serviceName",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.failureMessage.collect { msg ->
                msg?.let {
                    Toast.makeText(
                        this@BookAppointment_Activity,
                        "Error: $it",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Helpers
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getCurrentTime(): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return timeFormat.format(Date())
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(
                    Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.time
                )
                binding.dateB.editText?.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                    }.time
                )
                binding.time.editText?.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // false → 12h format with AM/PM
        )
        timePicker.show()
    }

    // 🆕 Fetch service details to get shopId and imageUrl
    private fun fetchServiceDetails() {
        lifecycleScope.launch {
            try {
                val service = serviceRepository.getServiceById(serviceId)
                if (service != null) {
                    shopId = service.shopId
                    serviceImageUrl = service.imageUrl
                    println("Fetched shopId for service: $shopId")
                } else {
                    println("Service not found for ID: $serviceId")
                    Toast.makeText(this@BookAppointment_Activity, "Service not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                println("Error fetching service details: ${e.message}")
                Toast.makeText(this@BookAppointment_Activity, "Error loading service details", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
