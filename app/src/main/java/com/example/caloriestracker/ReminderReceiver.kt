package com.example.caloriestracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        when (intent.action) {
            "ACTION_WATER_REMINDER" -> {
                saveNotification(context, userId, "💧 C'est l'heure de boire !", "Pensez à bien vous hydrater. N'oubliez pas de valider.", "WATER")
                NotificationHelper.showWaterReminder(context)
            }
            "ACTION_WALK_REMINDER" -> {
                saveNotification(context, userId, "🚶 C'est l'heure de marcher !", "Prenez une petite pause pour marcher un peu.", "WALK")
                NotificationHelper.showWalkReminder(context)
            }
        }
    }

    private fun saveNotification(context: Context, userId: Int, title: String, desc: String, type: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            db.notificationDao().insertNotification(
                NotificationEntity(
                    userId = userId,
                    title = title,
                    description = desc,
                    timestamp = System.currentTimeMillis(),
                    type = type
                )
            )
        }
    }
}
