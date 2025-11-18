package com.hstan.autoservify.ui.Adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ItemShopsBinding
import com.hstan.autoservify.ui.main.Shops.Shop
import com.hstan.autoservify.ui.main.Shops.ShopActivity

class ShopAdapter(private var items: List<Shop>) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    inner class ShopViewHolder(val binding: ItemShopsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ItemShopsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = items[position]
        holder.binding.titleInput.text = shop.title
        holder.binding.description.text = shop.description
        holder.binding.location.text = "📍 ${shop.address}"

        // Load shop image
        com.bumptech.glide.Glide.with(holder.itemView.context)
            .load(shop.imageUrl.ifEmpty { com.hstan.autoservify.R.drawable.logo })
            .error(com.hstan.autoservify.R.drawable.logo)
            .placeholder(com.hstan.autoservify.R.drawable.logo)
            .into(holder.binding.imageView)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ShopActivity::class.java)
            intent.putExtra("data", Gson().toJson(shop))
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Shop>) {
        items = newItems
        notifyDataSetChanged()
    }
}