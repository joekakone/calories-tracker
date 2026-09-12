package com.example.caloriestracker

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MealDetailFragment : Fragment() {

    private lateinit var ivMealBackground: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvMealType: TextView
    private lateinit var tvMealName: TextView
    private lateinit var tvServings: TextView
    private lateinit var tvTotalCalories: TextView
    
    private lateinit var tvCarbsValue: TextView
    private lateinit var tvProteinValue: TextView
    private lateinit var tvFatValue: TextView
    
    private lateinit var llIngredientsContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_meal_detail, container, false)
        
        ivMealBackground = view.findViewById(R.id.ivMealBackground)
        btnBack = view.findViewById(R.id.btnBack)
        tvMealType = view.findViewById(R.id.tvMealType)
        tvMealName = view.findViewById(R.id.tvMealName)
        tvServings = view.findViewById(R.id.tvServings)
        tvTotalCalories = view.findViewById(R.id.tvTotalCalories)
        
        tvCarbsValue = view.findViewById(R.id.tvCarbsValue)
        tvProteinValue = view.findViewById(R.id.tvProteinValue)
        tvFatValue = view.findViewById(R.id.tvFatValue)
        
        llIngredientsContainer = view.findViewById(R.id.llIngredientsContainer)
        
        btnBack.setOnClickListener { findNavController().navigate(R.id.navigation_dashboard) }
        
        val mealId = arguments?.getInt("mealId", -1) ?: -1
        
        if (mealId != -1) {
            loadMealDetails(mealId)
        }

        return view
    }
    
    private fun loadMealDetails(mealId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val meal = db.mealDao().getMealById(mealId)
            
            meal?.let { loadedMeal ->
                withContext(Dispatchers.Main) {
                    tvMealName.text = loadedMeal.name
                    tvMealType.text = loadedMeal.mealType
                    tvTotalCalories.text = "${loadedMeal.calories} Kcal"
                    tvCarbsValue.text = "${loadedMeal.carbs}gr"
                    tvProteinValue.text = "${loadedMeal.protein}g"
                    tvFatValue.text = "${loadedMeal.fat}g"
                    
                    val tvHealthBadge = view?.findViewById<TextView>(R.id.tvHealthBadge)
                    tvHealthBadge?.text = loadedMeal.healthBadge ?: ""
                    if (loadedMeal.healthBadge == "Bonne bouffe") {
                        tvHealthBadge?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        tvHealthBadge?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                    } else if (loadedMeal.healthBadge == "Mal bouffe") {
                        tvHealthBadge?.setTextColor(android.graphics.Color.parseColor("#F44336"))
                        tvHealthBadge?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFEBEE"))
                    } else {
                        tvHealthBadge?.visibility = View.GONE
                    }
                    
                    if (loadedMeal.imageUri != null) {
                        val uri = Uri.parse(loadedMeal.imageUri)
                        val file = File(uri.path!!)
                        if (file.exists()) {
                            var bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            try {
                                val exif = android.media.ExifInterface(file.absolutePath)
                                val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)
                                val matrix = android.graphics.Matrix()
                                when (orientation) {
                                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                                }
                                if (orientation != android.media.ExifInterface.ORIENTATION_NORMAL) {
                                    bitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                }
                            } catch (e: Exception) {}
                            ivMealBackground.setImageBitmap(bitmap)
                        }
                    }
                    
                    // Parse ingredients
                    if (!loadedMeal.ingredients.isNullOrEmpty() && loadedMeal.ingredients != "[]") {
                        try {
                            val ingredientsArray = JSONArray(loadedMeal.ingredients)
                            llIngredientsContainer.removeAllViews()
                            for (i in 0 until ingredientsArray.length()) {
                                val ingObj = ingredientsArray.getJSONObject(i)
                                val ingName = ingObj.optString("name", "Ingrédient")
                                val ingCals = ingObj.optInt("calories", 0)
                                val ingCarbs = ingObj.optInt("carbs", 0)
                                val ingProtein = ingObj.optInt("protein", 0)
                                val ingFat = ingObj.optInt("fat", 0)
                                val ingAmount = ingObj.optString("amount", "100g")
                                
                                val ingView = layoutInflater.inflate(R.layout.item_ingredient, llIngredientsContainer, false)
                                ingView.findViewById<TextView>(R.id.tvIngName).text = ingName
                                ingView.findViewById<TextView>(R.id.tvIngAmount).text = ingAmount
                                ingView.findViewById<TextView>(R.id.tvIngCalories).text = "$ingCals Kcal"
                                ingView.findViewById<TextView>(R.id.tvIngCarbs).text = "${ingCarbs}gr"
                                ingView.findViewById<TextView>(R.id.tvIngProtein).text = "${ingProtein}gr"
                                ingView.findViewById<TextView>(R.id.tvIngFat).text = "${ingFat}gr"
                                
                                llIngredientsContainer.addView(ingView)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        llIngredientsContainer.removeAllViews()
                        val tv = TextView(requireContext()).apply {
                            text = "Aucun ingrédient détecté."
                            setPadding(32, 32, 32, 32)
                        }
                        llIngredientsContainer.addView(tv)
                    }
                }
            }
        }
    }
}
