// HabitTrackerDrawing.kt
package com.example.blue.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawHabitTracker(
    center: Offset,
    maxRadius: Float,
    innerMargin: Float,
    habits: List<Habit>,
    completions: List<HabitCompletion>,
    darkBlue: Color,
    paleBlue: Color,
    paleGrey: Color,
    lightGreen: Color,
    darkGreen: Color,
    selectedDayIndex: Int,
    selectedHabitIndex: Int
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

    // Draw each habit layer (habit 0 is innermost, highest index is outermost)
    habits.forEachIndexed { habitIndex, habit ->
        val reversedIndex = numHabits - 1 - habitIndex
        val outerRadius = maxRadius - (reversedIndex * habitLayerThickness)
        val innerRadius = outerRadius - habitLayerThickness * 0.8f // Leave gap between layers

        // Draw each day segment for this habit
        for (dayIndex in 0 until numDays) {
            // Calculate angle for this day (going anticlockwise from starting position)
            val dayStartAngle = startingAngle - (dayIndex * segmentAngle)

            // Find completion status for this habit/day
            val completion = completions.find {
                it.habitId == habit.id && it.dayIndex == dayIndex
            }

            // Check if this is the selected segment
            val isSelected = (habitIndex == selectedHabitIndex && dayIndex == selectedDayIndex)

            val segmentColor = if (isSelected) {
                // Selected segment uses green colors
                when (completion?.isCompleted) {
                    true -> darkGreen     // Completed - dark green
                    else -> lightGreen    // No data or not completed - light green
                }
            } else {
                // Non-selected segments use original blue/grey colors
                when (completion?.isCompleted) {
                    null -> paleGrey      // No data - pale grey
                    true -> darkBlue      // Completed - dark blue
                    false -> paleBlue     // Not completed - pale blue
                }
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
