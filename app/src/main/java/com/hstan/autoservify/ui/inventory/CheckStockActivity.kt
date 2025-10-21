package com.hstan.autoservify.ui.inventory

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.hstan.autoservify.databinding.ActivityCheckStockBinding
import com.hstan.autoservify.model.repositories.AuthRepository
import com.hstan.autoservify.ui.main.Shops.SpareParts.Addpartscraft
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraft
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CheckStockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckStockBinding
    private lateinit var outOfStockAdapter: StockStatusAdapter
    private lateinit var lowStockAdapter: StockStatusAdapter
    private val firestore = FirebaseFirestore.getInstance()
    
    private val outOfStockItems = mutableListOf<PartsCraft>()
    private val lowStockItems = mutableListOf<PartsCraft>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckStockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        loadStockData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh stock data when returning from edit
        loadStockData()
    }

    private fun setupToolbar() {
        binding.backBtn.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerViews() {
        // Out of Stock RecyclerView
        outOfStockAdapter = StockStatusAdapter(
            items = outOfStockItems,
            isOutOfStock = true,
            onItemClick = { part ->
                // Open edit activity
                val intent = Intent(this, Addpartscraft::class.java)
                intent.putExtra("partscraft_data", Gson().toJson(part))
                startActivity(intent)
            }
        )
        binding.outOfStockRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CheckStockActivity)
            adapter = outOfStockAdapter
        }

        // Low Stock RecyclerView
        lowStockAdapter = StockStatusAdapter(
            items = lowStockItems,
            isOutOfStock = false,
            onItemClick = { part ->
                // Open edit activity
                val intent = Intent(this, Addpartscraft::class.java)
                intent.putExtra("partscraft_data", Gson().toJson(part))
                startActivity(intent)
            }
        )
        binding.lowStockRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CheckStockActivity)
            adapter = lowStockAdapter
        }
    }

    private fun loadStockData() {
        lifecycleScope.launch {
            try {
                val authRepository = AuthRepository()
                val currentUser = authRepository.getCurrentUser()
                
                if (currentUser != null) {
                    val profileResult = authRepository.getUserProfile(currentUser.uid)
                    if (profileResult.isSuccess) {
                        val userProfile = profileResult.getOrThrow()
                        val shopId = userProfile.shopId
                        
                        if (!shopId.isNullOrEmpty()) {
                            // Fetch parts with managed inventory
                            val querySnapshot = firestore.collection("Partscrafts")
                                .whereEqualTo("shopId", shopId)
                                .whereEqualTo("manageInventory", true)
                                .get()
                                .await()

                            val allParts = querySnapshot.documents.mapNotNull { doc ->
                                doc.toObject(PartsCraft::class.java)?.apply {
                                    id = doc.id
                                }
                            }

                            // Separate into out of stock and low stock
                            outOfStockItems.clear()
                            lowStockItems.clear()

                            allParts.forEach { part ->
                                when {
                                    part.quantity == 0 -> outOfStockItems.add(part)
                                    part.quantity > 0 && part.quantity < part.lowStockLimit -> lowStockItems.add(part)
                                }
                            }

                            // Update UI
                            updateUI()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUI() {
        // Update adapters
        outOfStockAdapter.notifyDataSetChanged()
        lowStockAdapter.notifyDataSetChanged()

        // Show/hide sections based on data
        binding.outOfStockSection.visibility = if (outOfStockItems.isEmpty()) View.GONE else View.VISIBLE
        binding.lowStockSection.visibility = if (lowStockItems.isEmpty()) View.GONE else View.VISIBLE
        
        // Show empty state if both lists are empty
        binding.emptyState.visibility = if (outOfStockItems.isEmpty() && lowStockItems.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}

