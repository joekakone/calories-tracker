package com.example.caloriestracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        loadNotifications(view)
        
        return view
    }

    private fun loadNotifications(view: View) {
        val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        val llNotifications = view.findViewById<android.widget.LinearLayout>(R.id.llNotifications)
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val notifications = db.notificationDao().getNotificationsForUser(userId)
            
            // Mark all as read when opened
            db.notificationDao().markAllAsRead(userId)

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                llNotifications.removeAllViews()
                
                if (notifications.isEmpty()) {
                    val emptyView = android.widget.TextView(requireContext()).apply {
                        text = "Aucune notification pour le moment."
                        textSize = 16f
                        setPadding(0, 32, 0, 0)
                        gravity = android.view.Gravity.CENTER
                    }
                    llNotifications.addView(emptyView)
                } else {
                    notifications.forEach { notif ->
                        val notifView = layoutInflater.inflate(R.layout.item_notification_layout, llNotifications, false)
                        
                        val ivIcon = notifView.findViewById<ImageView>(R.id.ivIcon)
                        val tvTitle = notifView.findViewById<android.widget.TextView>(R.id.tvTitle)
                        val tvDesc = notifView.findViewById<android.widget.TextView>(R.id.tvDesc)
                        val tvTime = notifView.findViewById<android.widget.TextView>(R.id.tvTime)
                        
                        tvTitle.text = notif.title
                        tvDesc.text = notif.description
                        
                        if (notif.type == "WATER") {
                            ivIcon.setImageResource(android.R.drawable.ic_popup_reminder)
                            ivIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E3F2FD"))
                            ivIcon.setColorFilter(android.graphics.Color.parseColor("#2196F3")) // Blue
                        } else {
                            ivIcon.setImageResource(android.R.drawable.ic_menu_directions)
                            ivIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                            ivIcon.setColorFilter(android.graphics.Color.parseColor("#4CAF50")) // Green
                        }
                        
                        // Time formatting
                        val diff = System.currentTimeMillis() - notif.timestamp
                        val minutes = diff / (60 * 1000)
                        val hours = diff / (60 * 60 * 1000)
                        tvTime.text = when {
                            minutes < 1 -> "À l'instant"
                            minutes < 60 -> "Il y a $minutes min"
                            hours < 24 -> "Il y a $hours h"
                            else -> "Il y a ${hours / 24} jours"
                        }
                        
                        llNotifications.addView(notifView)
                    }
                }
            }
        }
    }
}
