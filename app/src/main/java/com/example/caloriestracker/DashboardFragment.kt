package com.example.caloriestracker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class DashboardFragment : Fragment() {

    private lateinit var tvCard1Value: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressDesc: TextView
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvCarbsBadge: TextView
    private lateinit var tvProteinBadge: TextView
    private lateinit var tvFatBadge: TextView
    private lateinit var llRecentActivities: LinearLayout
    private lateinit var lineChart: LineChart
    private lateinit var tvAvatarInitials: TextView
    private lateinit var ivAvatarImage: android.widget.ImageView
    private lateinit var ivNotification: android.widget.ImageView
    private lateinit var tvNotificationBadge: TextView
    private lateinit var tvCard2Value: TextView
    private lateinit var tvCard3Value: TextView
    private lateinit var tvCard4Value: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        
        tvCard1Value = view.findViewById(R.id.tvCard1Value)
        progressBar = view.findViewById(R.id.progressBar)
        tvProgressDesc = view.findViewById(R.id.tvProgressDesc)
        tvProgressPercent = view.findViewById(R.id.tvProgressPercent)
        tvCarbsBadge = view.findViewById(R.id.tvCarbsBadge)
        tvProteinBadge = view.findViewById(R.id.tvProteinBadge)
        tvFatBadge = view.findViewById(R.id.tvFatBadge)
        llRecentActivities = view.findViewById<LinearLayout>(R.id.llRecentActivities)
        lineChart = view.findViewById(R.id.lineChart)
        tvAvatarInitials = view.findViewById(R.id.tvAvatarInitials)
        ivAvatarImage = view.findViewById(R.id.ivAvatarImage)
        ivNotification = view.findViewById(R.id.ivNotification)
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge)
        tvCard2Value = view.findViewById(R.id.tvCard2Value)
        tvCard3Value = view.findViewById(R.id.tvCard3Value)
        tvCard4Value = view.findViewById(R.id.tvCard4Value)

        ivNotification.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_notifications)
        }

        setupChart()
        loadDashboardData()

        return view
    }

    private fun setupChart() {
        lineChart.description.isEnabled = false
        lineChart.setDrawGridBackground(false)
        lineChart.axisRight.isEnabled = false
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisLeft.setDrawGridLines(true)
        lineChart.legend.isEnabled = false

        val days = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(days)
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.textColor = android.graphics.Color.parseColor("#9E9E9E")
        lineChart.axisLeft.textColor = android.graphics.Color.parseColor("#9E9E9E")

        val consumedEntries = arrayListOf(
            Entry(0f, 800f), Entry(1f, 1300f), Entry(2f, 1100f),
            Entry(3f, 900f), Entry(4f, 1000f), Entry(5f, 1400f), Entry(6f, 1200f)
        )
        val burnedEntries = arrayListOf(
            Entry(0f, 500f), Entry(1f, 800f), Entry(2f, 700f),
            Entry(3f, 600f), Entry(4f, 850f), Entry(5f, 650f), Entry(6f, 1000f)
        )

        val set1 = LineDataSet(consumedEntries, "Consommées")
        set1.color = android.graphics.Color.parseColor("#00C569")
        set1.lineWidth = 2f
        set1.setDrawCircles(true)
        set1.circleRadius = 3f
        set1.setCircleColor(android.graphics.Color.parseColor("#00C569"))
        set1.setDrawValues(false)

        val set2 = LineDataSet(burnedEntries, "Dépensées")
        set2.color = android.graphics.Color.parseColor("#F44336")
        set2.lineWidth = 2f
        set2.setDrawCircles(true)
        set2.circleRadius = 3f
        set2.setCircleColor(android.graphics.Color.parseColor("#F44336"))
        set2.setDrawValues(false)

        val data = LineData(set1, set2)
        lineChart.data = data
        lineChart.invalidate()
    }

    private fun loadDashboardData() {
        val sharedPref = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        if (userId == -1) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            val user = db.userDao().getUserById(userId)
            val meals = db.mealDao().getMealsForUser(userId)
            val workouts = db.workoutDao().getWorkoutsForUser(userId)
            val unreadCount = db.notificationDao().getUnreadCount(userId)

            val goal = user?.dailyCalorieGoal ?: 2100
            var consumedCalories = 0
            var consumedCarbs = 0
            var consumedProteins = 0
            var consumedFat = 0

            meals.forEach { meal ->
                consumedCalories += meal.calories
                consumedCarbs += meal.carbs
                consumedProteins += meal.protein
                consumedFat += meal.fat
            }

            var burnedCalories = 0
            var todaySteps = 0
            val now = java.util.Calendar.getInstance()
            
            workouts.forEach { workout ->
                val workoutCal = java.util.Calendar.getInstance()
                workoutCal.timeInMillis = workout.dateMs
                
                if (now.get(java.util.Calendar.YEAR) == workoutCal.get(java.util.Calendar.YEAR) &&
                    now.get(java.util.Calendar.DAY_OF_YEAR) == workoutCal.get(java.util.Calendar.DAY_OF_YEAR)) {
                    burnedCalories += workout.caloriesBurned
                    todaySteps += workout.steps
                }
            }

            val percentInt = if (consumedCalories > 0) (burnedCalories * 100) / consumedCalories else 0
            val percentFloat = if (consumedCalories > 0) (burnedCalories.toFloat() * 100) / consumedCalories.toFloat() else 0f
            
            // Formatage spécifique pour avoir 2 chiffres après la virgule
            val percentStr = String.format(java.util.Locale.FRANCE, "%.2f%%", percentFloat).replace("00%", "%").replace(",0%", "%")
            
            val remaining = consumedCalories - burnedCalories
            
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE)
            val consumedStr = formatter.format(consumedCalories).replace('\u202F', ' ').replace('\u00A0', ' ')
            val remainingStr = formatter.format(if (remaining > 0) remaining else 0).replace('\u202F', ' ').replace('\u00A0', ' ')

            withContext(Dispatchers.Main) {
                tvCard1Value.text = consumedStr
                tvCard2Value.text = burnedCalories.toString()
                progressBar.progress = percentInt
                tvProgressDesc.text = "Reste : $remainingStr kcal"
                tvProgressPercent.text = percentStr
                tvCarbsBadge.text = "${consumedCarbs}g"
                tvProteinBadge.text = "${consumedProteins}g"
                tvFatBadge.text = "${consumedFat}g"
                tvCard4Value.text = formatter.format(todaySteps).replace('\u202F', ' ').replace('\u00A0', ' ')
                
                if (user != null) {
                    if (!user.profilePictureUri.isNullOrEmpty()) {
                        val file = java.io.File(user.profilePictureUri)
                        if (file.exists()) {
                            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            ivAvatarImage.setImageBitmap(bitmap)
                            ivAvatarImage.visibility = View.VISIBLE
                            tvAvatarInitials.visibility = View.GONE
                        } else {
                            ivAvatarImage.visibility = View.GONE
                            tvAvatarInitials.visibility = View.VISIBLE
                            tvAvatarInitials.text = if (user.name.isNotEmpty()) user.name.substring(0, 1).uppercase() else "U"
                        }
                    } else {
                        ivAvatarImage.visibility = View.GONE
                        tvAvatarInitials.visibility = View.VISIBLE
                        tvAvatarInitials.text = if (user.name.isNotEmpty()) user.name.substring(0, 1).uppercase() else "U"
                    }
                } else {
                    ivAvatarImage.visibility = View.GONE
                    tvAvatarInitials.visibility = View.VISIBLE
                    tvAvatarInitials.text = "U"
                }
                
                // Update Notification Badge
                if (unreadCount > 0) {
                    tvNotificationBadge.visibility = View.VISIBLE
                    tvNotificationBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
                } else {
                    tvNotificationBadge.visibility = View.GONE
                }
                
                // Update Water Intake
                val waterIntake = sharedPref.getFloat("water_intake_$userId", 0f)
                tvCard3Value.text = String.format(java.util.Locale.FRANCE, "%.1f", waterIntake)
                
                
                // Helper function for time formatting
                fun formatTime(timestamp: Long): String {
                    val date = java.util.Date(timestamp)
                    val cal = java.util.Calendar.getInstance()
                    cal.time = date
                    val now = java.util.Calendar.getInstance()
                    val timeFormat = java.text.SimpleDateFormat("HH'H'mm", java.util.Locale.FRANCE)
                    val timeStr = timeFormat.format(date)
                    val isToday = now.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
                                  now.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR)
                    now.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    val isYesterday = now.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
                                      now.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR)
                    return when {
                        isToday -> "Aujourd'hui à $timeStr"
                        isYesterday -> "Hier à $timeStr"
                        else -> {
                            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)
                            "${dateFormat.format(date)} à $timeStr"
                        }
                    }
                }

                // Populate Recent Activities (max 3)
                llRecentActivities.removeAllViews()
                
                data class ActivityItem(val name: String, val timestamp: Long, val calories: Int, val isExercise: Boolean)
                val activityList = mutableListOf<ActivityItem>()
                meals.forEach { 
                    activityList.add(ActivityItem(it.name, it.dateTimestamp, it.calories, false))
                }
                // Add real exercises
                workouts.forEach { workout ->
                    activityList.add(ActivityItem(workout.name, workout.dateMs, -workout.caloriesBurned, true))
                }
                
                val recentActivities = activityList.sortedByDescending { it.timestamp }.take(3)
                
                recentActivities.forEach { activity ->
                    val activityView = layoutInflater.inflate(R.layout.item_recent_activity, llRecentActivities, false)
                    val ivIcon = activityView.findViewById<android.widget.ImageView>(R.id.ivActivityIcon)
                    val tvTitle = activityView.findViewById<TextView>(R.id.tvActivityTitle)
                    val tvTime = activityView.findViewById<TextView>(R.id.tvActivityTime)
                    val tvCalories = activityView.findViewById<TextView>(R.id.tvActivityCalories)
                    val tvUnit = activityView.findViewById<TextView>(R.id.tvActivityUnit)
                    
                    val truncatedName = if (activity.name.length > 20) activity.name.substring(0, 17) + "..." else activity.name
                    tvTitle.text = truncatedName
                    tvTime.text = formatTime(activity.timestamp)
                    
                    val activityFormatter = java.text.NumberFormat.getNumberInstance(java.util.Locale.FRANCE)
                    val formattedCalories = activityFormatter.format(Math.abs(activity.calories)).replace('\u202F', ' ').replace('\u00A0', ' ')
                    
                    if (activity.isExercise || activity.calories < 0) {
                        ivIcon.setImageResource(R.drawable.ic_walk_icon)
                        ivIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFECEF"))
                        ivIcon.setColorFilter(android.graphics.Color.parseColor("#F44336"))
                        tvCalories.text = "-$formattedCalories"
                        tvCalories.setTextColor(android.graphics.Color.parseColor("#F44336"))
                        tvUnit?.setTextColor(android.graphics.Color.parseColor("#F44336"))
                    } else {
                        ivIcon.setImageResource(R.drawable.ic_meal_icon)
                        ivIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                        ivIcon.setColorFilter(android.graphics.Color.parseColor("#4CAF50"))
                        tvCalories.text = "+$formattedCalories"
                        tvCalories.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                        tvUnit?.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                    }
                    
                    llRecentActivities.addView(activityView)
                }
            }
        }
    }
}
