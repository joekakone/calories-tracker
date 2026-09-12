package com.example.caloriestracker

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class MealSummaryFragment : Fragment() {

    private lateinit var ivMealImage: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var etMealName: EditText
    private lateinit var tvCalories: TextView
    private lateinit var tvProtein: TextView
    private lateinit var tvCarbs: TextView
    private lateinit var tvFat: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvBadge: TextView
    private lateinit var btnAddMeal: Button

    private var imageUriStr: String? = null
    
    // Result data
    private var caloriesResult = 0
    private var proteinResult = 0
    private var carbsResult = 0
    private var fatResult = 0
    private var badgeResult = "Inconnu"
    private var ingredientsJsonStr = "[]"
    
    private var scannedMeal: MealEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_meal_summary, container, false)
        
        ivMealImage = view.findViewById(R.id.ivMealImage)
        btnBack = view.findViewById(R.id.btnBack)
        etMealName = view.findViewById(R.id.etMealName)
        tvCalories = view.findViewById(R.id.tvCalories)
        tvProtein = view.findViewById(R.id.tvProtein)
        tvCarbs = view.findViewById(R.id.tvCarbs)
        tvFat = view.findViewById(R.id.tvFat)
        tvDescription = view.findViewById(R.id.tvDescription)
        tvBadge = view.findViewById(R.id.tvBadge)
        btnAddMeal = view.findViewById(R.id.btnAddMeal)

        imageUriStr = arguments?.getString("imageUri")

        btnBack.setOnClickListener { findNavController().popBackStack() }

        if (imageUriStr != null) {
            val uri = Uri.parse(imageUriStr)
            val file = File(uri.path!!)
            if (file.exists()) {
                var bitmap = BitmapFactory.decodeFile(file.absolutePath)
                
                // Fix EXIF orientation
                try {
                    val exif = android.media.ExifInterface(file.absolutePath)
                    val orientation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = android.graphics.Matrix()
                    when (orientation) {
                        android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    if (orientation != android.media.ExifInterface.ORIENTATION_NORMAL && orientation != android.media.ExifInterface.ORIENTATION_UNDEFINED) {
                        bitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                ivMealImage.setImageBitmap(bitmap)
                
                // Call Gemini API
                analyzeImageWithGemini(bitmap)
            }
        }

        btnAddMeal.setOnClickListener {
            saveMeal()
        }

        return view
    }

    private fun analyzeImageWithGemini(bitmap: android.graphics.Bitmap) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "votre_cle_api_ici") {
            Toast.makeText(requireContext(), "Veuillez configurer votre clé API Gemini dans local.properties", Toast.LENGTH_LONG).show()
            tvDescription.text = "Erreur: Clé API manquante."
            return
        }

        val generativeModel = GenerativeModel(
            modelName = "gemini-flash-latest",
            apiKey = apiKey
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prompt = """
                    Analyse cette image de nourriture et renvoie UNIQUEMENT un objet JSON valide avec les clés suivantes :
                    - "name": le nom du plat détecté (court).
                    - "calories": entier estimé pour la portion visible.
                    - "protein": entier (en grammes).
                    - "carbs": entier (en grammes).
                    - "fat": entier (en grammes).
                    - "description": une courte description appétissante en français.
                    - "healthBadge": soit "Bonne bouffe" soit "Mal bouffe".
                    - "ingredients": un tableau d'objets, chaque objet ayant les clés "name" (nom de l'ingrédient), "calories", "protein", "carbs", "fat" (tous entiers), et "amount" (chaîne, ex: "100gr").
                    Ne renvoie aucun texte avant ou après le JSON.
                """.trimIndent()

                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )

                var responseText = response.text ?: ""
                if (responseText.contains("{") && responseText.contains("}")) {
                    responseText = responseText.substring(responseText.indexOf("{"), responseText.lastIndexOf("}") + 1)
                }
                
                if (responseText.isNotEmpty()) {
                    val json = JSONObject(responseText)
                    val name = json.optString("name", "Plat Inconnu")
                    caloriesResult = json.optInt("calories", 0)
                    proteinResult = json.optInt("protein", json.optInt("proteins", 0))
                    carbsResult = json.optInt("carbs", 0)
                    fatResult = json.optInt("fat", json.optInt("fats", 0))
                    val description = json.optString("description", "")
                    badgeResult = json.optString("healthBadge", json.optString("badge", "Inconnu"))
                    
                    val ingredientsArray = json.optJSONArray("ingredients")
                    ingredientsJsonStr = ingredientsArray?.toString() ?: "[]"

                    val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    val userId = sharedPref.getInt("user_id", -1)
                    scannedMeal = MealEntity(
                        userId = userId,
                        name = name,
                        mealType = "Snack",
                        calories = caloriesResult,
                        protein = proteinResult,
                        carbs = carbsResult,
                        fat = fatResult,
                        imageUri = imageUriStr,
                        healthBadge = badgeResult,
                        ingredients = ingredientsJsonStr
                    )

                    withContext(Dispatchers.Main) {
                        etMealName.setText(name)
                        tvCalories.text = "$caloriesResult kcal"
                        tvProtein.text = "Protéines: ${proteinResult}g"
                        tvCarbs.text = "Glucides: ${carbsResult}g"
                        tvFat.text = "Lipides: ${fatResult}g"
                        tvDescription.text = description
                        tvBadge.text = badgeResult
                        
                        if (badgeResult == "Bonne bouffe") {
                            tvBadge.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        } else {
                            tvBadge.setTextColor(android.graphics.Color.parseColor("#F44336"))
                        }
                        
                        btnAddMeal.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erreur lors de l'analyse IA", Toast.LENGTH_SHORT).show()
                    tvDescription.text = "Impossible d'analyser l'image."
                }
            }
        }
    }

    private fun saveMeal() {
        val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        
        if (userId != -1) {
            val meal = scannedMeal ?: MealEntity(
                userId = userId,
                name = etMealName.text.toString(),
                mealType = "Déjeuner", // Default for now
                calories = caloriesResult,
                protein = proteinResult,
                carbs = carbsResult,
                fat = fatResult,
                imageUri = imageUriStr,
                healthBadge = badgeResult,
                ingredients = ingredientsJsonStr
            )
            
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(requireContext())
                val mealId = db.mealDao().insertMeal(meal).toInt()
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Repas ajouté !", Toast.LENGTH_SHORT).show()
                    val bundle = Bundle().apply {
                        putInt("mealId", mealId)
                    }
                    findNavController().navigate(R.id.action_summary_to_detail, bundle)
                }
            }
        }
    }
}
