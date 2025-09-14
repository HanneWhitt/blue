// MainActivity.kt
package com.example.blue.presentation

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import org.json.JSONArray
import org.json.JSONObject
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
    val dayIndex: Int, // 0-9 for 10 days
    val isCompleted: Boolean?  // null = no data, true = completed, false = not completed
)

data class Habit(
    val id: Int,
    val name: String,
    val color: Color
)

data class HabitData(
    val habits: List<Habit>,
    val completions: List<HabitCompletion>
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

    val context = LocalContext.current
    val habitData = remember {
        loadHabitDataFromAssets(context)
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
            habits = habitData.habits,
            completions = habitData.completions,
            darkBlue = darkBlue,
            paleBlue = paleBlue,
            paleGrey = paleGrey
        )
    }
}

fun loadHabitDataFromAssets(context: Context): HabitData {
    return try {
        val inputStream = context.assets.open("sample_habit_data.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        val habitsArray = jsonObject.getJSONArray("habits")
        val habits = mutableListOf<Habit>()
        for (i in 0 until habitsArray.length()) {
            val habitJson = habitsArray.getJSONObject(i)
            val colorHex = habitJson.getString("colorHex")
            val color = Color(android.graphics.Color.parseColor(colorHex))
            habits.add(
                Habit(
                    id = habitJson.getInt("id"),
                    name = habitJson.getString("name"),
                    color = color
                )
            )
        }

        val completionsArray = jsonObject.getJSONArray("completions")
        val completions = mutableListOf<HabitCompletion>()
        for (i in 0 until completionsArray.length()) {
            val completionJson = completionsArray.getJSONObject(i)
            val isCompleted = if (completionJson.isNull("isCompleted")) {
                null
            } else {
                completionJson.getBoolean("isCompleted")
            }
            completions.add(
                HabitCompletion(
                    habitId = completionJson.getInt("habitId"),
                    dayIndex = completionJson.getInt("dayIndex"),
                    isCompleted = isCompleted
                )
            )
        }

        HabitData(habits, completions)
    } catch (e: Exception) {
        // Fallback to empty data if file loading fails
        HabitData(emptyList(), emptyList())
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
    val numDays = 10
    val numHabits = habits.size

    // Arc configuration variables
    val startingAngle = -90f  // Start at 12 o'clock (vertical, top of screen)
    val gapAngle = 45f        // Gap size in degrees (adjustable)

    val totalArcAngle = 360f - gapAngle  // Available degrees for the days
    val segmentAngle = totalArcAngle / numDays  // Each day gets equal portion

    val availableRadius = maxRadius - innerMargin
    val habitLayerThickness = availableRadius / numHabits

    // Draw each habit layer (from outside to inside)
    habits.forEachIndexed { habitIndex, habit ->
        val outerRadius = maxRadius - (habitIndex * habitLayerThickness)
        val innerRadius = outerRadius - habitLayerThickness * 0.8f // Leave gap between layers

        // Draw each day segment for this habit
        for (dayIndex in 0 until numDays) {
            // Calculate angle for this day (going anticlockwise from starting position)
            val dayStartAngle = startingAngle - (dayIndex * segmentAngle)

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
                startAngle = dayStartAngle,
                sweepAngle = -segmentAngle + 2f, // Negative for anticlockwise, small gap between segments
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