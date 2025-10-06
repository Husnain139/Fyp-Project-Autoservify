package com.hstan.autoservify.ui.main.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.databinding.FragmentSearchBinding
import com.hstan.autoservify.ui.Adapters.ShopAdapter
import com.hstan.autoservify.ui.main.Shops.Shop
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.model.repositories.ServiceRepository
import com.hstan.autoservify.model.repositories.PartsCraftRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var shopAdapter: ShopAdapter
    private val searchResults = ArrayList<Shop>()
    private var searchJob: Job? = null
    
    private val shopRepository = ShopRepository()
    private val serviceRepository = ServiceRepository()
    private val partsCraftRepository = PartsCraftRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearchFunctionality()
        setupChips()
        loadAllShops() // Load shops by default
    }

    private fun setupRecyclerView() {
        shopAdapter = ShopAdapter(searchResults)
        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shopAdapter
        }
    }

    private fun setupSearchFunctionality() {
        binding.searchEditText.addTextChangedListener { text ->
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                try {
                    delay(300) // Debounce search
                    if (isAdded && _binding != null) {
                        performSearch(text.toString().trim())
                    }
                } catch (e: Exception) {
                    println("SearchFragment: Search functionality error: ${e.message}")
                }
            }
        }
    }

    private fun setupChips() {
        binding.chipShops.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                uncheckOtherChips(binding.chipShops)
                performSearch(binding.searchEditText.text.toString().trim())
            }
        }

        binding.chipServices.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                uncheckOtherChips(binding.chipServices)
                Toast.makeText(context, "Service search coming soon", Toast.LENGTH_SHORT).show()
                // For now, show shops
                performSearch(binding.searchEditText.text.toString().trim())
            }
        }

        binding.chipParts.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                uncheckOtherChips(binding.chipParts)
                Toast.makeText(context, "Parts search coming soon", Toast.LENGTH_SHORT).show()
                // For now, show shops
                performSearch(binding.searchEditText.text.toString().trim())
            }
        }
    }

    private fun uncheckOtherChips(selectedChip: View) {
        when (selectedChip.id) {
            binding.chipShops.id -> {
                binding.chipServices.isChecked = false
                binding.chipParts.isChecked = false
            }
            binding.chipServices.id -> {
                binding.chipShops.isChecked = false
                binding.chipParts.isChecked = false
            }
            binding.chipParts.id -> {
                binding.chipShops.isChecked = false
                binding.chipServices.isChecked = false
            }
        }
    }

    private fun performSearch(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded || _binding == null) {
                    println("SearchFragment: Fragment not ready for search")
                    return@launch
                }
                
                if (query.isEmpty()) {
                    loadAllShops()
                } else {
                    searchShops(query)
                }
            } catch (e: Exception) {
                println("SearchFragment: Search failed: ${e.message}")
                if (isAdded && context != null) {
                    Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadAllShops() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded || _binding == null) return@launch
                
                shopRepository.getShops().collect { shops ->
                    if (isAdded && _binding != null) {
                        updateResults(shops)
                    }
                }
            } catch (e: Exception) {
                println("SearchFragment: Error loading shops: ${e.message}")
                if (isAdded && _binding != null) {
                    updateResults(emptyList())
                }
            }
        }
    }

    private fun searchShops(query: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded || _binding == null) return@launch
                
                shopRepository.getShops().collect { allShops ->
                    if (isAdded && _binding != null) {
                        val filteredShops = allShops.filter { shop ->
                            shop.title.contains(query, ignoreCase = true) ||
                            shop.description.contains(query, ignoreCase = true) ||
                            shop.address.contains(query, ignoreCase = true)
                        }
                        updateResults(filteredShops)
                    }
                }
            } catch (e: Exception) {
                println("SearchFragment: Error searching shops: ${e.message}")
                if (isAdded && _binding != null) {
                    updateResults(emptyList())
                }
            }
        }
    }

    private fun updateResults(results: List<Shop>) {
        searchResults.clear()
        searchResults.addAll(results)
        shopAdapter.notifyDataSetChanged()
        
        // Show/hide empty state
        if (results.isEmpty() && binding.searchEditText.text.toString().isNotEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.searchResultsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.searchResultsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        println("SearchFragment: onDestroyView called")
        searchJob?.cancel()
        println("SearchFragment: Search job cancelled")
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        println("SearchFragment: onPause called")
        // Cancel any ongoing searches when leaving the fragment
        searchJob?.cancel()
    }

    override fun onResume() {
        super.onResume()
        println("SearchFragment: onResume called")
    }
}