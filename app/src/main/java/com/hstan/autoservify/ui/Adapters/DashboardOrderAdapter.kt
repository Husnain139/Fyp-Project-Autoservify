package com.hstan.autoservify.ui.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ItemOrderBinding
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.orders.OrderDetailActivity

class DashboardOrderAdapter(
    private var items: List<Order>,
    private val onItemClick: (Order) -> Unit = {}
) : RecyclerView.Adapter<DashboardOrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = items[position]
        
        holder.binding.orderItemTitle.text = order.item?.title ?: "Unknown Item"
        holder.binding.orderQty.text = "Qty: ${order.quantity}"
        holder.binding.orderPrice.text = "Total: ₹${(order.item?.price ?: 0) * order.quantity}"
        holder.binding.orderStatus.text = order.status.replaceFirstChar { it.uppercase() }
        holder.binding.orderDate.text = order.orderDate
        
        // Hide edit/cancel buttons - shopkeeper can only view for dashboard
        holder.binding.orderView.visibility = View.VISIBLE
        holder.binding.orderCancel.visibility = View.GONE
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(order)
            
            // Navigate to OrderDetailActivity
            val context = holder.itemView.context
            val intent = Intent(context, OrderDetailActivity::class.java)
            intent.putExtra("order_data", Gson().toJson(order))
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Order>) {
        items = newItems
        notifyDataSetChanged()
    }
}
