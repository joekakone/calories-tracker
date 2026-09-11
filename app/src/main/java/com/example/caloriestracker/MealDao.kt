package com.example.caloriestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealEntity): Long

    @Query("SELECT * FROM meals WHERE userId = :userId ORDER BY dateTimestamp DESC")
    suspend fun getMealsForUser(userId: Int): List<MealEntity>

    // Get meals for a specific day could be implemented by filtering timestamps, but for now we'll fetch all or limit
    @Query("SELECT SUM(calories) FROM meals WHERE userId = :userId")
    suspend fun getTotalCaloriesForUser(userId: Int): Int?
}
