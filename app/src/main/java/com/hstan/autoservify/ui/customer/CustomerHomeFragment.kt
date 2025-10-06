package com.hstan.autoservify.ui.customer

import android.os.Bundle
import android.content.Intent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.ui.main.Shops.AddShopActivity
import com.hstan.autoservify.ui.main.Shops.Shop
import kotlinx.coroutines.launch
import com.hstan.autoservify.ui.Adapters.ShopAdapter
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.databinding.FragmentCustomerHomeBinding

class CustomerHomeFragment : Fragment() {

    lateinit var adapter: ShopAdapter
    val items = ArrayList<Shop>()
    lateinit var binding: FragmentCustomerHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize shop adapter
        adapter = ShopAdapter(items)

        // Setup RecyclerView
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(context)

        // Setup click listeners
        setupClickListeners()

        // Load shops
        loadShops()
    }

    private fun setupClickListeners() {
        binding.fabAddShop.setOnClickListener {
            startActivity(Intent(requireContext(), AddShopActivity::class.java))
        }
    }

    private fun loadShops() {
        lifecycleScope.launch {
            val shopRepository = ShopRepository()
            shopRepository.getShops().collect { shops ->
                items.clear()
                items.addAll(shops)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun checkUserPermissions() {
        lifecycleScope.launch {
            val authRepository = AuthRepository()
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                val result = authRepository.getUserProfile(currentUser.uid)
                if (result.isSuccess) {
                    val userProfile = result.getOrThrow()
                    // Only show add shop FAB for admin users or in development
                    binding.fabAddShop.visibility = View.GONE // Customers shouldn't add shops
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkUserPermissions()
    }
}
