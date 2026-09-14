package com.example.caloriestracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.core.content.res.ResourcesCompat

class ExercicesFragment : Fragment() {
    private lateinit var rvWorkouts: RecyclerView
    private var workouts: List<WorkoutSession> = emptyList()
    private var currentFilter = "Tout"
    
    private lateinit var tabTout: TextView
    private lateinit var tabMarche: TextView
    private lateinit var tabCourse: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_exercices, container, false)
        
        val btnStartSession = view.findViewById<ImageView>(R.id.btnStartSession)
        rvWorkouts = view.findViewById(R.id.rvWorkouts)
        tabTout = view.findViewById(R.id.tabTout)
        tabMarche = view.findViewById(R.id.tabMarche)
        tabCourse = view.findViewById(R.id.tabCourse)
        
        rvWorkouts.layoutManager = LinearLayoutManager(requireContext())
        
        btnStartSession.setOnClickListener {
            val bottomSheet = WorkoutSelectionBottomSheet { selectedType ->
                val bundle = Bundle().apply {
                    putString("workoutType", selectedType)
                }
                findNavController().navigate(R.id.action_exercices_to_workout_map, bundle)
            }
            bottomSheet.show(childFragmentManager, "WorkoutSelectionBottomSheet")
        }
        
        tabTout.setOnClickListener { setFilter("Tout") }
        tabMarche.setOnClickListener { setFilter("Marche") }
        tabCourse.setOnClickListener { setFilter("Course") }
        
        loadWorkouts()
        
        return view
    }
    
    private fun loadWorkouts() {
        val sharedPref = requireActivity().getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            workouts = db.workoutDao().getWorkoutsForUser(userId)
            
            withContext(Dispatchers.Main) {
                setFilter(currentFilter)
            }
        }
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        
        updateTabUI(tabTout, filter == "Tout")
        updateTabUI(tabMarche, filter == "Marche")
        updateTabUI(tabCourse, filter == "Course")
        
        val filtered = if (filter == "Tout") {
            workouts
        } else {
            workouts.filter { it.type.equals(filter, ignoreCase = true) }
        }
        
        rvWorkouts.adapter = WorkoutAdapter(filtered) { workout ->
            val bundle = Bundle().apply {
                putLong("workoutId", workout.id)
            }
            findNavController().navigate(R.id.action_exercices_to_workout_details, bundle)
        }
    }

    private fun updateTabUI(tab: TextView, isSelected: Boolean) {
        if (isSelected) {
            tab.setBackgroundResource(R.drawable.bg_pill)
            tab.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.primary_blue))
            tab.setTextColor(android.graphics.Color.WHITE)
            tab.typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_bold)
        } else {
            tab.background = null
            tab.setTextColor(requireContext().getColor(R.color.text_subtitle))
            tab.typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins)
        }
    }
    
    private inner class WorkoutAdapter(
        private val items: List<WorkoutSession>,
        private val onItemClick: (WorkoutSession) -> Unit
    ) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvWorkoutTitle)
            val tvDetails: TextView = view.findViewById(R.id.tvWorkoutDetails)
            val tvCalories: TextView = view.findViewById(R.id.tvCalories)
            val iconWorkout: ImageView = view.findViewById(R.id.iconWorkout)

            init {
                view.setOnClickListener {
                    onItemClick(items[adapterPosition])
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_workout_layout, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.name
            
            val m = (item.durationMs / 60000).toInt()
            val s = ((item.durationMs % 60000) / 1000).toInt()
            val distanceKm = item.distanceMeters / 1000f
            
            holder.tvDetails.text = String.format("%02d:%02d min • %d pas • %.2f km", m, s, item.steps, distanceKm)
            holder.tvCalories.text = "-${item.caloriesBurned}\nkcal"
            
            if (item.type == "Course") {
                holder.iconWorkout.setImageResource(R.drawable.ic_run_icon)
            } else {
                holder.iconWorkout.setImageResource(R.drawable.ic_walk_icon)
            }
        }

        override fun getItemCount() = items.size
    }
}
