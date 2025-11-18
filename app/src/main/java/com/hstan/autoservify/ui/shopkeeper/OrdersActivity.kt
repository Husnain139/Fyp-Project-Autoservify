package com.hstan.autoservify.ui.shopkeeper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.orders.OrderAdapter
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.ui.orders.ManualOrderServiceActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class OrdersActivity : AppCompatActivity() {

    private lateinit var orderAdapter: OrderAdapter
    private val orders = mutableListOf<Order>()
    private val orderRepository = OrderRepository()
    private val authRepository = AuthRepository()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private var ordersJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)
        
        // Set title
        title = "My Orders"
        
        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupFAB()
        loadShopkeeperOrders()
    }

    private fun setupFAB() {
        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabCreateManualOrder)
        
        // Hide FAB if this is customer view
        val isCustomerView = intent.getBooleanExtra("IS_CUSTOMER_VIEW", false)
        if (isCustomerView) {
            fab.visibility = android.view.View.GONE
        } else {
            fab.setOnClickListener {
                val intent = Intent(this, com.hstan.autoservify.ui.orders.ManualOrderServiceActivity::class.java)
                intent.putExtra("ENTRY_MODE", "ORDER")
                startActivity(intent)
            }
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.ordersRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        
        orderAdapter = OrderAdapter(
            items = orders as List<Any>,
            onViewClick = { item ->
                if (item is Order) {
                    // View order details
                    Toast.makeText(this, "Order: ${item.id}", Toast.LENGTH_SHORT).show()
                }
            },
            onCancelClick = { item ->
                if (item is Order) {
                    showCancelConfirmationDialog(item)
                }
            },
            onDeleteClick = { item ->
                if (item is Order) {
                    showDeleteConfirmationDialog(item)
                }
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrdersActivity)
            adapter = orderAdapter
        }
        
        updateEmptyState()
    }

    private fun showCancelConfirmationDialog(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Cancel Order")
            .setMessage("Are you sure you want to cancel this order? The order status will be changed to 'Canceled'.")
            .setPositiveButton("Yes") { _, _ ->
                cancelOrder(order)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to permanently delete this order? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteOrder(order)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun cancelOrder(order: Order) {
        lifecycleScope.launch {
            try {
                val result = orderRepository.cancelOrder(order)
                if (result.isSuccess) {
                    Toast.makeText(this@OrdersActivity, "Order canceled successfully", Toast.LENGTH_SHORT).show()
                    // The order list will auto-update via the Flow collector
                } else {
                    Toast.makeText(this@OrdersActivity, "Failed to cancel order", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@OrdersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteOrder(order: Order) {
        lifecycleScope.launch {
            try {
                val result = orderRepository.deleteOrder(order.id)
                if (result.isSuccess) {
                    Toast.makeText(this@OrdersActivity, "Order deleted successfully", Toast.LENGTH_SHORT).show()
                    // Remove from local list immediately for instant UI update
                    orders.remove(order)
                    orderAdapter.updateData(orders as List<Any>)
                    updateEmptyState()
                } else {
                    Toast.makeText(this@OrdersActivity, "Failed to delete order", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@OrdersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadShopkeeperOrders() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        
                        // Check if this is a customer viewing their order history
                        val isCustomerView = intent.getBooleanExtra("IS_CUSTOMER_VIEW", false)
                        
                        if (isCustomerView) {
                            println("OrdersActivity: Loading customer orders for user: ${currentUser.uid}")
                            loadCustomerOrders(currentUser.uid)
                        } else {
                            // Shopkeeper view
                            val shopId = userProfile.shopId
                            
                            if (!shopId.isNullOrEmpty()) {
                                println("OrdersActivity: Loading orders for shop: $shopId")
                                loadOrdersByShopId(shopId)
                            } else {
                                println("OrdersActivity: No shop ID found for user")
                                Toast.makeText(this@OrdersActivity, "No shop found for your account", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        println("OrdersActivity: Failed to get user profile")
                        Toast.makeText(this@OrdersActivity, "Failed to load user profile", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    println("OrdersActivity: No current user")
                    Toast.makeText(this@OrdersActivity, "Please log in again", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                println("OrdersActivity: Error loading orders: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@OrdersActivity, "Error loading orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadOrdersByShopId(shopId: String) {
        // Cancel any existing job to prevent "job was canceled" errors
        ordersJob?.cancel()
        
        ordersJob = lifecycleScope.launch {
            try {
                println("OrdersActivity: Loading orders for shop ID: $shopId")
                orderRepository.getShopOrders(shopId).collect { shopOrders ->
                    orders.clear()
                    orders.addAll(shopOrders)
                    orderAdapter.updateData(shopOrders as List<Any>)
                    updateEmptyState()
                    println("OrdersActivity: Loaded ${shopOrders.size} shop orders")
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    println("OrdersActivity: Order collection was cancelled (this is normal)")
                    return@launch // Don't show error for cancellation
                }
                println("OrdersActivity: Error collecting shop orders: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@OrdersActivity, "Error loading shop orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCustomerOrders(customerId: String) {
        // Cancel any existing job to prevent "job was canceled" errors
        ordersJob?.cancel()
        
        ordersJob = lifecycleScope.launch {
            try {
                println("OrdersActivity: Loading orders for customer ID: $customerId")
                orderRepository.getCustomerOrders(customerId).collect { customerOrders ->
                    orders.clear()
                    orders.addAll(customerOrders)
                    orderAdapter.updateData(customerOrders as List<Any>)
                    updateEmptyState()
                    println("OrdersActivity: Loaded ${customerOrders.size} customer orders for user $customerId")
                    
                    // Debug: Log each order's details
                    customerOrders.forEachIndexed { index, order ->
                        println("OrdersActivity: Order $index - ID: ${order.id}, Item: ${order.item?.title}, UserId: ${order.userId}")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    println("OrdersActivity: Order collection was cancelled (this is normal)")
                    return@launch // Don't show error for cancellation
                }
                println("OrdersActivity: Error collecting customer orders: ${e.message}")
                e.printStackTrace()
                Toast.makeText(this@OrdersActivity, "Error loading your orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState() {
        if (orders.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // No need to reload here - Flow automatically updates when data changes
        // Calling loadShopkeeperOrders() here was causing "job was canceled" errors
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel the job when activity is destroyed
        ordersJob?.cancel()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
