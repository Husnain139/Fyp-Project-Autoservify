package com.hstan.autoservify.ui.main.Shops.SpareParts

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ActivityPartscraftdetailBinding
import com.hstan.autoservify.ui.orders.CreateOrderActivity

class Partscraftdetail :  AppCompatActivity() {

    lateinit var binding: ActivityPartscraftdetailBinding;
    lateinit var partCraft: PartsCraft;

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPartscraftdetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val partData = intent.getStringExtra("data")
        if (partData == null) {
            Toast.makeText(this, "Error: No item data received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        try {
            partCraft = Gson().fromJson(partData, PartsCraft::class.java)
            if (partCraft == null) {
                Toast.makeText(this, "Error: Invalid item data", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading item: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.spTitle.text = partCraft.title ?: "Unknown Item"
        binding.spDesc.text = partCraft.description ?: "No description"  
		binding.spPrice.text = "Rs ${partCraft.price}"

        // Show inventory status if managed
        if (partCraft.manageInventory) {
            binding.inventoryStatusContainer.visibility = View.VISIBLE
            binding.availableStock.text = "Available Stock: ${partCraft.quantity} units"
            
            // Disable Add to Cart if out of stock
            if (partCraft.quantity == 0) {
                binding.AddtoCartButton.isEnabled = false
                binding.AddtoCartButton.text = "Out of Stock"
                binding.AddtoCartButton.alpha = 0.5f
            }
        } else {
            binding.inventoryStatusContainer.visibility = View.GONE
        }

//        binding.productImage.setImageResource(partCraft.image.toInt())


        binding.AddtoCartButton.setOnClickListener {
            try {
                // Check stock before proceeding
                if (partCraft.manageInventory && partCraft.quantity == 0) {
                    Toast.makeText(this, "This item is out of stock", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val intent = Intent(this, CreateOrderActivity::class.java)
                val partJson = Gson().toJson(partCraft)
                intent.putExtra("data", partJson)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening cart: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
