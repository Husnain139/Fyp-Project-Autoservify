package com.hstan.autoservify.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hstan.autoservify.R

class SelectedPartsAdapter(
    private val selectedParts: List<AddAppointmentPartsActivity.SelectedPartItem>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedPartsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val partNameText: TextView = itemView.findViewById(R.id.selectedPartName)
        val partQuantityText: TextView = itemView.findViewById(R.id.selectedPartQuantity)
        val partPriceText: TextView = itemView.findViewById(R.id.selectedPartPrice)
        val removeButton: ImageButton = itemView.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_part, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectedItem = selectedParts[position]
        val part = selectedItem.part

        holder.partNameText.text = part.title
        holder.partQuantityText.text = "Qty: ${selectedItem.quantity}"
        
        val totalPrice = part.price * selectedItem.quantity
        holder.partPriceText.text = "Rs. $totalPrice"

        holder.removeButton.setOnClickListener {
            onRemoveClick(position)
        }
    }

    override fun getItemCount(): Int = selectedParts.size
}

