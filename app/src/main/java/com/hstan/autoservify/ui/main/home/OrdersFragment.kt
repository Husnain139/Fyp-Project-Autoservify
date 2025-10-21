package com.hstan.autoservify.ui.main.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.databinding.FragmentOrdersBinding
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment
import com.hstan.autoservify.ui.orders.OrderAdapter
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.AppUser
import kotlinx.coroutines.launch

class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: OrderFragmentViewModel
    private lateinit var adapter: OrderAdapter

    // one list to display both orders + appointments
    private val items = ArrayList<Any>()  // Any = Order OR Appointment
    
    // Flag to prevent multiple simultaneous data loads
    private var isDataLoaded = false

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
            println("OrdersFragment: onViewCreated called")
            // Initialize ViewModel first - ensure we use the fragment scope
            if (!::viewModel.isInitialized) {
                viewModel = ViewModelProvider(this)[OrderFragmentViewModel::class.java]
                println("OrdersFragment: ViewModel initialized")
            } else {
                println("OrdersFragment: ViewModel already initialized")
            }

            // init adapter (you will need to make OrderAdapter handle both Order & Appointment types)
            if (!::adapter.isInitialized) {
                adapter = OrderAdapter(
                    emptyList(),
                    onViewClick = { /* navigate to detail screen if needed */ },
                    onCancelClick = { item ->
                        try {
                            when (item) {
                                is Order -> viewModel.cancelOrder(item)
                                is Appointment -> viewModel.cancelAppointment(item)
                            }
                        } catch (e: Exception) {
                            println("Error cancelling item: ${e.message}")
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Failed to cancel item", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDeleteClick = { item ->
                        try {
                            when (item) {
                                is Order -> viewModel.deleteOrder(item)
                                is Appointment -> {
                                    // Appointments don't support delete, only cancel
                                    Toast.makeText(context, "Use cancel for appointments", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            println("Error deleting item: ${e.message}")
                            if (isAdded && context != null) {
                                Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                println("OrdersFragment: Adapter initialized")
            } else {
                println("OrdersFragment: Adapter already initialized")
            }

            // Ensure RecyclerView is properly set up
            if (binding.recyclerview.layoutManager == null) {
                binding.recyclerview.layoutManager = LinearLayoutManager(context)
                println("OrdersFragment: LayoutManager set")
            }
            
            if (binding.recyclerview.adapter == null) {
                binding.recyclerview.adapter = adapter
                println("OrdersFragment: Adapter set to RecyclerView")
            }
        } catch (e: Exception) {
            println("Error in onViewCreated: ${e.message}")
            e.printStackTrace()
            Toast.makeText(context, "Error initializing orders", Toast.LENGTH_SHORT).show()
            return
        }

        // Use viewLifecycleOwner for proper lifecycle management
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.failureMessage.collect { msg ->
                    msg?.let { 
                        if (isAdded && context != null) {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show() 
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error collecting failure messages: ${e.message}")
            }
        }

        // observe orders
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.orders.collect { list ->
                    if (isAdded) {
                        refreshData(list, viewModel.appointments.value)
                    }
                }
            } catch (e: Exception) {
                println("Error collecting orders: ${e.message}")
            }
        }

        // observe appointments
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.appointments.collect { list ->
                    if (isAdded) {
                        refreshData(viewModel.orders.value, list)
                    }
                }
            } catch (e: Exception) {
                println("Error collecting appointments: ${e.message}")
            }
        }

        // Load orders based on user role (only once per view creation)
        println("OrdersFragment: isDataLoaded = $isDataLoaded")
        if (!isDataLoaded) {
            try {
                println("OrdersFragment: Loading orders for user")
                loadOrdersForUser()
                isDataLoaded = true
            } catch (e: Exception) {
                println("OrdersFragment: Error loading orders for user: ${e.message}")
                e.printStackTrace()
                isDataLoaded = false // Reset flag on error
                if (isAdded && context != null) {
                    Toast.makeText(context, "Failed to load orders", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            println("OrdersFragment: Data already loaded, skipping")
        }
    }

    private fun refreshData(orderList: List<Order>?, appointmentList: List<Appointment>?) {
        try {
            println("OrdersFragment: refreshData called - Orders: ${orderList?.size}, Appointments: ${appointmentList?.size}")
            
            // Safety check for fragment lifecycle
            if (!isAdded || _binding == null) {
                println("OrdersFragment: Fragment not ready for data refresh")
                return
            }
            
            items.clear()
            orderList?.let { 
                items.addAll(it)
                println("OrdersFragment: Orders loaded: ${it.size} items")
            }
            appointmentList?.let { 
                items.addAll(it)
                println("OrdersFragment: Appointments loaded: ${it.size} items")
            }
            println("OrdersFragment: Total items in adapter: ${items.size}")
            
            // Ensure adapter is initialized before updating
            if (::adapter.isInitialized && ::viewModel.isInitialized) {
                adapter.updateData(items)
                println("OrdersFragment: Adapter updated successfully")
            } else {
                println("OrdersFragment: Adapter or ViewModel not initialized yet")
            }
        } catch (e: Exception) {
            println("OrdersFragment: Error refreshing data: ${e.message}")
            e.printStackTrace()
            if (isAdded && context != null) {
                Toast.makeText(context, "Error updating orders", Toast.LENGTH_SHORT).show()
            }
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
                                // Load orders and appointments for this customer
                                println("Loading data for customer: ${userProfile.uid}")
                                viewModel.loadCustomerOrders(userProfile.uid)
                                viewModel.readAppointments() // Load all appointments for now
                            }
                            "shop_owner" -> {
                                // Load orders AND appointments for this shop owner's shop
                                val shopId = userProfile.shopId
                                if (!shopId.isNullOrEmpty()) {
                                    println("Loading data for shop: $shopId")
                                    viewModel.loadShopOrders(shopId)
                                    viewModel.loadShopAppointments(shopId) // 🆕 Use shop-specific method
                                } else {
                                    // Shop owner but no shopId, load all orders/appointments
                                    println("Shop owner with no shopId, loading all data")
                                    viewModel.readOrders()
                                    viewModel.readAppointments()
                                }
                            }
                            else -> {
                                // Unknown user type, load all
                                println("Unknown user type: ${userProfile.userType}, loading all data")
                                viewModel.readOrders()
                                viewModel.readAppointments()
                            }
                        }
                    } else {
                        // If profile not found, try to load as customer
                        println("Profile not found, loading as customer: ${currentUser.uid}")
                        viewModel.loadCustomerOrders(currentUser.uid)
                        viewModel.readAppointments() // Load all appointments for now
                    }
                } else {
                    // No user logged in, load all data
                    println("No user logged in, loading all data")
                    viewModel.readOrders()
                    viewModel.readAppointments()
                }
            } catch (e: Exception) {
                println("Error in loadOrdersForUser: ${e.message}")
                e.printStackTrace()
                // Fallback to loading all data
                try {
                    viewModel.readOrders()
                    viewModel.readAppointments()
                } catch (fallbackError: Exception) {
                    println("Fallback also failed: ${fallbackError.message}")
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Failed to load orders", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        println("OrdersFragment: onDestroyView called")
        _binding = null
        // Reset flag when view is destroyed
        isDataLoaded = false
        println("OrdersFragment: Data loaded flag reset")
    }

    override fun onResume() {
        super.onResume()
        println("OrdersFragment: onResume called")
    }

    override fun onPause() {
        super.onPause()
        println("OrdersFragment: onPause called")
    }
}
