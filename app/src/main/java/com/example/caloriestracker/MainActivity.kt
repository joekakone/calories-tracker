package com.example.caloriestracker

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                R.id.navigation_register -> {
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
}
