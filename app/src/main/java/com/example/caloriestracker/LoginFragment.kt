package com.example.caloriestracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val tvRegister = view.findViewById<TextView>(R.id.tvRegister)
        
        btnLogin.setOnClickListener {
            // Bypass login for now and go to dashboard
            findNavController().navigate(R.id.action_login_to_dashboard)
        }
        
        tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
        
        return view
    }
}
