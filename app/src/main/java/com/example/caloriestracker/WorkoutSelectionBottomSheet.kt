package com.example.caloriestracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WorkoutSelectionBottomSheet(private val onSelect: (String) -> Unit) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_workout_selection, container, false)
        
        view.findViewById<LinearLayout>(R.id.btnWalk).setOnClickListener {
            onSelect("Marche")
            dismiss()
        }
        
        view.findViewById<LinearLayout>(R.id.btnRun).setOnClickListener {
            onSelect("Course")
            dismiss()
        }

        return view
    }
}
