package com.hstan.autoservify.ui.orders

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ActivityOrderDetailBinding
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.OrderRepository
import com.hstan.autoservify.model.repositories.ReviewRepository
import com.hstan.autoservify.ui.main.Shops.SpareParts.Partscraftdetail
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.reviews.ReviewDialog
import kotlinx.coroutines.launch

class OrderDetailActivity : AppCompatActivity() {


    private lateinit var binding: ActivityOrderDetailBinding
    private lateinit var orderRepository: OrderRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var reviewRepository: ReviewRepository
    private var currentOrder: Order? = null
    private var relatedOrders: List<Order> = emptyList() // For multi-part orders
    private var isMultiPartOrder: Boolean = false
    private var currentUserType: String = ""
    private var currentUserId: String = ""
    private var hasReviewed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize repositories
        orderRepository = OrderRepository()
        authRepository = AuthRepository()
        reviewRepository = ReviewRepository()

        // Check if multi-part order
        val relatedOrderIds = intent.getStringArrayListExtra("related_order_ids")
        if (relatedOrderIds != null && relatedOrderIds.isNotEmpty()) {
            isMultiPartOrder = true
            loadMultiPartOrder(relatedOrderIds)
        } else {
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
                displayMultiPartOrder(relatedOrders)
            } else {
                displaySingleOrder(order)
            }

            binding.customerName.text = order.userName.ifBlank { "Unknown Customer" }
            binding.customerEmail.text = order.userEmail.ifBlank { "No email provided" }
            binding.customerContact.text = order.userContact.ifBlank { "No contact provided" }

            binding.deliveryAddress.text = order.postalAddress.ifBlank { "No address provided" }
            binding.specialRequirements.text = if (order.specialRequirements.isBlank()) "No special requirements" else order.specialRequirements

            binding.orderDate.text = order.orderDate.ifBlank { "No date" }
            binding.orderStatus.text = order.status.ifBlank { "pending" }.replaceFirstChar { it.uppercase() }

