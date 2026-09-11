package com.example.caloriestracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Context
import android.widget.Button
import android.widget.EditText

import androidx.navigation.fragment.findNavController

class ScannerFragment : Fragment() {

    private lateinit var viewFinder: PreviewView
    private lateinit var tvHeader: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var scannerOverlay: View
    private lateinit var btnToggleBarcode: CardView
    private lateinit var btnCapture: ImageView
    private lateinit var tvCaptureInstruction: TextView
    private lateinit var cardNutrition: CardView
    private lateinit var tvCalories: TextView
    private lateinit var etMealName: EditText
    private lateinit var btnAddMeal: Button
    private lateinit var cameraExecutor: ExecutorService
    
    private var isBarcodeMode = false
    private var imageCapture: ImageCapture? = null
    private var lastScannedBarcode: String? = null
    private var scannedMeal: MealEntity? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Permissions requises pour utiliser la caméra", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scanner, container, false)
        viewFinder = view.findViewById(R.id.viewFinder)
        tvHeader = view.findViewById(R.id.tvHeader)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)
        scannerOverlay = view.findViewById(R.id.scannerOverlay)
        btnToggleBarcode = view.findViewById(R.id.btnToggleBarcode)
        btnCapture = view.findViewById(R.id.btnCapture)
        tvCaptureInstruction = view.findViewById(R.id.tvCaptureInstruction)
        cardNutrition = view.findViewById(R.id.cardNutrition)
        tvCalories = view.findViewById(R.id.tvCalories)
        etMealName = view.findViewById(R.id.etMealName)
        btnAddMeal = view.findViewById(R.id.btnAddMeal)
        
        cameraExecutor = Executors.newSingleThreadExecutor()

        btnToggleBarcode.setOnClickListener {
            isBarcodeMode = !isBarcodeMode
            updateUIMode()
            startCamera() // Restart use cases
        }
        
        btnCapture.setOnClickListener {
            if (!isBarcodeMode) {
                // Simulate photo capture and analysis
                cardNutrition.visibility = View.VISIBLE
                etMealName.setText("Plat Inconnu")
                tvCalories.text = "Analyse IA...\n450 cal estimées"
                val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                val userId = sharedPref.getInt("user_id", -1)
                scannedMeal = MealEntity(userId = userId, name = "Plat Inconnu", mealType = "Déjeuner", calories = 450)
            }
        }

        btnAddMeal.setOnClickListener {
            scannedMeal?.let { meal ->
                val finalName = etMealName.text.toString()
                val updatedMeal = meal.copy(name = if(finalName.isNotBlank()) finalName else meal.name)
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    db.mealDao().insertMeal(updatedMeal)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Repas ajouté !", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.navigation_dashboard)
                    }
                }
            }
        }

        updateUIMode()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        return view
    }
    
    private fun updateUIMode() {
        if (isBarcodeMode) {
            tvHeader.text = "Scanner Code-barres"
            tvSubtitle.text = "Placez le code-barres dans le cadre"
            scannerOverlay.visibility = View.VISIBLE
            btnCapture.visibility = View.GONE
            tvCaptureInstruction.visibility = View.GONE
        } else {
            tvHeader.text = "Ajouter un Plat"
            tvSubtitle.text = "Visez un plat pour\nl'analyse photo..."
            scannerOverlay.visibility = View.GONE
            btnCapture.visibility = View.VISIBLE
            tvCaptureInstruction.visibility = View.VISIBLE
        }
        cardNutrition.visibility = View.GONE
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                
                if (isBarcodeMode) {
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodeValue ->
                                if (barcodeValue == lastScannedBarcode) return@BarcodeAnalyzer
                                lastScannedBarcode = barcodeValue

                                activity?.runOnUiThread {
                                    tvCalories.text = "Recherche Code: $barcodeValue..."
                                    cardNutrition.visibility = View.VISIBLE
                                }

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val response = ApiClient.openFoodFactsApi.getProductByBarcode(barcodeValue)
                                        if (response.isSuccessful) {
                                            val product = response.body()?.product
                                            val productName = product?.productName ?: "Produit Inconnu"
                                            val cals = product?.nutriments?.calories100g?.toInt() ?: 0
                                            val protein = product?.nutriments?.proteins100g?.toInt() ?: 0
                                            val carbs = product?.nutriments?.carbs100g?.toInt() ?: 0
                                            val fat = product?.nutriments?.fat100g?.toInt() ?: 0

                                            val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                                            val userId = sharedPref.getInt("user_id", -1)
                                            scannedMeal = MealEntity(userId = userId, name = productName, mealType = "Snack", calories = cals, protein = protein, carbs = carbs, fat = fat)

                                            withContext(Dispatchers.Main) {
                                                etMealName.setText(productName)
                                                tvCalories.text = "$cals cal (100g) | P:$protein g | G:$carbs g"
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                tvCalories.text = "Code: $barcodeValue\nProduit non trouvé"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            tvCalories.text = "Erreur réseau"
                                        }
                                    }
                                }
                            })
                        }
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
                } else {
                    imageCapture = ImageCapture.Builder().build()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                }
                
            } catch (exc: Exception) {
                Log.e("ScannerFragment", "Camera binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { onBarcodeDetected(it) }
                    }
                }
                .addOnFailureListener {
                    // Ignore errors
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
