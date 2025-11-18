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
import com.hstan.autoservify.databinding.FragmentShopkeeperPartsBinding
import com.hstan.autoservify.ui.Adapters.PartsCraftAdapter
import com.hstan.autoservify.ui.main.Shops.SpareParts.Addpartscraft
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraftViewModel
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.launch

class ShopkeeperPartsFragment : Fragment() {

    private var _binding: FragmentShopkeeperPartsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: PartsCraftAdapter
    private lateinit var viewModel: PartsCraftViewModel
    private val items = mutableListOf<PartsCraft>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopkeeperPartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(this)[PartsCraftViewModel::class.java]
        
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
        loadShopkeeperParts()
    }

    private fun setupRecyclerView() {
        adapter = PartsCraftAdapter(
            items = items,
            showEditDeleteButtons = true,
            onEditClick = { partsCraft ->
                // Open edit parts activity
                val intent = Intent(requireContext(), Addpartscraft::class.java)
                intent.putExtra("partscraft_data", com.google.gson.Gson().toJson(partsCraft))
                startActivity(intent)
            },
            onDeleteClick = { partsCraft ->
                showDeleteConfirmationDialog(partsCraft)
            }
        )
        
        binding.partsRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@ShopkeeperPartsFragment.adapter
        }
        
        updateEmptyState()
    }

    private fun setupClickListeners() {
        binding.fabAddPart.setOnClickListener {
            startActivity(Intent(requireContext(), Addpartscraft::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.partsCrafts.observe(viewLifecycleOwner) { partsList ->
            items.clear()
            items.addAll(partsList)
            adapter.notifyDataSetChanged()
            updateEmptyState()
        }
    }

    private fun loadShopkeeperParts() {
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
                            viewModel.loadPartsCraftsByShopId(shopId)
                        } else {
                            Toast.makeText(context, "No shop found for your account", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                println("ShopkeeperPartsFragment: Error loading parts: ${e.message}")
                // Silently handle error - no toast message
            }
        }
    }

    private fun showDeleteConfirmationDialog(partsCraft: PartsCraft) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Part")
            .setMessage("Are you sure you want to delete '${partsCraft.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deletePart(partsCraft)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePart(partsCraft: PartsCraft) {
        lifecycleScope.launch {
            try {
                viewModel.deletePartsCraft(partsCraft.id)
                items.remove(partsCraft)
                adapter.notifyDataSetChanged()
                updateEmptyState()
                Toast.makeText(context, "Part deleted successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete part", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState() {
        if (items.isEmpty()) {
            binding.partsRecyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.partsRecyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
