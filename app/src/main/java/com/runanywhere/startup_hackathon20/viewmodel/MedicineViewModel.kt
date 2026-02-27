package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.startup_hackathon20.database.MedicineDatabase
import com.runanywhere.startup_hackathon20.database.MedicineEntity
import com.runanywhere.startup_hackathon20.database.MedicineRepository
import com.runanywhere.startup_hackathon20.database.UserRepository
import com.runanywhere.startup_hackathon20.notification.NotificationHelper
import com.runanywhere.startup_hackathon20.notification.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MedicineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicineRepository
    private val userRepository: UserRepository
    val allMedicines: StateFlow<List<MedicineEntity>>

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUserId = MutableStateFlow<Long?>(null)

    init {
        val database = MedicineDatabase.getDatabase(application)
        val medicineDao = database.medicineDao()
        val userDao = database.userDao()
        repository = MedicineRepository(medicineDao)
        userRepository = UserRepository(userDao)
        
        // Create notification channel
        NotificationHelper.createNotificationChannel(application)

        // Load current user
        viewModelScope.launch {
            userRepository.loggedInUser.collect { user ->
                _currentUserId.value = user?.id
            }
        }

        // Automatically switch to user-specific medicines when user changes
        allMedicines = _currentUserId.flatMapLatest { userId ->
            if (userId != null) {
                repository.getMedicinesByUser(userId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun insertMedicine(
        name: String,
        dosage: String,
        frequency: String,
        time: String,
        duration: String,
        quantity: String,
        instructions: String
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val userId = _currentUserId.value
                if (userId == null) {
                    _error.value = "No user logged in"
                    return@launch
                }

                val medicine = MedicineEntity(
                    userId = userId,
                    name = name,
                    dosage = dosage,
                    frequency = frequency,
                    time = time,
                    duration = duration,
                    quantity = quantity,
                    instructions = instructions
                )
                
                // Insert medicine into database
                val medicineId = repository.insertMedicine(medicine)
                Log.d("MedicineViewModel", "Medicine inserted with ID: $medicineId")
                
                // Schedule reminder alarm
                if (time.isNotBlank() && frequency.isNotBlank()) {
                    ReminderScheduler.scheduleMedicineReminder(
                        getApplication(),
                        medicineId,
                        name,
                        dosage,
                        instructions,
                        time,
                        frequency
                    )
                    Log.d("MedicineViewModel", "Reminder scheduled for: $name")
                }
            } catch (e: Exception) {
                _error.value = "Failed to add medicine: ${e.message}"
                Log.e("MedicineViewModel", "Error inserting medicine: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                repository.updateMedicine(medicine)
            } catch (e: Exception) {
                _error.value = "Failed to update medicine: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMedicine(medicine: MedicineEntity) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Cancel reminder alarm before deleting
                ReminderScheduler.cancelMedicineReminder(getApplication(), medicine.id)
                Log.d("MedicineViewModel", "Reminder cancelled for medicine ID: ${medicine.id}")
                
                repository.deleteMedicine(medicine)
            } catch (e: Exception) {
                _error.value = "Failed to delete medicine: ${e.message}"
                Log.e("MedicineViewModel", "Error deleting medicine: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAllMedicines() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val userId = _currentUserId.value
                if (userId == null) {
                    _error.value = "No user logged in"
                    return@launch
                }

                repository.deleteAllMedicinesByUser(userId)
            } catch (e: Exception) {
                _error.value = "Failed to delete all medicines: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
