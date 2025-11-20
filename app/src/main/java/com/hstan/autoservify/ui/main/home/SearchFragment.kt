package com.hstan.autoservify.ui.main.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.databinding.FragmentSearchBinding
import com.hstan.autoservify.ui.Adapters.ShopAdapter
import com.hstan.autoservify.ui.main.Shops.Shop
import com.hstan.autoservify.model.repositories.ShopRepository
import com.hstan.autoservify.model.repositories.ServiceRepository
import com.hstan.autoservify.model.repositories.PartsCraftRepository
import com.hstan.autoservify.ui.Adapters.PartsCraftAdapter
import com.hstan.autoservify.ui.Adapters.ServiceAdapter
import com.hstan.autoservify.ui.main.Shops.Services.Service
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SearchFragment : Fragment() {

    private lateinit var ShopAdapter: ShopAdapter
    private lateinit var serviceAdapter: ServiceAdapter
    private lateinit var partsAdapter: PartsCraftAdapter


    private val shopResults = ArrayList<Shop>()
    private val serviceResults = ArrayList<Service>()
    private val partsResults = ArrayList<PartsCraft>()

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
        shopAdapter = ShopAdapter(shopResults)

        serviceAdapter = ServiceAdapter(
            items = serviceResults,
            onItemClick = { service -> },
            showEditDeleteButtons = false // customer view
        )

        partsAdapter = PartsCraftAdapter(
            items = partsResults,
            showEditDeleteButtons = false
        )

        // Use searchResults for shopAdapter
        shopAdapter = ShopAdapter(searchResults)

        // Set default layout for shop adapter → Linear
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
                binding.searchResultsRecyclerView.layoutManager = LinearLayoutManager(context) // linear for shops
                binding.searchResultsRecyclerView.adapter = shopAdapter
                performSearch(binding.searchEditText.text.toString().trim())
            }
        }

        binding.chipServices.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                uncheckOtherChips(binding.chipServices)
                binding.searchResultsRecyclerView.layoutManager = GridLayoutManager(context, 2) // grid for services
                binding.searchResultsRecyclerView.adapter = serviceAdapter
                performSearch(binding.searchEditText.text.toString().trim())
            }
        }

        binding.chipParts.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                uncheckOtherChips(binding.chipParts)
                binding.searchResultsRecyclerView.layoutManager = GridLayoutManager(context, 2) // grid for parts
                binding.searchResultsRecyclerView.adapter = partsAdapter
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

            when {
                binding.chipShops.isChecked -> {
                    if (query.isEmpty()) loadAllShops()
                    else searchShops(query)
                }

                binding.chipServices.isChecked -> {
                    if (query.isEmpty()) loadAllServices()
                    else searchServices(query)
                }

                binding.chipParts.isChecked -> {
                    if (query.isEmpty()) loadAllParts()
                    else searchParts(query)
                }
            }
        }
    }

    private fun loadAllServices() = lifecycleScope.launch {
        val list = serviceRepository.getServices()
        updateServiceResults(list)
    }

    private fun searchServices(query: String) = lifecycleScope.launch {
        val list = serviceRepository.getServices()

        val filtered = list.filter { service ->
            service.name.contains(query, true) ||
                    service.description.contains(query, true)
        }

        updateServiceResults(filtered)
    }


    private fun loadAllParts() = lifecycleScope.launch {
        partsCraftRepository.getPartsCrafts().collect { list ->
            updatePartsResults(list)
        }
    }

    private fun searchParts(query: String) = lifecycleScope.launch {
        partsCraftRepository.getPartsCrafts().collect { list ->
            val filtered = list.filter { part ->
                part.title.contains(query, true) ||
                        part.description.contains(query, true)
            }
            updatePartsResults(filtered)
        }
    }

    private fun updateShopResults(results: List<Shop>) {
        shopResults.clear()
        shopResults.addAll(results)
        shopAdapter.notifyDataSetChanged()
    }

    private fun updatePartsResults(results: List<PartsCraft>) {
        partsResults.clear()
        partsResults.addAll(results)
        partsAdapter.notifyDataSetChanged()
    }

    private fun updateServiceResults(results: List<Service>) {
        serviceResults.clear()
        serviceResults.addAll(results)
        serviceAdapter.notifyDataSetChanged()
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
                            shop.address.contains(query, ignoreCase = true) ||
                            shop.city.contains(query, ignoreCase = true)
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