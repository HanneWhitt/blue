// MainActivity.kt
package com.example.blue.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            HabitTrackerDisplay()
        }
    }
}

// Data classes
data class HabitCompletion(
    val habitId: Int,
    val dayIndex: Int, // 0-13 for 14 days
    val isCompleted: Boolean?  // null = no data, true = completed, false = not completed
)

data class Habit(
    val id: Int,
    val name: String,
    val color: Color
)

@Composable
fun HabitTrackerDisplay() {
    // Spacing configuration variables
    val outerMarginPx = 20f      // Distance from screen edge to outer habit layer
    val innerMarginPx = 40f      // Distance from screen center to inner habit layer

    // Define your three colors
    val darkBlue = Color(0xFF1565C0)      // Dark blue for completed
    val paleBlue = Color(0xFFBBDEFB)      // Pale blue for not completed
    val paleGrey = Color(0xFFE0E0E0)      // Pale grey for no data

    // Sample data - 5 habits over 14 days
    val habits = remember {
        listOf(
            Habit(0, "Exercise", darkBlue),
            Habit(1, "Read", darkBlue),
            Habit(2, "Meditate", darkBlue),
            Habit(3, "Water", darkBlue),
            Habit(4, "Sleep", darkBlue)
        )
    }

    // Sample completion data with good variety to show all three states
    val completions = remember {
        mutableListOf<HabitCompletion>().apply {
            for (habitId in 0..4) {
                for (day in 0..13) {
                    val completion = when {
                        // First 3 days: no data for all habits
                        day < 3 -> null
                        // Days 3-5: mixed pattern
                        day in 3..5 -> when (habitId) {
                            0 -> true  // Exercise: completed
                            1 -> false // Read: not completed
                            2 -> true  // Meditate: completed
                            3 -> false // Water: not completed
                            4 -> null  // Sleep: no data
                            else -> null
                        }
                        // Days 6-9: different pattern
                        day in 6..9 -> when (habitId) {
                            0 -> false // Exercise: not completed
                            1 -> true  // Read: completed
                            2 -> false // Meditate: not completed
                            3 -> true  // Water: completed
                            4 -> true  // Sleep: completed
                            else -> null
                        }
                        // Days 10-13: another pattern
                        else -> when (habitId) {
                            0 -> true  // Exercise: completed
                            1 -> true  // Read: completed
                            2 -> null  // Meditate: no data
                            3 -> true  // Water: completed
                            4 -> false // Sleep: not completed
                            else -> null
                        }
                    }
                    add(HabitCompletion(habitId, day, completion))
                }
            }
        }
    }

    val density = LocalDensity.current
    val screenSize = 200.dp // Approximate watch screen size

    Canvas(
        modifier = Modifier.size(screenSize)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f - outerMarginPx

        drawHabitTracker(
            center = center,
            maxRadius = maxRadius,
            innerMargin = innerMarginPx,
            habits = habits,
            completions = completions,
            darkBlue = darkBlue,
            paleBlue = paleBlue,
            paleGrey = paleGrey
        )
    }
}

fun DrawScope.drawHabitTracker(
    center: Offset,
    maxRadius: Float,
    innerMargin: Float,
    habits: List<Habit>,
    completions: List<HabitCompletion>,
    darkBlue: Color,
    paleBlue: Color,
    paleGrey: Color
) {
    val numDays = 14
    val numHabits = habits.size
    val segmentAngle = 360f / numDays
    val availableRadius = maxRadius - innerMargin
    val habitLayerThickness = availableRadius / numHabits

    // Draw each habit layer (from outside to inside)
    habits.forEachIndexed { habitIndex, habit ->
        val outerRadius = maxRadius - (habitIndex * habitLayerThickness)
        val innerRadius = outerRadius - habitLayerThickness * 0.8f // Leave gap between layers

        // Draw each day segment for this habit
        for (dayIndex in 0 until numDays) {
            val startAngle = dayIndex * segmentAngle - 90f // Start from top

            // Find completion status for this habit/day
            val completion = completions.find {
                it.habitId == habit.id && it.dayIndex == dayIndex
            }

            val segmentColor = when (completion?.isCompleted) {
                null -> paleGrey      // No data - pale grey
                true -> darkBlue      // Completed - dark blue
                false -> paleBlue     // Not completed - pale blue
            }

            // Draw the arc segment
            drawArc(
                color = segmentColor,
                startAngle = startAngle,
                sweepAngle = segmentAngle - 2f, // Small gap between segments
                useCenter = false,
                topLeft = Offset(
                    center.x - outerRadius,
                    center.y - outerRadius
                ),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = outerRadius - innerRadius)
            )
        }
    }
}