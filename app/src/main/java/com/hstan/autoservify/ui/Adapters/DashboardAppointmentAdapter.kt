package com.hstan.autoservify.ui.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ItemOrderBinding
import com.hstan.autoservify.ui.main.Shops.Services.Appointment

class DashboardAppointmentAdapter(
    private var items: List<Appointment>,
    private val onItemClick: (Appointment) -> Unit = {}
) : RecyclerView.Adapter<DashboardAppointmentAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppointmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = items[position]
        
        holder.binding.orderItemTitle.text = appointment.serviceName
        holder.binding.orderQty.text = "Customer: ${appointment.userName}"
        holder.binding.orderPrice.text = "Bill: ₹${appointment.bill}"
        holder.binding.orderStatus.text = appointment.status
        holder.binding.orderDate.text = "${appointment.appointmentDate} ${appointment.appointmentTime}"
        
        // Load service image
        Glide.with(holder.itemView.context)
            .load(appointment.serviceImageUrl.takeIf { it.isNotEmpty() } ?: R.drawable.logo)
            .placeholder(R.drawable.logo)
            .error(R.drawable.logo)
            .into(holder.binding.orderItemImage)
        
        // Hide cancel and delete buttons - dashboard is view-only
        holder.binding.orderCancelButton.visibility = View.GONE
        holder.binding.orderDelete.visibility = View.GONE
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(appointment)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Appointment>) {
        items = newItems
        notifyDataSetChanged()
    }
}
