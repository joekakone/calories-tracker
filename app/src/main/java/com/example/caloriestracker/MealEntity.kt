package com.example.caloriestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val name: String,
    val mealType: String, // e.g. "Petit déjeuner", "Déjeuner", "Dîner", "Snack"
    val calories: Int,
    val carbs: Int = 0,
    val protein: Int = 0,
    val fat: Int = 0,
    val dateTimestamp: Long = System.currentTimeMillis()
)
