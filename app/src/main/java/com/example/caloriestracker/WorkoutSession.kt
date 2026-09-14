package com.example.caloriestracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "Marche" ou "Course"
    val name: String, // Nom de l'exercice
    val userId: Int, // ID de l'utilisateur
    val dateMs: Long,
    val durationMs: Long,
    val distanceMeters: Float,
    val steps: Int,
    val caloriesBurned: Int,
    val routePointsJson: String // Serialized List of GeoPoints
)
