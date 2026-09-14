package com.example.caloriestracker

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

class WorkoutDetailsFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var tvWorkoutTitle: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView

    private val routePolyline = Polyline()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = "JoekakoneCaloriesTracker/1.0 (contact@joekakone.com)"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout_details, container, false)
        
        mapView = view.findViewById(R.id.mapView)
        tvWorkoutTitle = view.findViewById(R.id.tvWorkoutTitle)
        tvDuration = view.findViewById(R.id.tvDuration)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvSteps = view.findViewById(R.id.tvSteps)
        tvCalories = view.findViewById(R.id.tvCalories)
        
        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().popBackStack()
        }

        setupMap()
        loadWorkoutData()

        return view
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
        mapView.controller.setZoom(16.0)
        
        routePolyline.outlinePaint.color = Color.parseColor("#10B981") // Green
        routePolyline.outlinePaint.strokeWidth = 10f
        mapView.overlays.add(routePolyline)
    }

    private fun loadWorkoutData() {
        val workoutId = arguments?.getLong("workoutId", -1L) ?: -1L
        if (workoutId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val workout = db.workoutDao().getWorkoutById(workoutId)
            
            withContext(Dispatchers.Main) {
                workout?.let {
                    tvWorkoutTitle.text = it.name
                    
                    val h = (it.durationMs / 3600000).toInt()
                    val m = ((it.durationMs % 3600000) / 60000).toInt()
                    val s = ((it.durationMs % 60000) / 1000).toInt()
                    tvDuration.text = if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
                    
                    tvDistance.text = String.format("%.2f", it.distanceMeters / 1000f)
                    tvSteps.text = it.steps.toString()
                    tvCalories.text = it.caloriesBurned.toString()

                    try {
                        val jsonArray = JSONArray(it.routePointsJson)
                        val geoPoints = mutableListOf<GeoPoint>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            geoPoints.add(GeoPoint(obj.getDouble("lat"), obj.getDouble("lon")))
                        }
                        
                        if (geoPoints.isNotEmpty()) {
                            routePolyline.setPoints(geoPoints)
                            mapView.controller.setCenter(geoPoints.first())
                            mapView.invalidate()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
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
