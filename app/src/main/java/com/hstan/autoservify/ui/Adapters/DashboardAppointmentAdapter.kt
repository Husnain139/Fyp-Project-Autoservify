package com.hstan.autoservify.ui.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        
        // Hide edit/cancel buttons for dashboard view
        holder.binding.orderView.visibility = View.VISIBLE
        holder.binding.orderCancel.visibility = View.GONE
        
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
