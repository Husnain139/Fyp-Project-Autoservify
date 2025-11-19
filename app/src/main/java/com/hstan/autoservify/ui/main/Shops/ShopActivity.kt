package com.hstan.autoservify.ui.main.Shops

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.hstan.autoservify.R
import com.hstan.autoservify.databinding.ActivityShopBinding
import com.hstan.autoservify.ui.auth.LoginActivity
import com.hstan.autoservify.ui.main.Shops.Services.ServicesActivity
import com.hstan.autoservify.ui.main.Shops.SpareParts.PartsCraftActivity

class ShopActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var binding: ActivityShopBinding
    private var currentShop: Shop? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Fill title and description from intent JSON
        intent.getStringExtra("data")?.let { json ->
            val shop = Gson().fromJson(json, Shop::class.java)
            currentShop = shop // Store the shop for later use
            binding.titleInput.text = shop.title
            binding.description.text = shop.description
            
            // Load shop image
            com.bumptech.glide.Glide.with(this)
                .load(shop.imageUrl.ifEmpty { R.drawable.logo })
                .error(R.drawable.logo)
                .placeholder(R.drawable.logo)
                .centerCrop()
                .into(binding.imageView2)
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        val drawerIcon = findViewById<ImageView>(R.id.drawer_icon)
        drawerIcon.setOnClickListener {
            if (drawer.isDrawerVisible(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
            } else {
                drawer.openDrawer(GravityCompat.START)
            }
        }

        // ✅ Open Services page when clicking the card
        binding.servicesCard.setOnClickListener {
            val intent = Intent(this, ServicesActivity::class.java)
            currentShop?.let { shop ->
                intent.putExtra("shop_id", shop.id)
                intent.putExtra("shop_name", shop.title)
            }
            startActivity(intent)
        }

        // ✅ Open Services page when clicking arrow inside card
        binding.ServicePage.setOnClickListener {
            val intent = Intent(this, ServicesActivity::class.java)
            currentShop?.let { shop ->
                intent.putExtra("shop_id", shop.id)
                intent.putExtra("shop_name", shop.title)
            }
            startActivity(intent)
        }

        // ✅ Spare Parts card and arrow
        binding.sparePartsCard.setOnClickListener {
            val intent = Intent(this, PartsCraftActivity::class.java)
            currentShop?.let { shop ->
                intent.putExtra("shop_id", shop.id)
                intent.putExtra("shop_name", shop.title)
            }
            startActivity(intent)
        }


        binding.SparePartsPage.setOnClickListener {
            val intent = Intent(this, PartsCraftActivity::class.java)
            currentShop?.let { shop ->
                intent.putExtra("shop_id", shop.id)
                intent.putExtra("shop_name", shop.title)
            }
            startActivity(intent)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        when (item.itemId) {
            R.id.home -> {
                Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show()
            }
            R.id.orders -> {
                Toast.makeText(this, "Orders selected", Toast.LENGTH_SHORT).show()
            }
            R.id.profile -> {
                Toast.makeText(this, "Profile selected", Toast.LENGTH_SHORT).show()
            }
            R.id.about_us -> {
                Toast.makeText(this, "About Us selected", Toast.LENGTH_SHORT).show()
            }
            R.id.logout -> {
                showLogoutConfirmationDialog()
            }
            else -> {
                Toast.makeText(this, "Invalid option", Toast.LENGTH_SHORT).show()
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to log out?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(this@ShopActivity, LoginActivity::class.java))
                finish()
            }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
