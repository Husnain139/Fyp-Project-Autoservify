package com.hstan.autoservify.model.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.hstan.autoservify.model.AppUser
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun logout(): Result<Boolean> {
        FirebaseAuth.getInstance().signOut()
        return Result.success(true)
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        try {
            println("AuthRepository: Starting login for email: $email")
            val result = FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                println("AuthRepository: Login successful, user: ${user.uid}")
                return Result.success(user)
            } else {
                println("AuthRepository: Login result was null")
                return Result.failure(Exception("Authentication failed - user is null"))
            }
        } catch (e: Exception) {
            println("AuthRepository: Login exception: ${e.message}")
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    suspend fun signup(email: String, password: String, name: String): Result<FirebaseUser> {
        try {
            val result = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            result.user?.updateProfile(profileUpdates)?.await()
            return Result.success(result.user!!)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Boolean> {
        try {
            val result = FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            return Result.success(true)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun getCurrentUserEmail(): String? {
        return FirebaseAuth.getInstance().currentUser?.email
    }

    suspend fun saveUserProfile(user: AppUser): Result<Boolean> {
        return try {
            db.collection("users").document(user.uid).set(user).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<AppUser> {
        return try {
            println("AuthRepository: Getting user profile for UID: $uid")
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val user = doc.toObject(AppUser::class.java) ?: AppUser()
                println("AuthRepository: User profile found - Type: ${user.userType}")
                Result.success(user)
            } else {
                println("AuthRepository: User profile document does not exist")
                // Create a default customer profile for users without profile
                val defaultUser = AppUser(uid = uid, userType = "customer")
                Result.success(defaultUser)
            }
        } catch (e: Exception) {
            println("AuthRepository: Error getting user profile: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateUserShopId(uid: String, shopId: String): Result<Boolean> {
        return try {
            db.collection("users").document(uid)
                .update("shopId", shopId).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Boolean> {
        return try {
            FirebaseAuth.getInstance().currentUser?.updatePassword(newPassword)?.await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfileImage(uid: String, imageUrl: String): Result<Boolean> {
        return try {
            db.collection("users").document(uid)
                .update("profileImageUrl", imageUrl)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFCMToken(uid: String, fcmToken: String): Result<Boolean> {
        return try {
            db.collection("users").document(uid)
                .update("fcmToken", fcmToken)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}