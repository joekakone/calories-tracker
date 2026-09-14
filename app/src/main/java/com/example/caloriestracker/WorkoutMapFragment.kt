package com.example.caloriestracker

import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class WorkoutMapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var tvDuration: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var btnStartStop: Button
    
    private var isTracking = false
    private var workoutType = "Marche"
    
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private val routePolyline = Polyline()
    
    // Pour sauvegarder la séance
    private var currentDurationMs: Long = 0
    private var currentDistanceMeters: Float = 0f
    private var currentSteps: Int = 0
    private var currentCalories: Int = 0
    private var currentRoutePoints: List<android.location.Location> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = "JoekakoneCaloriesTracker/1.0 (contact@joekakone.com)"
        
        // Retrieve arguments if passed via Navigation
        // For simplicity, defaulting to "Marche" if not found
        arguments?.let {
            workoutType = it.getString("workoutType") ?: "Marche"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_workout_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        mapView = view.findViewById(R.id.mapView)
        tvDuration = view.findViewById(R.id.tvDuration)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvSteps = view.findViewById(R.id.tvSteps)
        tvCalories = view.findViewById(R.id.tvCalories)
        btnStartStop = view.findViewById(R.id.btnStartStop)
        
        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        checkAndRequestPermissions()
        setupListeners()
        observeService()
    }
    
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            setupMap()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(missingPermissions.toTypedArray())
        } else {
            setupMap()
        }
    }

    private fun setupMap() {
        val customTileSource = org.osmdroid.tileprovider.tilesource.XYTileSource(
            "Mapnik_Fresh",
            0, 19, 256, ".png", arrayOf(
                "https://a.tile.openstreetmap.org/",
                "https://b.tile.openstreetmap.org/",
                "https://c.tile.openstreetmap.org/"
            )
        )
        mapView.setTileSource(customTileSource)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)
        
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView)
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        mapView.overlays.add(myLocationOverlay)
        
        routePolyline.outlinePaint.color = Color.parseColor("#10B981") // Green
        routePolyline.outlinePaint.strokeWidth = 10f
        mapView.overlays.add(routePolyline)
    }
    
    private fun setupListeners() {
        btnStartStop.setOnClickListener {
            if (isTracking) {
                stopTracking()
            } else {
                startTracking()
            }
        }
    }
    
    private fun startTracking() {
        isTracking = true
        btnStartStop.text = "Terminer"
        btnStartStop.setBackgroundColor(Color.parseColor("#EF4444")) // Red
        
        val serviceIntent = Intent(requireContext(), WorkoutTrackingService::class.java).apply {
            action = WorkoutTrackingService.ACTION_START
            putExtra(WorkoutTrackingService.EXTRA_WORKOUT_TYPE, workoutType)
        }
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }
    
    private fun stopTracking() {
        isTracking = false
        btnStartStop.text = "Commencer la séance"
        btnStartStop.setBackgroundColor(Color.parseColor("#000000")) // Black
        
        val serviceIntent = Intent(requireContext(), WorkoutTrackingService::class.java).apply {
            action = WorkoutTrackingService.ACTION_STOP
        }
        requireContext().startService(serviceIntent)
        
        promptWorkoutNameAndSave()
    }

    private fun promptWorkoutNameAndSave() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_workout, null)
        val etWorkoutName = dialogView.findViewById<android.widget.EditText>(R.id.etWorkoutName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        
        etWorkoutName.setText(workoutType)
        etWorkoutName.hint = "Ex: $workoutType du matin"

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etWorkoutName.text.toString().takeIf { it.isNotBlank() } ?: workoutType
            saveWorkoutToDb(name)
            dialog.dismiss()
        }

        dialog.show()
    }
    
    private fun saveWorkoutToDb(name: String) {
        val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return
        
        // Convert route to simple json
        val routeJsonBuilder = java.lang.StringBuilder("[")
        currentRoutePoints.forEachIndexed { index, loc ->
            routeJsonBuilder.append("{\"lat\":${loc.latitude},\"lon\":${loc.longitude}}")
            if (index < currentRoutePoints.size - 1) routeJsonBuilder.append(",")
        }
        routeJsonBuilder.append("]")
        
        val session = WorkoutSession(
            type = workoutType,
            name = name,
            userId = userId,
            dateMs = System.currentTimeMillis(),
            durationMs = currentDurationMs,
            distanceMeters = currentDistanceMeters,
            steps = currentSteps,
            caloriesBurned = currentCalories,
            routePointsJson = routeJsonBuilder.toString()
        )
        
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            db.workoutDao().insertWorkout(session)
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(requireContext(), "Séance sauvegardée", android.widget.Toast.LENGTH_SHORT).show()
                WorkoutTrackingService.resetMetrics()
                findNavController().popBackStack()
            }
        }
    }
    
    private fun observeService() {
        viewLifecycleOwner.lifecycleScope.launch {
            WorkoutTrackingService.workoutMetrics.collect { metrics ->
                // Update local variables
                currentDurationMs = metrics.durationMs
                currentDistanceMeters = metrics.distanceMeters
                currentSteps = metrics.steps
                currentCalories = metrics.caloriesBurned
                currentRoutePoints = metrics.route
                
                // Update UI
                val h = (metrics.durationMs / 3600000).toInt()
                val m = ((metrics.durationMs % 3600000) / 60000).toInt()
                val s = ((metrics.durationMs % 60000) / 1000).toInt()
                tvDuration.text = if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
                
                tvDistance.text = String.format("%.2f", metrics.distanceMeters / 1000f)
                tvSteps.text = metrics.steps.toString()
                tvCalories.text = metrics.caloriesBurned.toString()
                
                // Update Polyline
                val geoPoints = metrics.route.map { GeoPoint(it.latitude, it.longitude) }
                routePolyline.setPoints(geoPoints)
                mapView.invalidate()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
