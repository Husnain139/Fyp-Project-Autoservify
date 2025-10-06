package com.hstan.autoservify.ui.main.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hstan.autoservify.ui.auth.LoginActivity

/**
 * Legacy MainActivity - now redirects to LoginActivity
 * This activity is kept for backward compatibility but should not be used as launcher
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirect to LoginActivity (which handles authentication and routing)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
