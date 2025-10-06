package com.hstan.autoservify.ui.orders

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ItemOrderBinding
import com.hstan.autoservify.ui.main.ViewModels.Order
import com.hstan.autoservify.ui.main.Shops.Services.Appointment

class OrderAdapter(
    private var items: List<Any>, // can be Order or Appointment
    private val onViewClick: ((Any) -> Unit)? = null,
    private val onCancelClick: ((Any) -> Unit)? = null
) : RecyclerView.Adapter<OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        when (val item = items[position]) {
            is Order -> bindOrder(holder, item)
            is Appointment -> bindAppointment(holder, item)
        }
    }

    private fun bindOrder(holder: OrderViewHolder, order: Order) {
        holder.binding.orderItemTitle.text = order.item?.title ?: "Unknown Item"
        holder.binding.orderQty.text = "Qty: ${order.quantity}"
        holder.binding.orderPrice.text = "Rs. ${order.item?.price ?: 0}"
        holder.binding.orderStatus.text = order.status.ifBlank { "pending" }
        holder.binding.orderDate.text = order.orderDate.ifBlank { "No date" }

        Glide.with(holder.itemView.context)
            .load(order.item?.image)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(holder.binding.orderItemImage)

        holder.binding.orderView.setOnClickListener { 
            onViewClick?.invoke(order)
            
            // Navigate to OrderDetailActivity
            val context = holder.itemView.context
            val intent = Intent(context, OrderDetailActivity::class.java)
            intent.putExtra("order_data", Gson().toJson(order))
            context.startActivity(intent)
        }
        holder.binding.orderCancel.setOnClickListener { onCancelClick?.invoke(order) }
        holder.itemView.setOnClickListener { 
            onViewClick?.invoke(order)
            
            // Navigate to OrderDetailActivity
            val context = holder.itemView.context
            val intent = Intent(context, OrderDetailActivity::class.java)
            intent.putExtra("order_data", Gson().toJson(order))
            context.startActivity(intent)
        }
    }

    private fun bindAppointment(holder: OrderViewHolder, appointment: Appointment) {
        holder.binding.orderItemTitle.text = appointment.serviceName.ifBlank { "Service" }
        holder.binding.orderQty.text = "${appointment.appointmentDate} ${appointment.appointmentTime}"
        holder.binding.orderPrice.text = "Bill: Rs. ${appointment.bill.ifBlank { "0" }}"
        holder.binding.orderStatus.text = appointment.status.ifBlank { "Pending" }
        holder.binding.orderDate.text = "Customer: ${appointment.userName}"

        Glide.with(holder.itemView.context)
            .load(R.drawable.logo) // Appointment has no image field
            .placeholder(R.drawable.logo)
            .into(holder.binding.orderItemImage)

        holder.binding.orderView.setOnClickListener { 
            onViewClick?.invoke(appointment)
            
            // Navigate to AppointmentDetailActivity
            val context = holder.itemView.context
            val intent = Intent(context, AppointmentDetailActivity::class.java)
            intent.putExtra(AppointmentDetailActivity.EXTRA_APPOINTMENT, Gson().toJson(appointment))
            context.startActivity(intent)
        }
        holder.binding.orderCancel.setOnClickListener { onCancelClick?.invoke(appointment) }
        holder.itemView.setOnClickListener { 
            onViewClick?.invoke(appointment)
            
            // Navigate to AppointmentDetailActivity
            val context = holder.itemView.context
            val intent = Intent(context, AppointmentDetailActivity::class.java)
            intent.putExtra(AppointmentDetailActivity.EXTRA_APPOINTMENT, Gson().toJson(appointment))
            context.startActivity(intent)
        }
    }

    fun updateData(newItems: List<Any>) {
        try {
            items = newItems.toList() // Create a defensive copy
            notifyDataSetChanged()
        } catch (e: Exception) {
            println("Error updating adapter data: ${e.message}")
            e.printStackTrace()
        }
    }
}
