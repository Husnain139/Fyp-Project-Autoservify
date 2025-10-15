package com.hstan.autoservify.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ActivityOrderDetailBinding
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.ui.main.ViewModels.Order
import kotlinx.coroutines.launch

class OrderDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailBinding
    private lateinit var orderRepository: OrderRepository
    private lateinit var authRepository: AuthRepository
    private var currentOrder: Order? = null
    private var relatedOrders: List<Order> = emptyList() // For multi-part manual orders
    private var isMultiPartOrder: Boolean = false
    private var currentUserType: String = ""
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize repositories
        orderRepository = OrderRepository()
        authRepository = AuthRepository()

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Order Details"

        // Check if this is a multi-part order or single order
        val relatedOrderIds = intent.getStringArrayListExtra("related_order_ids")
        if (relatedOrderIds != null && relatedOrderIds.isNotEmpty()) {
            // Multi-part manual order
            isMultiPartOrder = true
            loadMultiPartOrder(relatedOrderIds)
        } else {
            // Single order (existing logic)
            val orderJson = intent.getStringExtra("order_data")
            if (orderJson != null) {
                currentOrder = Gson().fromJson(orderJson, Order::class.java)
                getCurrentUserInfo()
            } else {
                Toast.makeText(this, "Error loading order details", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Setup click listeners
        setupClickListeners()
    }

    private fun loadMultiPartOrder(orderIds: List<String>) {
        lifecycleScope.launch {
            try {
                val orders = mutableListOf<Order>()
                for (orderId in orderIds) {
                    val result = orderRepository.getOrderById(orderId)
                    if (result.isSuccess) {
                        orders.add(result.getOrThrow())
                    }
                }
                
                if (orders.isNotEmpty()) {
                    relatedOrders = orders
                    currentOrder = orders.first() // Use first order for customer info
                    getCurrentUserInfo()
                } else {
                    Toast.makeText(this@OrderDetailActivity, "Error loading order details", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@OrderDetailActivity, "Error loading orders: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getCurrentUserInfo() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        currentUserType = userProfile.userType ?: ""
                        currentUserId = currentUser.uid
                        
                        println("OrderDetail: Current user type: '$currentUserType'")
                        println("OrderDetail: Current user ID: '$currentUserId'")
                        println("OrderDetail: Order status: '${currentOrder?.status}'")
                        
                        // Load order details and setup UI
                        setupOrderDetails()
                        setupStatusButtons()
                        startRealTimeUpdates()
                    }
                }
            } catch (e: Exception) {
                println("OrderDetail: Error loading user info: ${e.message}")
                Toast.makeText(this@OrderDetailActivity, "Error loading user info", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupOrderDetails() {
        currentOrder?.let { order ->
            if (isMultiPartOrder && relatedOrders.isNotEmpty()) {
                // Display multi-part order
                displayMultiPartOrder(relatedOrders)
            } else {
                // Display single order
                displaySingleOrder(order)
            }
            
            // Customer details (same for both single and multi-part)
            binding.customerName.text = order.userName.ifBlank { "Unknown Customer" }
            binding.customerEmail.text = order.userEmail.ifBlank { "No email provided" }
            binding.customerContact.text = order.userContact.ifBlank { "No contact provided" }
            
            // Delivery details
            binding.deliveryAddress.text = order.postalAddress.ifBlank { "No address provided" }
            binding.specialRequirements.text = if (order.specialRequirements.isBlank()) {
                "No special requirements"
            } else {
                order.specialRequirements
            }
            
            // Order info
            binding.orderDate.text = order.orderDate.ifBlank { "No date" }
            binding.orderStatus.text = order.status.ifBlank { "pending" }.replaceFirstChar { it.uppercase() }
            
            // Set status color
            updateStatusColor(order.status)
        }
    }

    private fun displaySingleOrder(order: Order) {
        // Show single item section
        binding.singleItemSection.visibility = View.VISIBLE
        binding.multiPartSection.visibility = View.GONE
        
        // Order item details
        binding.itemTitle.text = order.item?.title ?: "Unknown Item"
        binding.itemDescription.text = order.item?.description ?: "No description available"
        binding.itemPrice.text = "₹${order.item?.price ?: 0}"
        binding.orderQuantity.text = "Quantity: ${order.quantity}"
        binding.totalPrice.text = "Total: ₹${(order.item?.price ?: 0) * order.quantity}"
        
        // Load item image
        Glide.with(this)
            .load(order.item?.image)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(binding.itemImage)
    }

    private fun displayMultiPartOrder(orders: List<Order>) {
        // Hide single item views, show multi-part section
        binding.singleItemSection.visibility = View.GONE
        binding.multiPartSection.visibility = View.VISIBLE
        
        // Clear previous parts
        binding.partsListContainer.removeAllViews()
        
        // Display each part
        for (order in orders) {
            val partView = layoutInflater.inflate(android.R.layout.simple_list_item_2, binding.partsListContainer, false)
            val text1 = partView.findViewById<android.widget.TextView>(android.R.id.text1)
            val text2 = partView.findViewById<android.widget.TextView>(android.R.id.text2)
            
            text1.text = "${order.item?.title ?: "Unknown"} x ${order.quantity}"
            text2.text = "₹${(order.item?.price ?: 0) * order.quantity}"
            text1.textSize = 16f
            text2.textSize = 14f
            text1.setTextColor(getColor(R.color.text_primary))
            text2.setTextColor(getColor(R.color.text_secondary))
            
            binding.partsListContainer.addView(partView)
        }
        
        // Calculate and display total
        val totalPrice = orders.sumOf { (it.item?.price ?: 0) * it.quantity }
        binding.multiPartTotal.text = "Total: ₹$totalPrice"
    }

    private fun updateStatusColor(status: String) {
        val statusColor = when (status.lowercase().trim()) {
            "pending", "order placed", "placed" -> getColor(R.color.orange)
            "confirmed", "order confirmed" -> getColor(R.color.blue)
            "delivered", "order delivered" -> getColor(R.color.green)
            "received", "order received" -> getColor(R.color.success_green)
            "cancelled", "order cancelled" -> getColor(R.color.red)
            else -> getColor(R.color.gray)
        }
        binding.orderStatus.setTextColor(statusColor)
    }

    private fun setupStatusButtons() {
        currentOrder?.let { order ->
            val status = order.status.lowercase().trim()
            
            println("OrderDetail: Setting up buttons for user type: '$currentUserType', status: '$status'")
            println("OrderDetail: Raw status from order: '${order.status}'")
            
            when (currentUserType) {
                "shop_owner" -> {
                    println("OrderDetail: Setting up shopkeeper buttons")
                    setupShopkeeperButtons(status)
                }
                "customer" -> {
                    println("OrderDetail: Setting up customer buttons")
                    setupCustomerButtons(status)
                }
                else -> {
                    println("OrderDetail: Unknown user type, hiding all buttons")
                    hideAllButtons()
                }
            }
        } ?: run {
            println("OrderDetail: Current order is null, hiding all buttons")
            hideAllButtons()
        }
    }

    private fun setupShopkeeperButtons(status: String) {
        println("OrderDetail: setupShopkeeperButtons called with status: '$status'")
        when (status) {
            "pending", "", "order placed", "placed" -> {
                // Show "Confirm Order" button for new orders
                println("OrderDetail: Showing Confirm Order button")
                binding.confirmOrderBtn.visibility = View.VISIBLE
                binding.markDeliveredBtn.visibility = View.GONE
                binding.markReceivedBtn.visibility = View.GONE
            }
            "confirmed", "order confirmed" -> {
                // Show "Mark as Delivered" button for confirmed orders
                println("OrderDetail: Showing Mark as Delivered button")
                binding.confirmOrderBtn.visibility = View.GONE
                binding.markDeliveredBtn.visibility = View.VISIBLE
                binding.markReceivedBtn.visibility = View.GONE
            }
            "delivered", "order delivered", "received", "order received" -> {
                // Order is completed, hide all buttons
                println("OrderDetail: Order completed, hiding all buttons")
                hideAllButtons()
            }
            else -> {
                println("OrderDetail: Unknown status '$status', showing confirm button as default")
                // Default to showing confirm button for unknown statuses
                binding.confirmOrderBtn.visibility = View.VISIBLE
                binding.markDeliveredBtn.visibility = View.GONE
                binding.markReceivedBtn.visibility = View.GONE
            }
        }
    }

    private fun setupCustomerButtons(status: String) {
        println("OrderDetail: setupCustomerButtons called with status: '$status'")
        when (status) {
            "delivered", "order delivered" -> {
                // Show "Mark as Received" button for delivered orders
                println("OrderDetail: Showing Order Received button")
                binding.confirmOrderBtn.visibility = View.GONE
                binding.markDeliveredBtn.visibility = View.GONE
                binding.markReceivedBtn.visibility = View.VISIBLE
            }
            else -> {
                // Customer can't take action until order is delivered
                println("OrderDetail: Customer cannot take action with status '$status', hiding all buttons")
                hideAllButtons()
            }
        }
    }

    private fun hideAllButtons() {
        println("OrderDetail: hideAllButtons called")
        binding.confirmOrderBtn.visibility = View.GONE
        binding.markDeliveredBtn.visibility = View.GONE
        binding.markReceivedBtn.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.confirmOrderBtn.setOnClickListener {
            updateOrderStatus("Order Confirmed")
        }

        binding.markDeliveredBtn.setOnClickListener {
            updateOrderStatus("Order Delivered")
        }

        binding.markReceivedBtn.setOnClickListener {
            updateOrderStatus("Order Received")
        }
    }

    private fun updateOrderStatus(newStatus: String) {
        // Show loading state
        setButtonsEnabled(false)
        
        lifecycleScope.launch {
            try {
                if (isMultiPartOrder && relatedOrders.isNotEmpty()) {
                    // Update all related orders
                    var allSuccess = true
                    for (order in relatedOrders) {
                        order.status = newStatus
                        val result = orderRepository.updateOrder(order)
                        if (result.isFailure) {
                            allSuccess = false
                            break
                        }
                    }
                    
                    if (allSuccess) {
                        // Update current order reference
                        currentOrder?.status = newStatus
                        
                        Toast.makeText(
                            this@OrderDetailActivity,
                            "Order status updated to ${newStatus.replaceFirstChar { it.uppercase() }}",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Update UI immediately
                        binding.orderStatus.text = newStatus.replaceFirstChar { it.uppercase() }
                        updateStatusColor(newStatus)
                        setupStatusButtons()
                    } else {
                        Toast.makeText(
                            this@OrderDetailActivity,
                            "Failed to update order status",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Update single order
                    currentOrder?.let { order ->
                        order.status = newStatus
                        val result = orderRepository.updateOrder(order)
                        
                        if (result.isSuccess) {
                            Toast.makeText(
                                this@OrderDetailActivity,
                                "Order status updated to ${newStatus.replaceFirstChar { it.uppercase() }}",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Update UI immediately
                            binding.orderStatus.text = newStatus.replaceFirstChar { it.uppercase() }
                            updateStatusColor(newStatus)
                            setupStatusButtons()
                        } else {
                            Toast.makeText(
                                this@OrderDetailActivity,
                                "Failed to update order status",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Revert status change
                            order.status = binding.orderStatus.text.toString().lowercase()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@OrderDetailActivity,
                    "Error updating order: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setButtonsEnabled(true)
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.confirmOrderBtn.isEnabled = enabled
        binding.markDeliveredBtn.isEnabled = enabled
        binding.markReceivedBtn.isEnabled = enabled
        
        // Show loading indicator
        binding.loadingIndicator.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun startRealTimeUpdates() {
        currentOrder?.let { order ->
            lifecycleScope.launch {
                // Listen for real-time updates to this specific order
                orderRepository.getOrders().collect { orders ->
                    val updatedOrder = orders.find { it.id == order.id }
                    if (updatedOrder != null && updatedOrder.status != currentOrder?.status) {
                        // Order status changed, update UI
                        currentOrder = updatedOrder
                        runOnUiThread {
                            binding.orderStatus.text = updatedOrder.status.replaceFirstChar { it.uppercase() }
                            updateStatusColor(updatedOrder.status)
                            setupStatusButtons()
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
