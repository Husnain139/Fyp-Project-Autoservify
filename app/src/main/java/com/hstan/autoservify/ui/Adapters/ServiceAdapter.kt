package com.hstan.autoservify.ui.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ItemServiceBinding
import com.hstan.autoservify.ui.ViewHolder.serviceViewHolder
import com.hstan.autoservify.ui.main.Shops.Services.Service

class ServiceAdapter(
    private var items: List<Service>,
    private val onItemClick: (Service) -> Unit,
    private var showEditDeleteButtons: Boolean = true, // false for customers, true for shop owners
    private val onEditClick: (Service) -> Unit = {},
    private val onDeleteClick: (Service) -> Unit = {}
) : RecyclerView.Adapter<serviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): serviceViewHolder {
        val binding = ItemServiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return serviceViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: serviceViewHolder, position: Int) {
        val service = items[position]

        holder.binding.ServiceTitle.text = service.name
        holder.binding.ServDesc.text = service.description
        holder.binding.ServPrice.text = "Rs. ${service.price}"

        // Load service image from Cloudinary or use default logo
        Glide.with(holder.itemView.context)
            .load(service.imageUrl.ifEmpty { R.drawable.logo })
            .error(R.drawable.logo)
            .placeholder(R.drawable.logo)
            .into(holder.binding.ServicePic)

        // Item click listener
        holder.itemView.setOnClickListener { onItemClick(service) }

        // Show/hide edit and delete buttons based on user role
        if (showEditDeleteButtons) {
            holder.binding.editServ.visibility = View.VISIBLE
            holder.binding.spDel.visibility = View.VISIBLE
            
            // Edit click listener
            holder.binding.editServ.setOnClickListener { onEditClick(service) }
            
            // Delete click listener
            holder.binding.spDel.setOnClickListener { onDeleteClick(service) }
        } else {
            holder.binding.editServ.visibility = View.GONE
            holder.binding.spDel.visibility = View.GONE
        }
    }

    fun updateData(newItems: List<Service>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updatePermissions(showButtons: Boolean) {
        showEditDeleteButtons = showButtons
        notifyDataSetChanged()
    }

    fun getCurrentItems(): List<Service> = items
}
