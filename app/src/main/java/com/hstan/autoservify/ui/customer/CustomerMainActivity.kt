package com.hstan.autoservify.ui.customer

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

class CustomerMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_main)

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
        bottomNavigationView.setupWithNavController(navHostFragment.navController)
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
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            navHostFragment?.let {
                val navController = it.navController
                // Check if the destination exists before navigating
                if (navController.graph.findNode(fragmentId) != null) {
                    navController.navigate(fragmentId)
                } else {
                    Toast.makeText(this, "Navigation destination not found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Are you sure you want to log out?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    viewModel.logout()
                    startActivity(Intent(this@CustomerMainActivity, LoginActivity::class.java))
                    finish()
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        val alert = dialogBuilder.create()
        alert.show()
    }
}
