package com.hstan.autoservify.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.main.ViewModels.Order

class AppointmentSparePartsAdapter(
    private val spareParts: List<Order>
) : RecyclerView.Adapter<AppointmentSparePartsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val partNameText: TextView = itemView.findViewById(R.id.partNameText)
        val partQuantityText: TextView = itemView.findViewById(R.id.partQuantityText)
        val partPriceText: TextView = itemView.findViewById(R.id.partPriceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_spare_part, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = spareParts[position]
        val part = order.item

        holder.partNameText.text = part?.title ?: "Unknown Part"
        holder.partQuantityText.text = "Qty: ${order.quantity}"
        
        val totalPrice = (part?.price ?: 0) * order.quantity
        holder.partPriceText.text = "Rs. $totalPrice"
    }

    override fun getItemCount(): Int = spareParts.size
}

