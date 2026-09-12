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
import androidx.navigation.fragment.findNavController
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
                    val tvMealType = mealView.findViewById<TextView>(R.id.tvMealType)
                    val tvMealCalories = mealView.findViewById<TextView>(R.id.tvMealCalories)
                    
                    val tvProteinBadge = mealView.findViewById<TextView>(R.id.tvProteinBadge)
                    val tvCarbsBadge = mealView.findViewById<TextView>(R.id.tvCarbsBadge)
                    val tvFatBadge = mealView.findViewById<TextView>(R.id.tvFatBadge)
                    val ivMealImage = mealView.findViewById<android.widget.ImageView>(R.id.ivMealImage)
                    
                    tvMealName.text = meal.name
                    tvMealType.text = meal.mealType
                    tvMealCalories.text = "${meal.calories} kcal"
                    
                    tvProteinBadge.text = "${meal.protein}g"
                    tvCarbsBadge.text = "${meal.carbs}g"
                    tvFatBadge.text = "${meal.fat}g"
                    
                    if (meal.imageUri != null) {
                        try {
                            val uri = android.net.Uri.parse(meal.imageUri)
                            ivMealImage.setImageURI(uri)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    mealView.setOnClickListener {
                        val bundle = Bundle().apply {
                            putInt("mealId", meal.id)
                        }
                        findNavController().navigate(R.id.action_historique_to_detail, bundle)
                    }
                    
                    llMealsContainer.addView(mealView)
                }
            }
        }
    }
}
