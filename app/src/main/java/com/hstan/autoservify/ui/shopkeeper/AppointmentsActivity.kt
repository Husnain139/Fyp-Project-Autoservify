package com.hstan.autoservify.ui.shopkeeper

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.Adapters.AppointmentAdapter
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.model.repositories.AppointmentRepository
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class AppointmentsActivity : AppCompatActivity() {

    private lateinit var appointmentAdapter: AppointmentAdapter
    private val appointments = mutableListOf<Appointment>()
    private val appointmentRepository = AppointmentRepository()
    private val authRepository = AuthRepository()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)
        
        // Set title
        title = "My Appointments"
        
        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadShopkeeperAppointments()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.appointmentsRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        
        appointmentAdapter = AppointmentAdapter(
            appointments,
            onContactClick = { appointment ->
                // TODO: Implement contact functionality (e.g., open phone/email app)
                Toast.makeText(this, "Contact: ${appointment.userEmail}", Toast.LENGTH_SHORT).show()
            },
            onUpdateStatusClick = { appointment ->
                // TODO: Implement status update functionality
                Toast.makeText(this, "Update status for: ${appointment.serviceName}", Toast.LENGTH_SHORT).show()
            },
            onItemClick = { appointment ->
                openAppointmentDetail(appointment)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AppointmentsActivity)
            adapter = appointmentAdapter
        }
        
        updateEmptyState()
    }

    private fun loadShopkeeperAppointments() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        val shopId = userProfile.shopId
                        
                        if (!shopId.isNullOrEmpty()) {
                            println("AppointmentsActivity: Loading appointments for shop: $shopId")
                            loadAppointmentsByShopId(shopId)
                        } else {
                            println("AppointmentsActivity: No shop ID found for user")
                            Toast.makeText(this@AppointmentsActivity, "No shop found for your account", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        println("AppointmentsActivity: Failed to get user profile")
                        Toast.makeText(this@AppointmentsActivity, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    println("AppointmentsActivity: No current user")
                    Toast.makeText(this@AppointmentsActivity, "Please log in again", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                println("AppointmentsActivity: Error loading appointments: ${e.message}")
                Toast.makeText(this@AppointmentsActivity, "Error loading appointments", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAppointmentsByShopId(shopId: String) {
        lifecycleScope.launch {
            try {
                appointmentRepository.getShopAppointments(shopId).collect { shopAppointments ->
                    appointments.clear()
                    appointments.addAll(shopAppointments)
                    appointmentAdapter.notifyDataSetChanged()
                    updateEmptyState()
                    println("AppointmentsActivity: Loaded ${shopAppointments.size} appointments")
                }
            } catch (e: Exception) {
                println("AppointmentsActivity: Error collecting appointments: ${e.message}")
                Toast.makeText(this@AppointmentsActivity, "Error loading appointments", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState() {
        if (appointments.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun openAppointmentDetail(appointment: com.hstan.autoservify.ui.main.Shops.Services.Appointment) {
        val intent = android.content.Intent(this, com.hstan.autoservify.ui.orders.AppointmentDetailActivity::class.java)
        intent.putExtra(com.hstan.autoservify.ui.orders.AppointmentDetailActivity.EXTRA_APPOINTMENT, com.google.gson.Gson().toJson(appointment))
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
