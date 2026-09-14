package com.example.caloriestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NotificationDao {
    @Insert
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getNotificationsForUser(userId: Int): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    suspend fun getUnreadCount(userId: Int): Int

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: Int)

    @Update
    suspend fun updateNotification(notification: NotificationEntity)
    
    @Query("SELECT * FROM notifications WHERE id = :notifId LIMIT 1")
    suspend fun getNotificationById(notifId: Int): NotificationEntity?
}
