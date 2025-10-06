package com.hstan.autoservify.ui.main.Shops.SpareParts

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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


//        binding.productImage.setImageResource(partCraft.image.toInt())


        binding.AddtoCartButton.setOnClickListener {
            try {
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
