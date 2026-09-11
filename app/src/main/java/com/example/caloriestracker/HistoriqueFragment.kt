package com.example.caloriestracker

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoriqueFragment : Fragment() {

    private lateinit var tvDate: TextView
    private lateinit var tvTotalKcal: TextView
    private lateinit var tvGreeting: TextView
    private lateinit var llMealsContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_historique, container, false)
        
        tvDate = view.findViewById(R.id.tvDate)
        tvTotalKcal = view.findViewById(R.id.tvTotalKcal)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        llMealsContainer = view.findViewById(R.id.llMealsContainer)

        loadHistory()
        return view
    }

    private fun loadHistory() {
        val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val user = db.userDao().getUserById(userId)
            val meals = db.mealDao().getMealsForUser(userId)

            val totalCals = meals.sumOf { it.calories }

            withContext(Dispatchers.Main) {
                user?.let {
                    tvGreeting.text = "Bonjour, ${it.name} !"
                }
                
                val sdf = SimpleDateFormat("dd MMM.", Locale.getDefault())
                tvDate.text = "Aujourd'hui, ${sdf.format(Date())}"
                tvTotalKcal.text = "$totalCals\nkcal"

                llMealsContainer.removeAllViews()
                meals.forEach { meal ->
                    val mealView = layoutInflater.inflate(R.layout.item_meal, llMealsContainer, false)
                    
                    val tvMealName = mealView.findViewById<TextView>(R.id.tvMealName)
                    val tvMealTypeAndMacros = mealView.findViewById<TextView>(R.id.tvMealTypeAndMacros)
                    val tvMealCalories = mealView.findViewById<TextView>(R.id.tvMealCalories)
                    
                    tvMealName.text = meal.name
                    tvMealTypeAndMacros.text = "${meal.mealType} • P:${meal.protein}g G:${meal.carbs}g L:${meal.fat}g"
                    tvMealCalories.text = "${meal.calories} kcal"

                    mealView.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle(meal.name)
                            .setMessage("Macros complets :\nCalories : ${meal.calories} kcal\nProtéines : ${meal.protein}g\nGlucides : ${meal.carbs}g\nLipides : ${meal.fat}g")
                            .setPositiveButton("Fermer", null)
                            .show()
                    }
                    
                    llMealsContainer.addView(mealView)
                }
            }
        }
    }
}
