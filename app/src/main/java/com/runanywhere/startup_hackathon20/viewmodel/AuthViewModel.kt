package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.startup_hackathon20.database.MedicineDatabase
import com.runanywhere.startup_hackathon20.database.UserRepository
import com.runanywhere.startup_hackathon20.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context = application.applicationContext
    private val repository: UserRepository

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess

    init {
        val database = MedicineDatabase.getDatabase(application)
        val userDao = database.userDao()
        repository = UserRepository(userDao)

        // Create notification channel
        NotificationHelper.createNotificationChannel(context)
        
        checkExistingUser()
    }

    private fun checkExistingUser() {
        viewModelScope.launch {
            val user = repository.getLoggedInUserSync()
            if (user != null) {
                _authSuccess.value = true
            }
        }
    }

    fun register(name: String, username: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when {
                name.isBlank() -> {
                    _errorMessage.value = "Name cannot be empty"
                }

                username.isBlank() -> {
                    _errorMessage.value = "Username cannot be empty"
                }

                password.isBlank() -> {
                    _errorMessage.value = "Password cannot be empty"
                }

                password != confirmPassword -> {
                    _errorMessage.value = "Passwords do not match"
                }

                password.length < 4 -> {
                    _errorMessage.value = "Password must be at least 4 characters"
                }

                else -> {
                    try {
                        repository.registerUser(name, username, password)
                        _authSuccess.value = true
                        
                        // Note: Welcome notification will be sent after permission is granted
                        // from the UI layer (authscreen.kt)
                    } catch (e: Exception) {
                        _errorMessage.value = "Registration failed: ${e.message}"
                    }
                }
            }

            _isLoading.value = false
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when {
                username.isBlank() -> {
                    _errorMessage.value = "Username cannot be empty"
                }

                password.isBlank() -> {
                    _errorMessage.value = "Password cannot be empty"
                }

                else -> {
                    try {
                        val success = repository.loginUser(username, password)
                        if (success) {
                            _authSuccess.value = true
                        } else {
                            _errorMessage.value = "Invalid username or password"
                        }
                    } catch (e: Exception) {
                        _errorMessage.value = "Login failed: ${e.message}"
                    }
                }
            }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
    
    // Public function to send welcome notification (called after permission is granted)
    fun sendWelcomeNotificationNow(userName: String) {
        sendWelcomeNotification(userName)
    }
    
    private fun sendWelcomeNotification(userName: String) {
        // Send a welcome notification to the user's device
        NotificationHelper.sendWelcomeNotification(
            context = context,
            userName = userName
        )
    }
}
