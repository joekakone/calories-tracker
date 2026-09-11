package com.example.caloriestracker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    private lateinit var tvGoal: TextView
    private lateinit var tvConsumed: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRemaining: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvCarbsValue: TextView
    private lateinit var tvProteinsValue: TextView
    private lateinit var tvFatValue: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        
        tvGoal = view.findViewById(R.id.tvGoal)
        tvConsumed = view.findViewById(R.id.tvConsumed)
        progressBar = view.findViewById(R.id.progressBar)
        tvRemaining = view.findViewById(R.id.tvRemaining)
        tvPercent = view.findViewById(R.id.tvPercent)
        tvCarbsValue = view.findViewById(R.id.tvCarbsValue)
        tvProteinsValue = view.findViewById(R.id.tvProteinsValue)
        tvFatValue = view.findViewById(R.id.tvFatValue)

        loadDashboardData()

        return view
    }

    private fun loadDashboardData() {
        val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val user = db.userDao().getUserById(userId)
            val meals = db.mealDao().getMealsForUser(userId)

            val goal = user?.dailyCalorieGoal ?: 2100
            var consumedCalories = 0
            var consumedCarbs = 0
            var consumedProteins = 0
            var consumedFat = 0

            meals.forEach { meal ->
                consumedCalories += meal.calories
                consumedCarbs += meal.carbs
                consumedProteins += meal.protein
                consumedFat += meal.fat
            }

            val remaining = goal - consumedCalories
            val percent = if (goal > 0) (consumedCalories * 100) / goal else 0

            withContext(Dispatchers.Main) {
                tvGoal.text = "Objectif : $goal kcal"
                tvConsumed.text = consumedCalories.toString()
                progressBar.progress = percent
                tvRemaining.text = "Reste : ${if (remaining > 0) remaining else 0} kcal"
                tvPercent.text = "$percent%"
                tvCarbsValue.text = "${consumedCarbs}g"
                tvProteinsValue.text = "${consumedProteins}g"
                tvFatValue.text = "${consumedFat}g"
            }
        }
    }
}