            updateStatusColor(order.status)
        }
    }

    private fun displaySingleOrder(order: Order) {
        binding.singleItemSection.visibility = View.VISIBLE
        binding.multiPartSection.visibility = View.GONE

        binding.itemTitle.text = order.item?.title ?: "Unknown Item"
        binding.itemDescription.text = order.item?.description ?: "No description available"
        binding.itemPrice.text = "Rs.${order.item?.price ?: 0}"
        binding.orderQuantity.text = "Quantity: ${order.quantity}"
        binding.totalPrice.text = "Total: Rs.${(order.item?.price ?: 0) * order.quantity}"

        Glide.with(this)
            .load(order.item?.image)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(binding.itemImage)
    }


    private fun displayMultiPartOrder(orders: List<Order>) {
        binding.singleItemSection.visibility = View.GONE
        binding.multiPartSection.visibility = View.VISIBLE
        binding.partsListContainer.removeAllViews()

        for (order in orders) {
            val partView = layoutInflater.inflate(android.R.layout.simple_list_item_2, binding.partsListContainer, false)
            val text1 = partView.findViewById<android.widget.TextView>(android.R.id.text1)
            val text2 = partView.findViewById<android.widget.TextView>(android.R.id.text2)

            text1.text = "${order.item?.title ?: "Unknown"} x ${order.quantity}"
            text2.text = "Rs.${(order.item?.price ?: 0) * order.quantity}"
            text1.textSize = 16f
            text2.textSize = 14f
            text1.setTextColor(getColor(R.color.text_primary))
            text2.setTextColor(getColor(R.color.text_secondary))

            binding.partsListContainer.addView(partView)
        }

        val totalPrice = orders.sumOf { (it.item?.price ?: 0) * it.quantity }
        binding.multiPartTotal.text = "Total: Rs.$totalPrice"
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

            when (currentUserType) {
                "shop_owner" -> setupShopkeeperButtons(status)
                "customer" -> setupCustomerButtons(status)
                else -> hideAllButtons()
            }
        } ?: hideAllButtons()
    }

    private fun setupShopkeeperButtons(status: String) {
        when (status) {
            "cancelled", "order cancelled" -> {
                // Order cancelled by customer, hide all action buttons
                hideAllButtons()
            }
            "pending", "", "order placed", "placed" -> {
                binding.confirmOrderBtn.visibility = View.VISIBLE
                binding.markDeliveredBtn.visibility = View.GONE
                binding.markReceivedBtn.visibility = View.GONE
            }
            "confirmed", "order confirmed" -> {
                binding.confirmOrderBtn.visibility = View.GONE
                binding.markDeliveredBtn.visibility = View.VISIBLE
                binding.markReceivedBtn.visibility = View.GONE
            }
            "delivered", "order delivered", "received", "order received" -> hideAllButtons()
            else -> binding.confirmOrderBtn.visibility = View.VISIBLE
        }
    }




    private fun setupCustomerButtons(status: String) {
        // Show Cancel button only for pending orders
        binding.cancelButton.visibility = if (status == "pending" || status == "order placed" || status == "placed") {
            View.VISIBLE
        } else {
            View.GONE
        }

        when (status) {
            "delivered", "order delivered" -> {
                binding.confirmOrderBtn.visibility = View.GONE
                binding.markDeliveredBtn.visibility = View.GONE
                binding.markReceivedBtn.visibility = View.VISIBLE
                checkAndShowReviewButton(false)
            }
            "received", "order received" -> {
                hideAllButtons()
                checkAndShowReviewButton(true)
            }
            else -> {
                binding.confirmOrderBtn.visibility = View.GONE
                binding.markDeliveredBtn.visibility = View.GONE
                binding.markReceivedBtn.visibility = View.GONE
            }
        }
    }

    private fun checkAndShowReviewButton(showNow: Boolean) {
        currentOrder?.let { order ->
            lifecycleScope.launch {
                try {
                    val result = reviewRepository.getReviewForItem(order.id, currentUserId)
                    if (result.isSuccess) {
                        hasReviewed = result.getOrNull() != null
                        if (!hasReviewed && showNow) {
                            binding.leaveReviewBtn.visibility = View.VISIBLE
                        } else if (hasReviewed) {
                            binding.leaveReviewBtn.text = "Review Submitted ✓"
                            binding.leaveReviewBtn.isEnabled = false
                            if (showNow) binding.leaveReviewBtn.visibility = View.VISIBLE
                        }
                    }
                } catch (e: Exception) {
                    println("OrderDetail: Error checking review status: ${e.message}")
                }
            }
        }
    }

    private fun hideAllButtons() {
        binding.confirmOrderBtn.visibility = View.GONE
        binding.markDeliveredBtn.visibility = View.GONE
        binding.markReceivedBtn.visibility = View.GONE
        binding.leaveReviewBtn.visibility = View.GONE
        binding.cancelButton.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.confirmOrderBtn.setOnClickListener {
            updateOrderStatus("Order Confirmed")
        }

        binding.markDeliveredBtn.setOnClickListener {
            updateOrderStatus("Order Delivered")
        }

        binding.markReceivedBtn.setOnClickListener {
            updateOrderStatus("Order Received")
        }

        binding.leaveReviewBtn.setOnClickListener {
            showReviewDialog()
        }

        // --- New Cancel button listener ---
        binding.cancelButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes") { dialog, _ ->
                    updateOrderStatus("Order Cancelled")

                    val intent = Intent(this, Partscraftdetail::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showReviewDialog() {
        currentOrder?.let { order ->
            lifecycleScope.launch {
                try {
                    val currentUser = authRepository.getCurrentUser()
                    val userProfile = authRepository.getUserProfile(currentUser?.uid ?: "").getOrNull()

                    val dialog = ReviewDialog.newInstance(
                        itemId = order.id,
                        itemType = "ORDER",
                        shopId = order.shopId,
                        userId = currentUserId,
                        userName = userProfile?.name ?: "Customer"
                    ) {
                        binding.leaveReviewBtn.text = "Review Submitted ✓"
                        binding.leaveReviewBtn.isEnabled = false
                        hasReviewed = true
                    }
                    dialog.show(supportFragmentManager, "ReviewDialog")
                } catch (e: Exception) {
                    Toast.makeText(this@OrderDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateOrderStatus(newStatus: String) {
        setButtonsEnabled(false)
        lifecycleScope.launch {
            try {
                if (isMultiPartOrder && relatedOrders.isNotEmpty()) {
                    var allSuccess = true
                    for (order in relatedOrders) {
                        order.status = newStatus
                        val result = orderRepository.updateOrder(order)
                        if (result.isFailure) allSuccess = false
                    }
                    if (allSuccess) {
                        currentOrder?.status = newStatus
                        binding.orderStatus.text = newStatus.replaceFirstChar { it.uppercase() }
                        updateStatusColor(newStatus)
                        setupStatusButtons()
                        Toast.makeText(this@OrderDetailActivity, "Order status updated to ${newStatus.replaceFirstChar { it.uppercase() }}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OrderDetailActivity, "Failed to update order status", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    currentOrder?.let { order ->
                        order.status = newStatus
                        val result = orderRepository.updateOrder(order)
                        if (result.isSuccess) {
                            binding.orderStatus.text = newStatus.replaceFirstChar { it.uppercase() }
                            updateStatusColor(newStatus)
                            setupStatusButtons()
                            Toast.makeText(this@OrderDetailActivity, "Order status updated to ${newStatus.replaceFirstChar { it.uppercase() }}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@OrderDetailActivity, "Failed to update order status", Toast.LENGTH_SHORT).show()
                            order.status = binding.orderStatus.text.toString().lowercase()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@OrderDetailActivity, "Error updating order: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setButtonsEnabled(true)
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.confirmOrderBtn.isEnabled = enabled
        binding.markDeliveredBtn.isEnabled = enabled
        binding.markReceivedBtn.isEnabled = enabled
        binding.cancelButton.isEnabled = enabled
        binding.loadingIndicator.visibility = if (enabled) View.GONE else View.VISIBLE
    }

    private fun startRealTimeUpdates() {
        currentOrder?.let { order ->
            lifecycleScope.launch {
                orderRepository.getOrders().collect { orders ->
                    val updatedOrder = orders.find { it.id == order.id }
                    if (updatedOrder != null && updatedOrder.status != currentOrder?.status) {
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
