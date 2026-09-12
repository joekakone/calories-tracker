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

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvLogin = view.findViewById<TextView>(R.id.tvLogin)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        
        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(context, "Format d'email invalide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password.length < 8 || !password.any { it.isDigit() } || !password.any { it.isUpperCase() }) {
                Toast.makeText(context, "Le mot de passe doit contenir min. 8 caractères, 1 chiffre et 1 majuscule", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            // Insert user in Room
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(requireContext())
                val existingUser = db.userDao().getUserByEmail(email)
                
                if (existingUser != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Cet email est déjà utilisé.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val newUser = UserEntity(
                    name = name,
                    email = email,
                    passwordHash = password // in a real app, hash this!
                )
                
                val userId = db.userDao().insertUser(newUser)
                
                // Save session
                val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putInt("user_id", userId.toInt())
                    apply()
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Inscription réussie !", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_register_to_login) 
                }
            }
        }
        
        tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
        
        return view
    }
}
