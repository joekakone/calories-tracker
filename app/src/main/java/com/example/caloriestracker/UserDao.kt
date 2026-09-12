package com.example.caloriestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("UPDATE users SET profilePictureUri = :uri WHERE id = :userId")
    suspend fun updateProfilePicture(userId: Int, uri: String?)

    @Query("UPDATE users SET name = :newName WHERE id = :userId")
    suspend fun updateName(userId: Int, newName: String)

    @Query("UPDATE users SET passwordHash = :newPassword WHERE id = :userId")
    suspend fun updatePassword(userId: Int, newPassword: String)

    @Query("UPDATE users SET dailyCalorieGoal = :newGoal WHERE id = :userId")
    suspend fun updateDailyGoal(userId: Int, newGoal: Int)
}
