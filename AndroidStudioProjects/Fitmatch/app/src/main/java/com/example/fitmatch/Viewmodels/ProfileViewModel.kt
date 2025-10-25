package com.example.fitmatch.Viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserProfile(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val workouts: Int = 0,
    val streak: Int = 0,
    val goals: Int = 0,
    val totalTime: String = "0h",
    val profileImageUrl: String? = null
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile

    fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                _userProfile.value = UserProfile(
                    fullName = doc.getString("fullName") ?: "",
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    workouts = doc.getLong("workouts")?.toInt() ?: 0,
                    streak = doc.getLong("streak")?.toInt() ?: 0,
                    goals = doc.getLong("goals")?.toInt() ?: 0,
                    totalTime = doc.getString("totalTime") ?: "0h",
                    profileImageUrl = doc.getString("profileImageUrl")
                )
            }
        }
    }

    fun ensureUserDoc() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val user = auth.currentUser

        val defaults = hashMapOf(
            "fullName" to (user?.displayName ?: "User"),
            "email" to (user?.email ?: ""),
            "phone" to "",
            "workouts" to 0,
            "streak" to 0,
            "goals" to 0,
            "totalTime" to "0h",
            "profileImageUrl" to ""
        )

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                db.collection("users").document(uid).set(defaults)
                    .addOnSuccessListener { fetchUserProfile() }
            } else {
                fetchUserProfile()
            }
        }
    }


    fun updateField(field: String, value: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(field, value)
            .addOnSuccessListener {
                _userProfile.value = when (field) {
                    "fullName" -> _userProfile.value.copy(fullName = value)
                    "email" -> _userProfile.value.copy(email = value)
                    "phone" -> _userProfile.value.copy(phone = value)
                    else -> _userProfile.value
                }
            }
    }

    fun uploadProfileImage(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        val ref = storage.reference.child("profileImages/$uid/profile.jpg")
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                db.collection("users").document(uid)
                    .update("profileImageUrl", url.toString())
                _userProfile.value = _userProfile.value.copy(profileImageUrl = url.toString())
            }
        }
    }

    fun logout(onLogout: () -> Unit) {
        auth.signOut()
        onLogout()
    }
}
