package com.example.caloriestracker

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val tvRegister = view.findViewById<TextView>(R.id.tvRegister)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "Format d'email invalide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check Room for user
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(requireContext())
                val user = db.userDao().getUserByEmail(email)
                
                if (user == null || user.passwordHash != password) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // Save session
                val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putInt("user_id", user.id)
                    putLong("last_active_time", System.currentTimeMillis())
                    apply()
                }
                
                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_login_to_dashboard)
                }
            }
        }
        
        tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
        
        tvForgotPassword.setOnClickListener {
            Toast.makeText(context, "Un email de réinitialisation vous a été envoyé.", Toast.LENGTH_LONG).show()
        }
        
        return view
    }
}
