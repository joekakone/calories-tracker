package com.example.caloriestracker

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    
    private val requestPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        NotificationHelper.createNotificationChannel(this)
        AlarmScheduler.scheduleAlarms(this)


        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        
        bottomNav.setupWithNavController(navController)
        
        fab.setOnClickListener {
            if (navController.currentDestination?.id != R.id.navigation_scanner) {
                navController.navigate(R.id.navigation_scanner)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_splash,
                R.id.navigation_login,
                R.id.navigation_register,
                R.id.navigation_scanner,
                R.id.navigation_meal_summary,
                R.id.navigation_meal_detail,
                R.id.navigation_workout_map,
                R.id.navigation_notifications,
                R.id.workoutDetailsFragment -> {
                    bottomAppBar.visibility = View.GONE
                    fab.visibility = View.GONE
                }
                else -> {
                    bottomAppBar.visibility = View.VISIBLE
                    fab.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId != -1) {
            with(sharedPref.edit()) {
                putLong("last_active_time", System.currentTimeMillis())
                apply()
            }
        }
    }
}
