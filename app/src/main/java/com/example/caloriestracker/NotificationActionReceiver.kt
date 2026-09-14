package com.example.caloriestracker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        when (intent.action) {
            "ACTION_WATER_CHECKED" -> {
                // Dimiss the water notification
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_WATER)
                Toast.makeText(context, "Super ! Vous avez bu de l'eau 💧 (+200ml)", Toast.LENGTH_SHORT).show()
                
                if (userId != -1) {
                    val currentWater = sharedPref.getFloat("water_intake_$userId", 0f)
                    sharedPref.edit().putFloat("water_intake_$userId", currentWater + 0.2f).apply()
                    markLatestActioned(context, userId, "WATER")
                }
            }
            "ACTION_WALK_DONE" -> {
                // Dimiss the walk notification
                notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_WALK)
                Toast.makeText(context, "Bravo pour cette petite marche 🚶", Toast.LENGTH_SHORT).show()
                if (userId != -1) {
                    markLatestActioned(context, userId, "WALK")
                }
            }
        }
    }
    
    private fun markLatestActioned(context: Context, userId: Int, type: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val notifs = db.notificationDao().getNotificationsForUser(userId)
            val latest = notifs.firstOrNull { it.type == type && !it.isActioned }
            if (latest != null) {
                db.notificationDao().updateNotification(latest.copy(isActioned = true))
            }
        }
    }
}
