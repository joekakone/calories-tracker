package com.example.caloriestracker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
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
        
        val logoContainer = view.findViewById<View>(R.id.logoContainer)
        val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)

        // Animation du logo (Fade + Scale)
        val fadeLogo = ObjectAnimator.ofFloat(logoContainer, "alpha", 0f, 1f).apply { duration = 800 }
        val scaleXLogo = ObjectAnimator.ofFloat(logoContainer, "scaleX", 0.5f, 1f).apply { duration = 800 }
        val scaleYLogo = ObjectAnimator.ofFloat(logoContainer, "scaleY", 0.5f, 1f).apply { duration = 800 }
        val logoAnim = AnimatorSet().apply { playTogether(fadeLogo, scaleXLogo, scaleYLogo) }
        
        // Animation du titre (Fade + Slide)
        val fadeTitle = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f).apply { duration = 600 }
        val slideTitle = ObjectAnimator.ofFloat(tvAppName, "translationY", 30f, 0f).apply { duration = 600 }
        val titleAnim = AnimatorSet().apply { playTogether(fadeTitle, slideTitle) }
        
        // Animation du sous-titre (Fade + Slide)
        val fadeSubtitle = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f).apply { duration = 600 }
        val slideSubtitle = ObjectAnimator.ofFloat(tvSubtitle, "translationY", 30f, 0f).apply { duration = 600 }
        val subtitleAnim = AnimatorSet().apply { playTogether(fadeSubtitle, slideSubtitle) }

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(logoAnim, titleAnim, subtitleAnim)
        animatorSet.start()

        // Transition vers Login ou Dashboard après animation
        lifecycleScope.launch {
            delay(3500) // Attendre la fin de l'animation + un peu de temps
            
            val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val userId = sharedPref.getInt("user_id", -1)
            val lastActiveTime = sharedPref.getLong("last_active_time", 0L)
            val currentTime = System.currentTimeMillis()
            val thirtyMinutesInMillis = 30 * 60 * 1000L

            if (userId != -1 && (currentTime - lastActiveTime) < thirtyMinutesInMillis) {
                // Session valide
                findNavController().navigate(R.id.action_splash_to_dashboard)
            } else {
                // Session invalide ou expirée
                sharedPref.edit().clear().apply()
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }

        return view
    }
}
