package com.hstan.autoservify.ui.main.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.hstan.autoservify.databinding.FragmentOrdersBinding
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.ui.orders.OrderAdapter
import com.hstan.autoservify.ui.orders.ManualOrderServiceActivity
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.AppUser
import com.hstan.autoservify.R
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: OrderFragmentViewModel
    private lateinit var adapter: OrderAdapter

    private val allOrders = ArrayList<Order>()
    private val allAppointments = ArrayList<Appointment>()
    
    private var isDataLoaded = false
    private var currentTab = 0 // 0 = Orders, 1 = Appointments
    private var currentOrderStatusFilter = "Not Confirmed"
    private var currentAppointmentStatusFilter = "Not Confirmed"
    private var isShopkeeper = false

    // Views
    private lateinit var tabLayout: TabLayout
    private lateinit var orderFilterChipsContainer: View
    private lateinit var appointmentFilterChipsContainer: View
    private lateinit var chipOrderNotConfirmed: Chip
    private lateinit var chipOrderInProcess: Chip
    private lateinit var chipOrderCompleted: Chip
    private lateinit var chipAppointmentNotConfirmed: Chip
    private lateinit var chipAppointmentInProcess: Chip
    private lateinit var chipAppointmentCompleted: Chip
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateIcon: ImageView
    private lateinit var emptyStateTitle: TextView
    private lateinit var emptyStateMessage: TextView
    private lateinit var fab: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Initialize ViewModel
            if (!::viewModel.isInitialized) {
                viewModel = ViewModelProvider(this)[OrderFragmentViewModel::class.java]
            }

            // Initialize views
            initViews()

            // Check user type
            checkUserType()

            // Setup tabs
            setupTabs()

            // Setup filter chips
            setupFilterChips()

            // Setup RecyclerView
            setupRecyclerView()

            // Setup FAB
            setupFAB()
        } catch (e: Exception) {
            println("Error in onViewCreated: ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Error initializing orders", Toast.LENGTH_SHORT).show()
            return
        }

        // Observe orders
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.orders.collect { list ->
                    if (isAdded && list != null) {
                        allOrders.clear()
                        allOrders.addAll(list)
                        if (currentTab == 0) {
                            filterAndDisplayData()
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error collecting orders: ${e.message}")
            }
        }

        // Observe appointments
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.appointments.collect { list ->
                    if (isAdded && list != null) {
                        allAppointments.clear()
                        allAppointments.addAll(list)
                        if (currentTab == 1) {
                            filterAndDisplayData()
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error collecting appointments: ${e.message}")
            }
        }

        // Load orders based on user role
        if (!isDataLoaded) {
            loadOrdersForUser()
            isDataLoaded = true
        }
    }

    private fun initViews() {
        tabLayout = binding.tabLayout
        orderFilterChipsContainer = binding.orderFilterChipsContainer
        appointmentFilterChipsContainer = binding.appointmentFilterChipsContainer
        chipOrderNotConfirmed = binding.chipOrderNotConfirmed
        chipOrderInProcess = binding.chipOrderInProcess
        chipOrderCompleted = binding.chipOrderCompleted
        chipAppointmentNotConfirmed = binding.chipAppointmentNotConfirmed
        chipAppointmentInProcess = binding.chipAppointmentInProcess
        chipAppointmentCompleted = binding.chipAppointmentCompleted
        emptyStateLayout = binding.emptyStateLayout
        emptyStateIcon = binding.emptyStateIcon
        emptyStateTitle = binding.emptyStateTitle
        emptyStateMessage = binding.emptyStateMessage
        fab = binding.fabCreateManual
    }

    private fun checkUserType() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val authRepository = AuthRepository()
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        isShopkeeper = userProfile.userType == "shop_owner"
                        updateShopkeeperUI()
                    }
                }
            } catch (e: Exception) {
                println("Error checking user type: ${e.message}")
            }
        }
    }

    private fun updateShopkeeperUI() {
        if (isShopkeeper) {
            fab.visibility = View.VISIBLE
            // Show appropriate filter chips based on current tab
            updateFilterChipsVisibility()
        } else {
            fab.visibility = View.GONE
            orderFilterChipsContainer.visibility = View.GONE
            appointmentFilterChipsContainer.visibility = View.GONE
        }
    }

    private fun updateFilterChipsVisibility() {
        when (currentTab) {
            0 -> { // Orders tab
                orderFilterChipsContainer.visibility = if (isShopkeeper) View.VISIBLE else View.GONE
                appointmentFilterChipsContainer.visibility = View.GONE
            }
            1 -> { // Appointments tab
                orderFilterChipsContainer.visibility = View.GONE
                appointmentFilterChipsContainer.visibility = if (isShopkeeper) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Orders"))
        tabLayout.addTab(tabLayout.newTab().setText("Appointments"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateUIForTab()
                filterAndDisplayData()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupFilterChips() {
        // Order filter chips
        chipOrderNotConfirmed.setOnClickListener {
            currentOrderStatusFilter = "Not Confirmed"
            filterAndDisplayData()
        }

        chipOrderInProcess.setOnClickListener {
            currentOrderStatusFilter = "In Process"
            filterAndDisplayData()
        }

        chipOrderCompleted.setOnClickListener {
            currentOrderStatusFilter = "Completed"
            filterAndDisplayData()
        }

        // Appointment filter chips
        chipAppointmentNotConfirmed.setOnClickListener {
            currentAppointmentStatusFilter = "Not Confirmed"
            filterAndDisplayData()
        }

        chipAppointmentInProcess.setOnClickListener {
            currentAppointmentStatusFilter = "In Process"
            filterAndDisplayData()
        }

        chipAppointmentCompleted.setOnClickListener {
            currentAppointmentStatusFilter = "Completed"
            filterAndDisplayData()
        }
    }

    private fun setupRecyclerView() {
        adapter = OrderAdapter(
            emptyList(),
            onViewClick = { /* handled by adapter */ },
            onCancelClick = { item ->
                when (item) {
                    is Order -> viewModel.cancelOrder(item)
                    is Appointment -> viewModel.cancelAppointment(item)
                }
            },
            onDeleteClick = { item ->
                if (isShopkeeper) {
                    when (item) {
                        is Order -> viewModel.deleteOrder(item)
                        is Appointment -> {
                            Toast.makeText(context, "Use cancel for appointments", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
        binding.recyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@OrdersFragment.adapter
        }
    }

    private fun setupFAB() {
        fab.setOnClickListener {
            val intent = Intent(requireContext(), ManualOrderServiceActivity::class.java)
            intent.putExtra("ENTRY_MODE", if (currentTab == 0) "ORDER" else "SERVICE")
            startActivity(intent)
        }
    }

    private fun updateUIForTab() {
        // Show/hide appropriate filter chips based on tab
        updateFilterChipsVisibility()
        updateEmptyStateMessages()
    }

    private fun updateEmptyStateMessages() {
        when (currentTab) {
            0 -> {
                emptyStateIcon.setImageResource(R.drawable.baseline_shopping_cart_24)
                emptyStateTitle.text = "No Orders"
                emptyStateMessage.text = "Orders will appear here"
            }
            1 -> {
                emptyStateIcon.setImageResource(R.drawable.baseline_event_24)
                emptyStateTitle.text = "No Appointments"
                emptyStateMessage.text = "Appointments will appear here"
            }
        }
    }

    private fun filterAndDisplayData() {
        val filteredData: List<Any> = when (currentTab) {
            0 -> if (isShopkeeper) filterOrders() else allOrders.toList()
            1 -> if (isShopkeeper) filterAppointments() else allAppointments.toList()
            else -> emptyList()
        }

        adapter.updateData(filteredData)
        updateEmptyState(filteredData.isEmpty())
    }

    private fun filterOrders(): List<Order> {
        return when (currentOrderStatusFilter) {
            "Not Confirmed" -> allOrders.filter {
                it.status.contains("pending", ignoreCase = true) ||
                it.status.contains("order placed", ignoreCase = true)
            }
            "In Process" -> allOrders.filter {
                it.status.contains("processing", ignoreCase = true) ||
                it.status.contains("shipped", ignoreCase = true) ||
                it.status.contains("confirmed", ignoreCase = true)
            }
            "Completed" -> allOrders.filter {
                it.status.contains("delivered", ignoreCase = true) ||
                it.status.contains("completed", ignoreCase = true)
            }
            else -> allOrders
        }
    }

    private fun filterAppointments(): List<Appointment> {
        return when (currentAppointmentStatusFilter) {
            "Not Confirmed" -> allAppointments.filter {
                it.status.contains("pending", ignoreCase = true) ||
                it.status.isBlank()
            }
            "In Process" -> allAppointments.filter {
                it.status.contains("confirmed", ignoreCase = true) ||
                it.status.contains("in progress", ignoreCase = true)
            }
            "Completed" -> allAppointments.filter {
                it.status.contains("delivered", ignoreCase = true) ||
                it.status.contains("completed", ignoreCase = true)
            }
            else -> allAppointments
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerview.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerview.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    private fun loadOrdersForUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val authRepository = AuthRepository()
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        when (userProfile.userType) {
                            "customer" -> {
                                viewModel.loadCustomerOrders(userProfile.uid)
                                viewModel.readAppointments()
                            }
                            "shop_owner" -> {
                                val shopId = userProfile.shopId
                                if (!shopId.isNullOrEmpty()) {
                                    viewModel.loadShopOrders(shopId)
                                    viewModel.loadShopAppointments(shopId)
                                } else {
                                    viewModel.readOrders()
                                    viewModel.readAppointments()
                                }
                            }
                            else -> {
                                viewModel.readOrders()
                                viewModel.readAppointments()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error loading orders: ${e.message}")
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        isDataLoaded = false
    }
}
