package com.hstan.autoservify.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.hstan.autoservify.model.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel: ViewModel() {

    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> get() = _currentUser

    private val _userName = MutableStateFlow<String>("")
    val userName: StateFlow<String> get() = _userName

    val failureMessage = MutableStateFlow<String?>(null)
    val resetResponse = MutableStateFlow<Boolean?>(null)

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                println("AuthViewModel: Attempting login for email: $email")
                val result = authRepository.login(email, password)
                if (result.isSuccess) {
                    val user = result.getOrThrow()
                    println("AuthViewModel: Login successful for user: ${user.uid}")
                    _currentUser.value = user
                    fetchUserName()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown login error"
                    println("AuthViewModel: Login failed with error: $error")
                    failureMessage.value = error
                }
            } catch (e: Exception) {
                println("AuthViewModel: Login exception: ${e.message}")
                e.printStackTrace()
                failureMessage.value = "Login failed: ${e.message}"
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            if (result.isSuccess) {
                resetResponse.value = result.getOrThrow()
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch {
            val result = authRepository.signup(email, password, name)
            if (result.isSuccess) {
                _currentUser.value = result.getOrThrow()
                _userName.value = name  // Store name after signup
                fetchUserName()
            } else {
                failureMessage.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun checkUser() {
        val user = authRepository.getCurrentUser()
        println("AuthViewModel: checkUser() - Found user: ${user?.uid}")
        _currentUser.value = user
        if (user != null) {
            fetchUserName()
        }
    }

    private fun fetchUserName() {
        val user = _currentUser.value
        if (user != null) {
            _userName.value = user.displayName ?: "Unknown"
        }
    }

    // 🆕 Clear user state (for debugging)
    fun clearUserState() {
        _currentUser.value = null
        _userName.value = ""
        failureMessage.value = null
    }
}

