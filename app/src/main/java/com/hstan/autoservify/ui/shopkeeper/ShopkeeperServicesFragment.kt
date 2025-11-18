package com.hstan.autoservify.ui.shopkeeper

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.FragmentShopkeeperServicesBinding
import com.hstan.autoservify.ui.Adapters.ServiceAdapter
import com.hstan.autoservify.ui.main.Shops.Services.Add_Service_Activity
import com.hstan.autoservify.ui.main.Shops.Services.Service
import com.hstan.autoservify.ui.main.Shops.Services.ServiceViewModel
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class ShopkeeperServicesFragment : Fragment() {

    private var _binding: FragmentShopkeeperServicesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ServiceAdapter
    private lateinit var viewModel: ServiceViewModel
    private val items = mutableListOf<Service>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopkeeperServicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[ServiceViewModel::class.java]
        
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
        loadShopkeeperServices()
    }

    private fun setupRecyclerView() {
        adapter = ServiceAdapter(
            items = items,
            onItemClick = { service ->
                // Open service detail page
                val intent = Intent(requireContext(), com.hstan.autoservify.ui.main.Shops.Services.Service_Detail_Activity::class.java)
                intent.putExtra("data", com.google.gson.Gson().toJson(service))
                startActivity(intent)
            },
            showEditDeleteButtons = true,
            onEditClick = { service ->
                // Open edit service activity
                val intent = Intent(requireContext(), Add_Service_Activity::class.java)
                intent.putExtra("service_data", com.google.gson.Gson().toJson(service))
                startActivity(intent)
            },
            onDeleteClick = { service ->
                showDeleteConfirmationDialog(service)
            }
        )
        
        binding.servicesRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@ShopkeeperServicesFragment.adapter
        }
        
        updateEmptyState()
    }

    private fun setupClickListeners() {
        binding.fabAddService.setOnClickListener {
            startActivity(Intent(requireContext(), Add_Service_Activity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.services.observe(viewLifecycleOwner) { servicesList ->
            items.clear()
            items.addAll(servicesList)
            adapter.notifyDataSetChanged()
            updateEmptyState()
        }
    }

    private fun loadShopkeeperServices() {
        lifecycleScope.launch {
            try {
                val authRepository = AuthRepository()
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser != null) {
                    val result = authRepository.getUserProfile(currentUser.uid)
                    if (result.isSuccess) {
                        val userProfile = result.getOrThrow()
                        val shopId = userProfile.shopId
                        
                        if (!shopId.isNullOrEmpty()) {
                            viewModel.loadServicesByShopId(shopId)
                        } else {
                            Toast.makeText(context, "No shop found for your account", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading services", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(service: Service) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Service")
            .setMessage("Are you sure you want to delete '${service.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteService(service)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteService(service: Service) {
        lifecycleScope.launch {
            try {
                viewModel.deleteService(service.id)
                items.remove(service)
                adapter.notifyDataSetChanged()
                updateEmptyState()
                Toast.makeText(context, "Service deleted successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete service", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState() {
        if (items.isEmpty()) {
            binding.servicesRecyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.servicesRecyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
