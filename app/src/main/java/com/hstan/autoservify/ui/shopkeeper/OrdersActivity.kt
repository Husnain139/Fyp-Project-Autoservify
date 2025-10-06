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
import com.hstan.autoservify.ui.orders.OrderAdapter
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class OrdersActivity : AppCompatActivity() {

    private lateinit var orderAdapter: OrderAdapter
    private val orders = mutableListOf<Order>()
    private val orderRepository = OrderRepository()
    private val authRepository = AuthRepository()
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)
        
        // Set title
        title = "My Orders"
        
        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadShopkeeperOrders()
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.ordersRecyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        
        orderAdapter = OrderAdapter(
            items = orders as List<Any>,
            onViewClick = { item ->
                if (item is Order) {
                    Toast.makeText(this, "Order: ${item.id}", Toast.LENGTH_SHORT).show()
                }
            },
            onCancelClick = { item ->
                if (item is Order) {
                    Toast.makeText(this, "Order cancellation feature to be implemented", Toast.LENGTH_SHORT).show()
                }
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrdersActivity)
            adapter = orderAdapter
        }
        
        updateEmptyState()
    }

    private fun loadShopkeeperOrders() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        val shopId = userProfile.shopId
                        
                        if (!shopId.isNullOrEmpty()) {
                            println("OrdersActivity: Loading orders for shop: $shopId")
                            loadOrdersByShopId(shopId)
                        } else {
                            println("OrdersActivity: No shop ID found for user")
                            Toast.makeText(this@OrdersActivity, "No shop found for your account", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this@OrdersActivity, "Error loading orders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadOrdersByShopId(shopId: String) {
        lifecycleScope.launch {
            try {
                orderRepository.getShopOrders(shopId).collect { shopOrders ->
                    orders.clear()
                    orders.addAll(shopOrders)
                    orderAdapter.updateData(shopOrders as List<Any>)
                    updateEmptyState()
                    println("OrdersActivity: Loaded ${shopOrders.size} orders")
                }
            } catch (e: Exception) {
                println("OrdersActivity: Error collecting orders: ${e.message}")
                Toast.makeText(this@OrdersActivity, "Error loading orders", Toast.LENGTH_SHORT).show()
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
