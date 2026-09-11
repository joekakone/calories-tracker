package com.example.caloriestracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvLogin = view.findViewById<TextView>(R.id.tvLogin)
        
        btnRegister.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
        
        tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
        
        return view
    }
}
