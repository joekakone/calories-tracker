package com.example.caloriestracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkoutTrackingService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_WORKOUT_TYPE = "EXTRA_WORKOUT_TYPE"

        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "workout_tracking_channel"

        private val _workoutMetrics = MutableStateFlow(WorkoutMetrics())
        val workoutMetrics: StateFlow<WorkoutMetrics> = _workoutMetrics
        
        fun resetMetrics() {
            _workoutMetrics.value = WorkoutMetrics()
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private var initialStepCount = -1
    private var currentSteps = 0

    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0f
    private val routePoints = mutableListOf<Location>()

    private var startTime = 0L
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var workoutType = "Marche"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    if (lastLocation != null) {
                        totalDistanceMeters += lastLocation!!.distanceTo(location)
                    }
                    lastLocation = location
                    routePoints.add(location)
                    updateMetrics()
                }
            }
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START -> {
                    workoutType = it.getStringExtra(EXTRA_WORKOUT_TYPE) ?: "Marche"
                    startTracking()
                }
                ACTION_STOP -> {
                    stopTracking()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startTracking() {
        // Reset state
        initialStepCount = -1
        currentSteps = 0
        totalDistanceMeters = 0f
        lastLocation = null
        routePoints.clear()
        
        _workoutMetrics.value = WorkoutMetrics(type = workoutType)

        startForegroundService()

        // Start sensors
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Start GPS
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(2000)
            .setMinUpdateDistanceMeters(2f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // Permission missing
        }

        // Start timer
        startTime = System.currentTimeMillis()
        timerJob = serviceScope.launch {
            while (true) {
                delay(1000)
                updateMetrics()
            }
        }
    }

    private fun stopTracking() {
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        
        // Save to DB? Can be done in ViewModel or here. 
        // For simplicity, we'll let the ViewModel handle the save by listening to final metrics when STOP is clicked.
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundService() {
        val notification = createNotification("00:00", 0f, 0, 0)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateMetrics() {
        val elapsedMs = System.currentTimeMillis() - startTime
        
        // Estimate calories. Simplification: 0.05 kcal per step for walking, 0.07 for running.
        // Or based on distance: 60 kcal / km for walking, 75 kcal / km for running.
        val calPerKm = if (workoutType == "Course") 75f else 60f
        val calories = ((totalDistanceMeters / 1000f) * calPerKm).toInt()
        
        val metrics = WorkoutMetrics(
            type = workoutType,
            durationMs = elapsedMs,
            distanceMeters = totalDistanceMeters,
            steps = currentSteps,
            caloriesBurned = calories,
            route = routePoints.toList()
        )
        
        _workoutMetrics.value = metrics
        
        // Update notification occasionally to avoid spamming
        if (elapsedMs % 5000 < 1000) {
            val h = (elapsedMs / 3600000).toInt()
            val m = ((elapsedMs % 3600000) / 60000).toInt()
            val s = ((elapsedMs % 60000) / 1000).toInt()
            val timeStr = if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
            
            val notification = createNotification(timeStr, totalDistanceMeters, currentSteps, calories)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(time: String, dist: Float, steps: Int, cal: Int): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Séance de $workoutType")
            .setContentText("⏱ $time | 📏 ${String.format("%.2f", dist / 1000f)} km | 👟 $steps pas | 🔥 $cal kcal")
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Suivi d'activité",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification active pendant l'enregistrement d'une séance"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount < 0) {
                initialStepCount = event.values[0].toInt()
            }
            currentSteps = event.values[0].toInt() - initialStepCount
            updateMetrics()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    override fun onBind(intent: Intent?): IBinder? = null
}

data class WorkoutMetrics(
    val type: String = "Marche",
    val durationMs: Long = 0,
    val distanceMeters: Float = 0f,
    val steps: Int = 0,
    val caloriesBurned: Int = 0,
    val route: List<Location> = emptyList()
)
