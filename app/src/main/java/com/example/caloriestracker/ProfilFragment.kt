package com.example.caloriestracker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfilFragment : Fragment() {
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvLogout: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profil, container, false)
        
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail)
        tvLogout = view.findViewById(R.id.tvLogout)

        val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)

        if (userId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(requireContext())
                val user = db.userDao().getUserById(userId)

                withContext(Dispatchers.Main) {
                    if (user != null) {
                        tvProfileName.text = user.name
                        tvProfileEmail.text = user.email
                    }
                }
            }
        }

        tvLogout.setOnClickListener {
            // Logout
            with(sharedPref.edit()) {
                clear()
                apply()
            }
            findNavController().navigate(R.id.action_profil_to_login) // Need to make sure this action exists or use a global action. I'll just pop to login if possible, or splash.
        }

        return view
    }
}
