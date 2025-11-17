package com.example.fitmatch.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fitmatch.viewmodel.ProgressViewModel
import com.google.firebase.auth.FirebaseAuth

class ProgressViewModelFactory(
    private val repo: ProgressRepository,
    private val auth: FirebaseAuth
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            return ProgressViewModel(repo, auth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    }
}
