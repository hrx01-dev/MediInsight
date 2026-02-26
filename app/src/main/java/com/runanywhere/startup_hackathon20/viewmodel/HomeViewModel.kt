package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.startup_hackathon20.database.ChatMessageRepository
import com.runanywhere.startup_hackathon20.database.MedicineDatabase
import com.runanywhere.startup_hackathon20.database.UserEntity
import com.runanywhere.startup_hackathon20.database.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class InsightData(
    val title: String,
    val description: String,
    val time: String,
    val category: String = "Health"
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository
    private val chatRepository: ChatMessageRepository

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName

    private val _recentInsights = MutableStateFlow<List<InsightData>>(emptyList())
    val recentInsights: StateFlow<List<InsightData>> = _recentInsights

    init {
        val database = MedicineDatabase.getDatabase(application)
        val userDao = database.userDao()
        val chatDao = database.chatMessageDao()
        userRepository = UserRepository(userDao)
        chatRepository = ChatMessageRepository(chatDao)

        loadCurrentUser()
        loadRecentInsights()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.loggedInUser.collectLatest { user ->
                _currentUser.value = user
                _userName.value = user?.name ?: "User"
                // Reload insights when user changes
                user?.id?.let { loadRecentInsights() }
            }
        }
    }

    private fun loadRecentInsights() {
        viewModelScope.launch {
            val userId = _currentUser.value?.id
            if (userId != null) {
                chatRepository.getRecent20MessagesByUser(userId).collectLatest { messages ->
                    // Get user questions only (exclude AI responses)
                    val userQuestions = messages.filter { it.isUser }
                        .takeLast(5) // Get last 5 user questions
                        .reversed() // Most recent first
                    
                    // Generate insights based on questions
                    val insights = if (userQuestions.isNotEmpty()) {
                        generateInsightsFromQuestions(userQuestions.map { it.text to it.timestamp })
                    } else {
                        getDefaultMedicalInsights()
                    }
                    
                    _recentInsights.value = insights
                }
            } else {
                // No user logged in, show default insights
                _recentInsights.value = getDefaultMedicalInsights()
            }
        }
    }

    private fun generateInsightsFromQuestions(questions: List<Pair<String, Long>>): List<InsightData> {
        val insights = mutableListOf<InsightData>()
        
        questions.forEach { (question, timestamp) ->
            val insight = when {
                // Medicine related
                question.contains("paracetamol", ignoreCase = true) || 
                question.contains("pain", ignoreCase = true) ||
                question.contains("fever", ignoreCase = true) -> {
                    InsightData(
                        title = "Pain & Fever Management",
                        description = "Safe dosage and timing for common pain relievers",
                        time = getTimeAgo(timestamp),
                        category = "Medication"
                    )
                }
                
                question.contains("antibiotic", ignoreCase = true) ||
                question.contains("infection", ignoreCase = true) -> {
                    InsightData(
                        title = "Antibiotic Guidelines",
                        description = "Complete the full course even if symptoms improve",
                        time = getTimeAgo(timestamp),
                        category = "Medication"
                    )
                }
                
                question.contains("diabetes", ignoreCase = true) ||
                question.contains("blood sugar", ignoreCase = true) ||
                question.contains("insulin", ignoreCase = true) -> {
                    InsightData(
                        title = "Diabetes Management",
                        description = "Monitor blood sugar levels and maintain regular medication schedule",
                        time = getTimeAgo(timestamp),
                        category = "Chronic Care"
                    )
                }
                
                question.contains("blood pressure", ignoreCase = true) ||
                question.contains("hypertension", ignoreCase = true) -> {
                    InsightData(
                        title = "Blood Pressure Control",
                        description = "Take medications at the same time daily for best results",
                        time = getTimeAgo(timestamp),
                        category = "Chronic Care"
                    )
                }
                
                question.contains("allergy", ignoreCase = true) ||
                question.contains("allergic", ignoreCase = true) ||
                question.contains("antihistamine", ignoreCase = true) -> {
                    InsightData(
                        title = "Allergy Management",
                        description = "Identify triggers and keep emergency medication handy",
                        time = getTimeAgo(timestamp),
                        category = "Allergies"
                    )
                }
                
                question.contains("cold", ignoreCase = true) ||
                question.contains("cough", ignoreCase = true) ||
                question.contains("flu", ignoreCase = true) -> {
                    InsightData(
                        title = "Cold & Flu Care",
                        description = "Rest, hydration, and symptom management strategies",
                        time = getTimeAgo(timestamp),
                        category = "Common Illness"
                    )
                }
                
                question.contains("vitamin", ignoreCase = true) ||
                question.contains("supplement", ignoreCase = true) -> {
                    InsightData(
                        title = "Vitamin & Supplements",
                        description = "Best time to take supplements for optimal absorption",
                        time = getTimeAgo(timestamp),
                        category = "Nutrition"
                    )
                }
                
                question.contains("sleep", ignoreCase = true) ||
                question.contains("insomnia", ignoreCase = true) -> {
                    InsightData(
                        title = "Sleep Quality",
                        description = "Establish a consistent bedtime routine and sleep schedule",
                        time = getTimeAgo(timestamp),
                        category = "Wellness"
                    )
                }
                
                question.contains("headache", ignoreCase = true) ||
                question.contains("migraine", ignoreCase = true) -> {
                    InsightData(
                        title = "Headache Relief",
                        description = "Identify triggers and use appropriate pain management",
                        time = getTimeAgo(timestamp),
                        category = "Pain Management"
                    )
                }
                
                question.contains("stomach", ignoreCase = true) ||
                question.contains("digest", ignoreCase = true) ||
                question.contains("acid", ignoreCase = true) -> {
                    InsightData(
                        title = "Digestive Health",
                        description = "Take medications as directed and maintain healthy eating habits",
                        time = getTimeAgo(timestamp),
                        category = "Digestive"
                    )
                }
                
                else -> {
                    // Generic medical insight based on question
                    InsightData(
                        title = "Medication Safety",
                        description = "Always follow prescribed dosages and timing",
                        time = getTimeAgo(timestamp),
                        category = "General Health"
                    )
                }
            }
            
            insights.add(insight)
        }
        
        // If we have less than 3 insights, add default ones
        while (insights.size < 3) {
            insights.addAll(getDefaultMedicalInsights().take(3 - insights.size))
        }
        
        return insights.take(5) // Return max 5 insights
    }

    private fun getDefaultMedicalInsights(): List<InsightData> {
        return listOf(
            InsightData(
                title = "Medication Adherence",
                description = "Take medicines at the same time daily for better effectiveness",
                time = "General",
                category = "Best Practice"
            ),
            InsightData(
                title = "Stay Hydrated",
                description = "Drink adequate water, especially when taking medications",
                time = "Daily Tip",
                category = "Wellness"
            ),
            InsightData(
                title = "Storage Tips",
                description = "Store medicines in cool, dry place away from sunlight",
                time = "Essential",
                category = "Safety"
            ),
            InsightData(
                title = "Check Expiry Dates",
                description = "Regularly review medicine cabinet for expired medications",
                time = "Monthly",
                category = "Safety"
            ),
            InsightData(
                title = "Consult Your Doctor",
                description = "Never self-medicate for serious conditions",
                time = "Important",
                category = "Best Practice"
            )
        )
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} min ago"
            diff < 86400_000 -> "${diff / 3600_000} hr ago"
            diff < 172800_000 -> "Yesterday"
            diff < 604800_000 -> "${diff / 86400_000} days ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    fun updateUserName(newName: String) {
        viewModelScope.launch {
            _currentUser.value?.let { user ->
                userRepository.updateUserName(user.id, newName)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logoutCurrentUser()
        }
    }

    fun refreshInsights() {
        loadRecentInsights()
    }
}
