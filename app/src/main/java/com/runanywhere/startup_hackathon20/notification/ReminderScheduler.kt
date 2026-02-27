package com.runanywhere.startup_hackathon20.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun scheduleMedicineReminder(
        context: Context,
        medicineId: Long,
        medicineName: String,
        dosage: String,
        instructions: String,
        timeOfDay: String,
        frequency: String
    ) {
        Log.d(TAG, "Scheduling reminder for: $medicineName at $timeOfDay")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Parse time (format: "HH:mm")
        val timeParts = timeOfDay.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        // Create calendar for alarm
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // If time has passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Create intent with medicine data
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.MEDICINE_NAME, medicineName)
            putExtra(AlarmReceiver.DOSAGE, dosage)
            putExtra(AlarmReceiver.INSTRUCTIONS, instructions)
            putExtra(AlarmReceiver.NOTIFICATION_ID, medicineId.toInt())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Set repeating alarm based on frequency
            when (frequency.lowercase()) {
                "once a day", "daily" -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Set one-time alarm for $medicineName at ${calendar.time}")
                }
                "twice a day", "every 12 hours" -> {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_HALF_DAY,
                        pendingIntent
                    )
                    Log.d(TAG, "Set 12-hourly alarm for $medicineName")
                }
                "three times a day", "every 8 hours" -> {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        8 * 60 * 60 * 1000L, // 8 hours in milliseconds
                        pendingIntent
                    )
                    Log.d(TAG, "Set 8-hourly alarm for $medicineName")
                }
                "four times a day", "every 6 hours" -> {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        6 * 60 * 60 * 1000L, // 6 hours in milliseconds
                        pendingIntent
                    )
                    Log.d(TAG, "Set 6-hourly alarm for $medicineName")
                }
                else -> {
                    // Default: once daily
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Set default daily alarm for $medicineName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}", e)
        }
    }

    fun cancelMedicineReminder(context: Context, medicineId: Long) {
        Log.d(TAG, "Cancelling reminder for medicine ID: $medicineId")
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Alarm cancelled for medicine ID: $medicineId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alarm: ${e.message}", e)
        }
    }
}
