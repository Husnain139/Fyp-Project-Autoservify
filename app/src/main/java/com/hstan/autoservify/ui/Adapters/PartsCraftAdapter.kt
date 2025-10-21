package com.hstan.autoservify.ui.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ItemPartscraftBinding
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import com.hstan.autoservify.ui.ViewHolder.PartsCraftViewHolder
import com.hstan.autoservify.ui.main.Shops.SpareParts.Partscraftdetail

class PartsCraftAdapter(
    private var items: List<PartsCraft>,
    private var showEditDeleteButtons: Boolean = true, // false for customers, true for shop owners
    private val onEditClick: (PartsCraft) -> Unit = {},
    private val onDeleteClick: (PartsCraft) -> Unit = {}
) : RecyclerView.Adapter<PartsCraftViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartsCraftViewHolder {
        val binding = ItemPartscraftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PartsCraftViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PartsCraftViewHolder, position: Int) {
        val part = items[position]
        val context = holder.itemView.context

        holder.binding.SpTitle.text = part.title
        holder.binding.SpDesc.text = part.description
        holder.binding.SpPrice.text = "Rs. ${part.price}"

        // Show inventory icon if inventory is managed (for shopkeeper view)
        if (showEditDeleteButtons && part.manageInventory) {
            holder.binding.inventoryManagedIcon.visibility = View.VISIBLE
        } else {
            holder.binding.inventoryManagedIcon.visibility = View.GONE
        }

        // Check if part is out of stock (inventory managed and quantity is 0)
        val isOutOfStock = part.manageInventory && part.quantity == 0
        
        // Show out of stock badge for customer view
        if (!showEditDeleteButtons && isOutOfStock) {
            holder.binding.stockStatusBadge.visibility = View.VISIBLE
            // Apply gray overlay effect
            holder.itemView.alpha = 0.5f
        } else {
            holder.binding.stockStatusBadge.visibility = View.GONE
            holder.itemView.alpha = 1.0f
        }

        Glide.with(context)
            .load(part.image)
            .placeholder(com.hstan.autoservify.R.drawable.logo)
            .error(com.hstan.autoservify.R.drawable.logo)
            .into(holder.binding.SpPic)

        // Item click listener - disable if out of stock for customers
        if (!showEditDeleteButtons && isOutOfStock) {
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
        } else {
            holder.itemView.isClickable = true
            holder.itemView.setOnClickListener {
                val intent = Intent(context, Partscraftdetail::class.java)
                intent.putExtra("data", Gson().toJson(part))
                context.startActivity(intent)
            }
        }

        // Show/hide edit and delete buttons based on user role
        if (showEditDeleteButtons) {
            holder.binding.editSp.visibility = View.VISIBLE
            holder.binding.spDel.visibility = View.VISIBLE
            
            // Edit click listener
            holder.binding.editSp.setOnClickListener { onEditClick(part) }
            
            // Delete click listener
            holder.binding.spDel.setOnClickListener { onDeleteClick(part) }
        } else {
            holder.binding.editSp.visibility = View.GONE
            holder.binding.spDel.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<PartsCraft>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updatePermissions(showButtons: Boolean) {
        showEditDeleteButtons = showButtons
        notifyDataSetChanged()
    }

    fun getCurrentItems(): List<PartsCraft> = items
}