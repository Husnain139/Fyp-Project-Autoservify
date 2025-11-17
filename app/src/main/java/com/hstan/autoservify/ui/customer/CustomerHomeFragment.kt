package com.hstan.autoservify.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.hstan.autoservify.databinding.FragmentCustomerHomeBinding
import com.hstan.autoservify.model.repositories.PartsCraftRepository
import com.hstan.autoservify.model.repositories.ServiceRepository
import com.hstan.autoservify.ui.Adapters.PartsCraftAdapter
import com.hstan.autoservify.ui.Adapters.ServiceAdapter
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraftActivity
import com.hstan.autoservify.ui.main.Shops.Services.Service
import com.hstan.autoservify.ui.main.Shops.Services.ServicesActivity
import com.hstan.autoservify.ui.main.Shops.Services.Service_Detail_Activity
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import com.hstan.autoservify.ui.main.Shops.SpareParts.Partscraftdetail
import kotlinx.coroutines.launch

class CustomerHomeFragment : Fragment() {

    private var _binding: FragmentCustomerHomeBinding?= null
    private val binding get() = _binding!!

    private lateinit var servicesAdapter: ServiceAdapter
    private lateinit var sparePartsAdapter: PartsCraftAdapter
    
    private val services = mutableListOf<Service>()
    private val spareParts = mutableListOf<PartsCraft>()

    private val serviceRepository = ServiceRepository()
    private val partsCraftRepository = PartsCraftRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupServicesRecyclerView()
        setupSparePartsRecyclerView()
        setupClickListeners()
        loadData()
    }

    private fun setupServicesRecyclerView() {
        servicesAdapter = ServiceAdapter(
            items = services,
            onItemClick = { service ->
                val intent = Intent(requireContext(), Service_Detail_Activity::class.java)
                intent.putExtra("data", Gson().toJson(service))
                startActivity(intent)
            },
            showEditDeleteButtons = false, // Customer view - no edit/delete
            onEditClick = {},
            onDeleteClick = {}
        )
        
        binding.servicesRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = servicesAdapter
        }
    }

    private fun setupSparePartsRecyclerView() {
        sparePartsAdapter = PartsCraftAdapter(
            items = spareParts,
            showEditDeleteButtons = false, // Customer view - no edit/delete
            onEditClick = {},
            onDeleteClick = {}
        )
        
        binding.sparePartsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = sparePartsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.viewAllServices.setOnClickListener {
            startActivity(Intent(requireContext(), ServicesActivity::class.java))
        }

        binding.viewAllSpareParts.setOnClickListener {
            startActivity(Intent(requireContext(), PartsCraftActivity::class.java))
        }
    }

    private fun loadData() {
        _binding?.loadingIndicator?.visibility = View.VISIBLE
        
        // Load all services
        lifecycleScope.launch {
            try {
                val allServices = serviceRepository.getServices()
                if (_binding == null) return@launch
                
                services.clear()
                services.addAll(allServices)
                servicesAdapter.notifyDataSetChanged()
                
                // Update empty state
                _binding?.let { binding ->
                    if (services.isEmpty()) {
                        binding.servicesRecyclerView.visibility = View.GONE
                        binding.servicesEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.servicesRecyclerView.visibility = View.VISIBLE
                        binding.servicesEmptyState.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _binding?.let { binding ->
                    binding.servicesEmptyState.visibility = View.VISIBLE
                    binding.servicesRecyclerView.visibility = View.GONE
                }
            }
        }

        // Load all spare parts
        lifecycleScope.launch {
            try {
                partsCraftRepository.getPartsCrafts().collect { allParts ->
                    if (_binding == null) return@collect
                    
                    spareParts.clear()
                    spareParts.addAll(allParts)
                    sparePartsAdapter.notifyDataSetChanged()
                    
                    // Update empty state
                    _binding?.let { binding ->
                        if (spareParts.isEmpty()) {
                            binding.sparePartsRecyclerView.visibility = View.GONE
                            binding.sparePartsEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.sparePartsRecyclerView.visibility = View.VISIBLE
                            binding.sparePartsEmptyState.visibility = View.GONE
                        }
                        
                        binding.loadingIndicator.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _binding?.let { binding ->
                    binding.sparePartsEmptyState.visibility = View.VISIBLE
                    binding.sparePartsRecyclerView.visibility = View.GONE
                    binding.loadingIndicator.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
