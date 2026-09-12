package com.example.caloriestracker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_splash, container, false)
        
        val ivLogo = view.findViewById<ImageView>(R.id.ivLogo)
        val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
        val tvSlogan = view.findViewById<TextView>(R.id.tvSlogan)

        // Animation de fondu (Fade in)
        val fadeInLogo = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f).apply { duration = 1000 }
        val fadeInTitle = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f).apply { duration = 1000 }
        val fadeInSlogan = ObjectAnimator.ofFloat(tvSlogan, "alpha", 0f, 1f).apply { duration = 1000 }

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(fadeInLogo, fadeInTitle, fadeInSlogan)
        animatorSet.start()

        // Transition vers Login après animation
        lifecycleScope.launch {
            delay(3500) // Attendre la fin de l'animation + un peu de temps
            findNavController().navigate(R.id.navigation_login)
        }

        return view
    }
}
