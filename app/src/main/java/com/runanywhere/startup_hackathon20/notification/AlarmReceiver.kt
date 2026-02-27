package com.runanywhere.startup_hackathon20.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        const val MEDICINE_NAME = "medicine_name"
        const val DOSAGE = "dosage"
        const val INSTRUCTIONS = "instructions"
        const val NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received!")
        
        val medicineName = intent.getStringExtra(MEDICINE_NAME) ?: "Medicine"
        val dosage = intent.getStringExtra(DOSAGE) ?: "As prescribed"
        val instructions = intent.getStringExtra(INSTRUCTIONS) ?: ""
        val notificationId = intent.getIntExtra(NOTIFICATION_ID, 1)

        Log.d(TAG, "Sending notification for: $medicineName")
        
        // Send the notification
        NotificationHelper.sendMedicineReminder(
            context,
            medicineName,
            dosage,
            instructions,
            notificationId
        )
    }
}
