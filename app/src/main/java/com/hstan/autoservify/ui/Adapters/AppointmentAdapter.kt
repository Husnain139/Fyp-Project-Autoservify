package com.hstan.autoservify.ui.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.main.Shops.Services.Appointment

class AppointmentAdapter(
    private var appointments: MutableList<Appointment>,
    private val onContactClick: ((Appointment) -> Unit)? = null,
    private val onUpdateStatusClick: ((Appointment) -> Unit)? = null,
    private val onItemClick: ((Appointment) -> Unit)? = null
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceNameText: TextView = itemView.findViewById(R.id.serviceNameText)
        val statusText: TextView = itemView.findViewById(R.id.statusText)
        val customerNameText: TextView = itemView.findViewById(R.id.customerNameText)
        val customerEmailText: TextView = itemView.findViewById(R.id.customerEmailText)
        val appointmentDateText: TextView = itemView.findViewById(R.id.appointmentDateText)
        val appointmentTimeText: TextView = itemView.findViewById(R.id.appointmentTimeText)
        val contactButton: TextView = itemView.findViewById(R.id.contactButton)
        val updateStatusButton: TextView = itemView.findViewById(R.id.updateStatusButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]

        holder.apply {
            serviceNameText.text = appointment.serviceName.ifEmpty { "Service" }
            customerNameText.text = appointment.userName.ifEmpty { "Unknown Customer" }
            customerEmailText.text = appointment.userEmail.ifEmpty { "No email provided" }
            appointmentDateText.text = appointment.appointmentDate.ifEmpty { "No date" }
            appointmentTimeText.text = appointment.appointmentTime.ifEmpty { "No time" }

            // Set status with appropriate color
            statusText.text = appointment.status
            when (appointment.status.lowercase()) {
                "pending" -> {
                    statusText.setBackgroundColor(Color.parseColor("#FF9800")) // Orange
                    statusText.setTextColor(Color.WHITE)
                }
                "confirmed" -> {
                    statusText.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
                    statusText.setTextColor(Color.WHITE)
                }
                "completed" -> {
                    statusText.setBackgroundColor(Color.parseColor("#2196F3")) // Blue
                    statusText.setTextColor(Color.WHITE)
                }
                "cancelled" -> {
                    statusText.setBackgroundColor(Color.parseColor("#F44336")) // Red
                    statusText.setTextColor(Color.WHITE)
                }
                else -> {
                    statusText.setBackgroundColor(Color.parseColor("#9E9E9E")) // Gray
                    statusText.setTextColor(Color.WHITE)
                }
            }

            // Set up click listeners
            contactButton.setOnClickListener {
                onContactClick?.invoke(appointment)
            }

            updateStatusButton.setOnClickListener {
                onUpdateStatusClick?.invoke(appointment)
            }

            // Set click listener for the entire item
            itemView.setOnClickListener {
                onItemClick?.invoke(appointment)
            }
        }
    }

    override fun getItemCount(): Int = appointments.size

    fun updateData(newAppointments: List<Appointment>) {
        appointments.clear()
        appointments.addAll(newAppointments)
        notifyDataSetChanged()
    }
}

