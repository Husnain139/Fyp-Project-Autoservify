package com.hstan.autoservify.ui.shopkeeper

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.hstan.autoservify.R
import com.hstan.autoservify.ui.auth.LoginActivity
import com.hstan.autoservify.ui.main.ViewModels.MainViewModel
import kotlinx.coroutines.launch

class ShopkeeperMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopkeeper_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        val imageView = findViewById<ImageView>(R.id.drawer_icon)
        imageView.setOnClickListener {
            if (drawer.isDrawerVisible(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
            } else {
                drawer.openDrawer(GravityCompat.START)
            }
        }

        // Profile icon click listener
        val profileIcon = findViewById<ImageView>(R.id.right_icon)
        profileIcon.setOnClickListener {
            navigateToFragment(R.id.item_profile)
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            ?: return

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        
        // Setup navigation controller with bottom navigation
        bottomNavigationView.setupWithNavController(navHostFragment.navController)
        
        // Setup custom navigation for shopkeepers
        setupShopkeeperNavigation(bottomNavigationView, navHostFragment)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        when (item.itemId) {
            R.id.home -> {
                navigateToFragment(R.id.item_home)
            }
            R.id.orders -> {
                navigateToFragment(R.id.item_cart)
            }
            R.id.profile -> {
                navigateToFragment(R.id.item_profile)
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

    private fun navigateToFragment(fragmentId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.navigate(fragmentId)
    }

    private fun showLogoutConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Are you sure you want to log out?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    viewModel.logout()
                    startActivity(Intent(this@ShopkeeperMainActivity, LoginActivity::class.java))
                    finish()
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val alert = dialogBuilder.create()
        alert.show()
    }

    override fun onResume() {
        super.onResume()
        // Ensure Home is selected when returning from activities
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        
        if (navHostFragment != null) {
            val currentDestination = navHostFragment.navController.currentDestination?.id
            when (currentDestination) {
                R.id.item_home -> bottomNavigationView.selectedItemId = R.id.item_home
                R.id.item_manage_parts -> bottomNavigationView.selectedItemId = R.id.item_manage_parts
                R.id.item_manage_services -> bottomNavigationView.selectedItemId = R.id.item_manage_services
                R.id.item_cart -> bottomNavigationView.selectedItemId = R.id.item_cart
                R.id.item_profile -> bottomNavigationView.selectedItemId = R.id.item_profile
            }
        }
    }

    private fun setupShopkeeperNavigation(bottomNavigationView: BottomNavigationView, navHostFragment: NavHostFragment) {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_home -> {
                    // Dashboard - navigate to home fragment
                    navHostFragment.navController.navigate(R.id.item_home)
                    true
                }
                R.id.item_manage_parts -> {
                    // Manage Spare Parts - navigate to parts fragment
                    navHostFragment.navController.navigate(R.id.item_manage_parts)
                    true
                }
                R.id.item_manage_services -> {
                    // Manage Services - navigate to services fragment
                    navHostFragment.navController.navigate(R.id.item_manage_services)
                    true
                }
                R.id.item_cart -> {
                    // Orders - navigate to orders fragment
                    navHostFragment.navController.navigate(R.id.item_cart)
                    true
                }
                R.id.item_profile -> {
                    // Profile
                    navHostFragment.navController.navigate(R.id.item_profile)
                    true
                }
                else -> false
            }
        }
    }
}
