package com.hstan.autoservify.ui.main.home.Profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.hstan.autoservify.ui.main.home.Profile.MyAccountActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.hstan.autoservify.databinding.FragmentProfileBinding
import com.hstan.autoservify.ui.auth.LoginActivity
import com.hstan.autoservify.ui.shopkeeper.OrdersActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // ✅ Load user info
        firebaseAuth.currentUser?.uid?.let { loadUserProfile(it) }

        // ✅ My Account
        binding.profileHeader.setOnClickListener {
            startActivity(Intent(requireContext(), MyAccountActivity::class.java))
        }
        binding.MyAccount.setOnClickListener {
            startActivity(Intent(requireContext(), MyAccountActivity::class.java))
        }

        // ✅ Orders
        binding.OrderHistory.setOnClickListener {
            startActivity(Intent(requireContext(), OrdersActivity::class.java))
        }

        // ✅ Help Center
        binding.HelpCentre.setOnClickListener {
            Toast.makeText(requireContext(), "Help Center clicked", Toast.LENGTH_SHORT).show()
            //startActivity(Intent(requireContext(), HelpCentreActivity::class.java))
        }

        // ✅ About Us
        binding.AboutUs.setOnClickListener {
            Toast.makeText(requireContext(), "About Us clicked", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(requireContext(), AboutUsActivity::class.java))
        }

        // ✅ Logout
        binding.logout.setOnClickListener {
            showLogoutDialog()
        }

        return binding.root
    }

    private fun loadUserProfile(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    binding.userName.text = document.getString("name") ?: "Unknown User"
                    binding.userPhone.text = document.getString("email") ?: "No Email"
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                firebaseAuth.signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
