package com.example.caloriestracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY dateMs DESC")
    suspend fun getWorkoutsForUser(userId: Int): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions ORDER BY dateMs DESC")
    suspend fun getAllWorkouts(): List<WorkoutSession>
    
    @Query("SELECT * FROM workout_sessions WHERE id = :workoutId")
    suspend fun getWorkoutById(workoutId: Long): WorkoutSession?
}
