package com.example.fitmatch.models

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.fitmatch.data.ProgressSummary
import com.example.fitmatch.data.WeeklyProgress
import com.example.fitmatch.data.WeeklySeries
import com.example.fitmatch.data.WorkoutEntry
import com.example.fitmatch.screens.sampleWorkouts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DemoRepo : FitMatchMLRepository {
    @RequiresApi(Build.VERSION_CODES.O)
    private val _recent = MutableStateFlow(sampleWorkouts())
    @RequiresApi(Build.VERSION_CODES.O)
    override fun observeRecentWorkouts(): Flow<List<WorkoutEntry>> = _recent

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getWeeklyProgress(scope: String): WeeklyProgress {
        // Simulate latency
        delay(350)
        val labels = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
        val durations = listOf(35,58,0,52,66,92,34) // minutes
        val calories = listOf(320,440,0,360,510,680,290)
        return WeeklyProgress(
            summary = ProgressSummary(
                totalWorkouts = 89,
                workoutsChangePct = 5,
                caloriesBurned = 12450,
                caloriesSpanDays = 12,
                activeTimeMinutes = 72*60,
                weightLbs = 180.5,
                weightChangeLbs = -4.5
            ),
            series = WeeklySeries(labels, durations, calories),
            recent = _recent.value
        )
    }
}