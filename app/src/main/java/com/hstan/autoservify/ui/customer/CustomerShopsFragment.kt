package com.hstan.autoservify.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.databinding.FragmentCustomerShopsBinding
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.ui.Adapters.ShopAdapter
import com.hstan.autoservify.ui.main.Shops.Shop
import kotlinx.coroutines.launch

class CustomerShopsFragment : Fragment() {

    private var _binding: FragmentCustomerShopsBinding? = null
    private val binding get() = _binding!!

    private lateinit var shopAdapter: ShopAdapter
    private val shops = mutableListOf<Shop>()
    private val shopRepository = ShopRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerShopsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadShops()
    }

    private fun setupRecyclerView() {
        shopAdapter = ShopAdapter(shops)
        
        binding.shopsRecyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = shopAdapter
        }
    }

    private fun loadShops() {
        _binding?.loadingIndicator?.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                shopRepository.getShops().collect { allShops ->
                    if (_binding == null) return@collect
                    
                    shops.clear()
                    shops.addAll(allShops)
                    shopAdapter.notifyDataSetChanged()
                    
                    _binding?.let { binding ->
                        binding.loadingIndicator.visibility = View.GONE
                        
                        // Update empty state
                        if (shops.isEmpty()) {
                            binding.shopsRecyclerview.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE
                        } else {
                            binding.shopsRecyclerview.visibility = View.VISIBLE
                            binding.emptyState.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _binding?.let { binding ->
                    binding.loadingIndicator.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    binding.shopsRecyclerview.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

