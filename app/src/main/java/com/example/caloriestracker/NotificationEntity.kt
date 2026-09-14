package com.example.caloriestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: String,
    val isRead: Boolean = false,
    val isActioned: Boolean = false
)
