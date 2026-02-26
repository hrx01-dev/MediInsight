package com.runanywhere.startup_hackathon20.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.startup_hackathon20.database.MedicineDatabase
import com.runanywhere.startup_hackathon20.database.MedicineEntity
import com.runanywhere.startup_hackathon20.database.MedicineRepository
import com.runanywhere.startup_hackathon20.database.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class MedicationReminder(
    val id: Long,
    val medicineName: String,
    val dosage: String,
    val time: String,
    val instructions: String,
    val remainingDoses: Int,
    val createdAt: Long
)

data class DailyTip(
    val title: String,
    val description: String,
    val category: String
)

sealed class NotificationItem {
    data class Reminder(val reminder: MedicationReminder) : NotificationItem()
    data class Tip(val tip: DailyTip) : NotificationItem()
}

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val medicineRepository: MedicineRepository
    private val userRepository: UserRepository

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications

    private val _currentUserId = MutableStateFlow<Long?>(null)

    init {
        val database = MedicineDatabase.getDatabase(application)
        val medicineDao = database.medicineDao()
        val userDao = database.userDao()
        medicineRepository = MedicineRepository(medicineDao)
        userRepository = UserRepository(userDao)

        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepository.loggedInUser.collectLatest { user ->
                _currentUserId.value = user?.id
                user?.id?.let { loadNotifications(it) }
            }
        }
    }

    private fun loadNotifications(userId: Long) {
        viewModelScope.launch {
            medicineRepository.getMedicinesByUser(userId).collectLatest { medicines ->
                val notificationsList = mutableListOf<NotificationItem>()

                // Add daily tip at the top
                notificationsList.add(NotificationItem.Tip(getDailyTip()))

                // Generate medication reminders
                medicines.forEach { medicine ->
                    val reminders = generateMedicationReminders(medicine)
                    reminders.forEach { reminder ->
                        notificationsList.add(NotificationItem.Reminder(reminder))
                    }
                }

                // Add additional health tips
                if (medicines.isEmpty()) {
                    // If no medicines, show more tips
                    notificationsList.add(NotificationItem.Tip(getHealthTip(1)))
                    notificationsList.add(NotificationItem.Tip(getHealthTip(2)))
                } else {
                    // Add one more tip
                    notificationsList.add(NotificationItem.Tip(getHealthTip(0)))
                }

                _notifications.value = notificationsList
            }
        }
    }

    private fun generateMedicationReminders(medicine: MedicineEntity): List<MedicationReminder> {
        val reminders = mutableListOf<MedicationReminder>()
        val frequency = extractFrequency(medicine.frequency)
        
        // Parse duration to calculate remaining doses
        val totalDays = parseDuration(medicine.duration)
        val elapsedDays = ((System.currentTimeMillis() - medicine.createdAt) / (1000 * 60 * 60 * 24)).toInt()
        val remainingDays = maxOf(0, totalDays - elapsedDays)
        val remainingDoses = remainingDays * frequency

        // Generate reminders based on frequency
        val times = generateReminderTimes(medicine.time, frequency)
        
        times.forEach { time ->
            reminders.add(
                MedicationReminder(
                    id = medicine.id,
                    medicineName = medicine.name,
                    dosage = medicine.dosage,
                    time = time,
                    instructions = medicine.instructions,
                    remainingDoses = remainingDoses,
                    createdAt = medicine.createdAt
                )
            )
        }

        return reminders
    }

    private fun extractFrequency(frequencyText: String): Int {
        // Extract number from frequency text (e.g., "3 times a day" -> 3)
        val numbers = frequencyText.filter { it.isDigit() }
        return if (numbers.isNotEmpty()) numbers.toIntOrNull() ?: 1 else 1
    }

    private fun parseDuration(durationText: String): Int {
        // Parse duration to days (e.g., "7 days" -> 7, "2 weeks" -> 14)
        val lowerText = durationText.lowercase()
        val numbers = lowerText.filter { it.isDigit() }
        val value = if (numbers.isNotEmpty()) numbers.toIntOrNull() ?: 7 else 7

        return when {
            lowerText.contains("week") -> value * 7
            lowerText.contains("month") -> value * 30
            lowerText.contains("year") -> value * 365
            else -> value // assume days
        }
    }

    private fun generateReminderTimes(timeText: String, frequency: Int): List<String> {
        val times = mutableListOf<String>()
        val lowerTime = timeText.lowercase()

        when {
            lowerTime.contains("morning") || lowerTime.contains("breakfast") -> {
                times.add("8:00 AM - Morning")
                if (frequency >= 2) times.add("1:00 PM - Afternoon")
                if (frequency >= 3) times.add("8:00 PM - Night")
                if (frequency >= 4) times.add("11:00 PM - Before bed")
            }
            lowerTime.contains("afternoon") || lowerTime.contains("lunch") -> {
                times.add("1:00 PM - Afternoon")
                if (frequency >= 2) times.add("8:00 PM - Night")
                if (frequency >= 3) times.add("8:00 AM - Morning")
            }
            lowerTime.contains("evening") || lowerTime.contains("dinner") -> {
                times.add("7:00 PM - Evening")
                if (frequency >= 2) times.add("8:00 AM - Morning")
                if (frequency >= 3) times.add("1:00 PM - Afternoon")
            }
            lowerTime.contains("night") || lowerTime.contains("bed") -> {
                times.add("10:00 PM - Bedtime")
                if (frequency >= 2) times.add("8:00 AM - Morning")
                if (frequency >= 3) times.add("2:00 PM - Afternoon")
            }
            lowerTime.contains("meal") -> {
                times.add("8:00 AM - After breakfast")
                if (frequency >= 2) times.add("1:00 PM - After lunch")
                if (frequency >= 3) times.add("8:00 PM - After dinner")
            }
            lowerTime.contains("before meal") -> {
                times.add("7:30 AM - Before breakfast")
                if (frequency >= 2) times.add("12:30 PM - Before lunch")
                if (frequency >= 3) times.add("7:30 PM - Before dinner")
            }
            else -> {
                // Default schedule
                times.add("8:00 AM - Morning")
                if (frequency >= 2) times.add("2:00 PM - Afternoon")
                if (frequency >= 3) times.add("8:00 PM - Evening")
                if (frequency >= 4) times.add("11:00 PM - Night")
            }
        }

        return times.take(frequency)
    }

    private fun getDailyTip(): DailyTip {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val tips = getAllDailyTips()
        return tips[dayOfYear % tips.size]
    }

    private fun getHealthTip(offset: Int): DailyTip {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val tips = getAllHealthTips()
        return tips[(dayOfYear + offset) % tips.size]
    }

    private fun getAllDailyTips(): List<DailyTip> {
        return listOf(
            DailyTip(
                title = "💧 Stay Hydrated",
                description = "Drink at least 8 glasses of water daily. Proper hydration helps medicines work effectively.",
                category = "Wellness"
            ),
            DailyTip(
                title = "⏰ Medication Timing",
                description = "Take medicines at the same time each day for consistent blood levels and better results.",
                category = "Medication"
            ),
            DailyTip(
                title = "🍎 Healthy Diet",
                description = "Eat a balanced diet rich in fruits and vegetables to support your immune system.",
                category = "Nutrition"
            ),
            DailyTip(
                title = "😴 Quality Sleep",
                description = "Get 7-8 hours of sleep. Good sleep helps your body heal and medications work better.",
                category = "Sleep"
            ),
            DailyTip(
                title = "🚶 Daily Exercise",
                description = "30 minutes of moderate exercise daily improves overall health and medication effectiveness.",
                category = "Fitness"
            ),
            DailyTip(
                title = "📅 Check Expiry Dates",
                description = "Review your medicine cabinet monthly and dispose of expired medications safely.",
                category = "Safety"
            ),
            DailyTip(
                title = "❄️ Proper Storage",
                description = "Store medicines in a cool, dry place away from direct sunlight and moisture.",
                category = "Storage"
            ),
            DailyTip(
                title = "👨‍⚕️ Doctor Consultation",
                description = "Never skip follow-up appointments. Regular check-ups ensure treatment effectiveness.",
                category = "Healthcare"
            ),
            DailyTip(
                title = "⚠️ Side Effects",
                description = "Monitor for side effects and report unusual symptoms to your doctor immediately.",
                category = "Safety"
            ),
            DailyTip(
                title = "🚫 Avoid Alcohol",
                description = "Alcohol can interfere with many medications. Avoid it during treatment.",
                category = "Safety"
            ),
            DailyTip(
                title = "📝 Keep Records",
                description = "Maintain a medication diary to track doses, times, and any side effects.",
                category = "Organization"
            ),
            DailyTip(
                title = "🤝 Medication Interactions",
                description = "Always inform your doctor about all medications and supplements you're taking.",
                category = "Safety"
            ),
            DailyTip(
                title = "🧘 Stress Management",
                description = "Practice relaxation techniques. Stress can affect medication absorption and effectiveness.",
                category = "Mental Health"
            ),
            DailyTip(
                title = "🥗 Food Interactions",
                description = "Some foods can affect medicine absorption. Ask your pharmacist about food restrictions.",
                category = "Nutrition"
            ),
            DailyTip(
                title = "✅ Complete the Course",
                description = "Finish prescribed medication courses even if you feel better to prevent resistance.",
                category = "Medication"
            )
        )
    }

    private fun getAllHealthTips(): List<DailyTip> {
        return listOf(
            DailyTip(
                title = "🌡️ Monitor Vital Signs",
                description = "Track your blood pressure and temperature if you're on specific medications.",
                category = "Monitoring"
            ),
            DailyTip(
                title = "🧴 Hand Hygiene",
                description = "Wash hands before handling medications to prevent contamination.",
                category = "Hygiene"
            ),
            DailyTip(
                title = "☀️ Vitamin D",
                description = "Get 15 minutes of sunlight daily for natural Vitamin D, unless contraindicated.",
                category = "Wellness"
            ),
            DailyTip(
                title = "🚰 Take with Water",
                description = "Unless specified otherwise, take medications with a full glass of water.",
                category = "Medication"
            ),
            DailyTip(
                title = "🔔 Set Reminders",
                description = "Use phone alarms or pill organizers to never miss a dose.",
                category = "Organization"
            ),
            DailyTip(
                title = "🏥 Emergency Contacts",
                description = "Keep your doctor's and pharmacy's contact numbers easily accessible.",
                category = "Preparedness"
            ),
            DailyTip(
                title = "🧊 Fever Management",
                description = "For fever above 101°F (38.3°C), consult a doctor before taking additional medication.",
                category = "Health"
            ),
            DailyTip(
                title = "💊 Pill Crushers",
                description = "Never crush or split pills unless your pharmacist says it's safe to do so.",
                category = "Safety"
            ),
            DailyTip(
                title = "🌙 Evening Medications",
                description = "Take sedating medications at night to avoid daytime drowsiness.",
                category = "Timing"
            ),
            DailyTip(
                title = "🔄 Refill on Time",
                description = "Order refills 3-5 days before running out to maintain consistency.",
                category = "Planning"
            )
        )
    }

    fun refreshNotifications() {
        _currentUserId.value?.let { loadNotifications(it) }
    }
}
