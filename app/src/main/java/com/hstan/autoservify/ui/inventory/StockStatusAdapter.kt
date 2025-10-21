package com.hstan.autoservify.ui.inventory

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ItemStockStatusBinding
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft

class StockStatusAdapter(
    private val items: List<PartsCraft>,
    private val isOutOfStock: Boolean,
    private val onItemClick: (PartsCraft) -> Unit
) : RecyclerView.Adapter<StockStatusAdapter.StockStatusViewHolder>() {

    inner class StockStatusViewHolder(val binding: ItemStockStatusBinding) : 
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockStatusViewHolder {
        val binding = ItemStockStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockStatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockStatusViewHolder, position: Int) {
        val part = items[position]
        
        with(holder.binding) {
            // Set part details
            partTitle.text = part.title
            partPrice.text = "Rs. ${part.price}"
            partQuantity.text = "Available: ${part.quantity} units"
            
            // Set status badge
            if (isOutOfStock) {
                stockStatusBadge.text = "Out of Stock"
                stockStatusBadge.setBackgroundColor(Color.parseColor("#D32F2F"))
            } else {
                stockStatusBadge.text = "Low Stock"
                stockStatusBadge.setBackgroundColor(Color.parseColor("#F57C00"))
            }
            
            // Load image
            Glide.with(holder.itemView.context)
                .load(part.image)
                .placeholder(R.drawable.logo)
                .error(R.drawable.logo)
                .into(partImage)
            
            // Set click listener
            root.setOnClickListener {
                onItemClick(part)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}

