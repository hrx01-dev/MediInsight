package com.runanywhere.startup_hackathon20.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.runanywhere.startup_hackathon20.MainActivity
import com.runanywhere.startup_hackathon20.R

object NotificationHelper {
    private const val CHANNEL_ID = "medicine_reminders"
    private const val CHANNEL_NAME = "Medicine Reminders"
    private const val TAG = "NotificationHelper"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Reminders for medicine consumption"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    fun sendMedicineReminder(
        context: Context,
        medicineName: String,
        dosage: String,
        instructions: String,
        notificationId: Int
    ) {
        Log.d(TAG, "Sending medicine reminder: $medicineName")
        
        createNotificationChannel(context)

        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "notifications")
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lightbulb)
            .setContentTitle("Medicine Reminder")
            .setContentText("Time to take $medicineName")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Dosage: $dosage\nInstructions: $instructions"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .setLights(android.graphics.Color.GREEN, 1000, 2000)

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "Notification sent with ID: $notificationId")
    }
}
