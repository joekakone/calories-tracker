package com.example.caloriestracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ID = "calories_tracker_reminders"
    private const val CHANNEL_NAME = "Rappels"
    
    const val NOTIFICATION_ID_WATER = 1001
    const val NOTIFICATION_ID_WALK = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Canal pour les rappels d'eau et de marche"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showWaterReminder(context: Context) {
        // Intent for the "Coché" action
        val checkIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_WATER_CHECKED"
        }
        val checkPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, checkIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon) 
            .setContentTitle("💧 C'est l'heure de boire !")
            .setContentText("Pensez à bien vous hydrater. N'oubliez pas de valider.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "✔️ Coché", checkPendingIntent)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_WATER, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted, ignore or log
            }
        }
    }

    fun showWalkReminder(context: Context) {
        // Intent for the "C'est fait" action
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_WALK_DONE"
        }
        val donePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle("🚶 C'est l'heure de marcher !")
            .setContentText("Prenez une petite pause pour marcher un peu.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, "👟 C'est fait", donePendingIntent)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_WALK, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }
}
