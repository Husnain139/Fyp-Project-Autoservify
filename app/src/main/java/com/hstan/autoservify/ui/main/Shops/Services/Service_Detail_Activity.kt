package com.hstan.autoservify.ui.main.Shops.Services

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.gson.Gson
import com.hstan.autoservify.databinding.ActivityServiceDetailBinding
import com.hstan.autoservify.ui.main.Cart.BookAppointment_Activity

class Service_Detail_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceDetailBinding
    private lateinit var service: Service
    private val viewModel: ServiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Get service object from intent
        val jsonData = intent.getStringExtra("data")
        service = Gson().fromJson(jsonData, Service::class.java)

        // ✅ Call ViewModel to fetch latest service info
        viewModel.loadServiceById(service.id)

        // ✅ Observe service updates
        viewModel.selectedService.observe(this, Observer { updatedService ->
            updatedService?.let {
                service = it
                binding.srvTitle.text = it.name
                binding.srvDesc.text = it.description
                binding.srvPrice.text = "Rs. ${it.price}"
                
                // Load service image
                com.bumptech.glide.Glide.with(this)
                    .load(it.imageUrl.ifEmpty { com.hstan.autoservify.R.drawable.logo })
                    .error(com.hstan.autoservify.R.drawable.logo)
                    .placeholder(com.hstan.autoservify.R.drawable.logo)
                    .into(binding.ShopPic)
            }
        })

        viewModel.failureMessage.observe(this, Observer { msg ->
            msg?.let {
                // Show error if needed
            }
        })

        // ✅ Book appointment → only send id + name
        binding.BookAppointmentButton.setOnClickListener {
            val intent = Intent(this, BookAppointment_Activity::class.java)
            intent.putExtra("service_id", service.id)
            intent.putExtra("service_name", service.name)
            startActivity(intent)
        }
    }
}
