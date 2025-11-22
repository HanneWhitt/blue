// HabitTrackerDrawing.kt
package com.example.blue.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.asin

fun DrawScope.drawArcSegment(
    color: Color,
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    outerRadius: Float,
    fillFrac: Float = 1.0f,
    backColor: Color? = null
) {
    val thickness = outerRadius - innerRadius
    // Stroke is centered on path, so use midpoint between inner and outer radii
    val midRadius = (innerRadius + outerRadius) / 2

    when {
        fillFrac >= 1.0f -> {
            // Full segment in main color
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - midRadius,
                    center.y - midRadius
                ),
                size = Size(midRadius * 2, midRadius * 2),
                style = Stroke(width = thickness)
            )
        }
        fillFrac <= 0.0f && backColor != null -> {
            // Full segment in background color
            drawArc(
                color = backColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - midRadius,
                    center.y - midRadius
                ),
                size = Size(midRadius * 2, midRadius * 2),
                style = Stroke(width = thickness)
            )
        }
        else -> {
            // Partial fill: draw background then partial foreground
            if (backColor != null) {
                // Draw full background segment
                drawArc(
                    color = backColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - midRadius,
                        center.y - midRadius
                    ),
                    size = Size(midRadius * 2, midRadius * 2),
                    style = Stroke(width = thickness)
                )
            }

            // Draw partial foreground segment (from inner to fillFrac of thickness)
            val partialOuterRadius = innerRadius + (thickness * fillFrac)
            val partialStrokeWidth = partialOuterRadius - innerRadius
            val partialMidRadius = (innerRadius + partialOuterRadius) / 2

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    center.x - partialMidRadius,
                    center.y - partialMidRadius
                ),
                size = Size(partialMidRadius * 2, partialMidRadius * 2),
                style = Stroke(width = partialStrokeWidth)
            )
        }
    }
}

fun DrawScope.drawRoundedArcSegment(
    color: Color,
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    outerRadius: Float
) {
    // Convert input angles from degrees to radians
    val startAngle_rad = Math.toRadians(startAngle.toDouble())
    val sweepAngle_rad = Math.toRadians(sweepAngle.toDouble())

    val c_rad = (outerRadius - innerRadius) / 2
    val cc_r = innerRadius + c_rad*2

    val delta_theta = 2 * asin(c_rad / (2 * cc_r))

    val theta_c1 = startAngle_rad - delta_theta
    val theta_c2 = startAngle_rad + sweepAngle_rad + delta_theta

    val c1_x = center.x + cc_r * cos(theta_c1).toFloat()
    val c1_y = center.y + cc_r * sin(theta_c1).toFloat()

    val c2_x = center.x + cc_r * cos(theta_c2).toFloat()
    val c2_y = center.y + cc_r * sin(theta_c2).toFloat()

    // Draw both circles with radius c_rad at the calculated centers
    drawCircle(
        color = color,
        radius = c_rad,
        center = Offset(c1_x, c1_y)
    )

    drawCircle(
        color = color,
        radius = c_rad,
        center = Offset(c2_x, c2_y)
    )

    val theta_c1_deg = Math.toDegrees(theta_c1).toFloat()
    val theta_c2_deg = Math.toDegrees(theta_c2).toFloat()
    val sweep_deg = theta_c2_deg - theta_c1_deg

    drawArcSegment(
        color = color,
        center = center,
        startAngle = theta_c1_deg,
        sweepAngle = sweep_deg,
        innerRadius = innerRadius,
        outerRadius = outerRadius
    )
}

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
    val radGapProportion = 0.8f
    val angleGapCenterOffset = 0.3f

    // Arc configuration variables
    val startingAngle = -90f  // Start at 12 o'clock (vertical, top of screen)
    val gapAngle = 45f        // Gap size in degrees (adjustable)

    val totalArcAngle = 360f - gapAngle  // Available degrees for the days
    val segmentAngle = totalArcAngle / numDays  // Each day gets equal portion

    val availableRadius = maxRadius - innerMargin
    val habitLayerThickness = availableRadius / numHabits

    val effectiveCenterOffsetRadius = innerMargin*angleGapCenterOffset

    // Draw each habit layer (habit 0 is innermost, highest index is outermost)
    habits.forEachIndexed { habitIndex, habit ->
        val reversedIndex = numHabits - 1 - habitIndex
        val outerRadius = maxRadius - (reversedIndex * habitLayerThickness)
        val innerRadius = outerRadius - habitLayerThickness * radGapProportion // Leave gap between layers

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

            val effectiveCenterAngle = Math.toRadians((dayStartAngle - segmentAngle/2).toDouble())

            val effectiveCenter_x = center.x + effectiveCenterOffsetRadius*cos(effectiveCenterAngle).toFloat()
            val effectiveCenter_y = center.y + effectiveCenterOffsetRadius*sin(effectiveCenterAngle).toFloat()
            val effectiveCenter = Offset(effectiveCenter_x, effectiveCenter_y)

            // Draw based on habit type
            when (habit) {
                is Habit.MultipleHabit -> {
                    val completionCount = completion?.completionCount ?: 0
                    val completedColor = if (isSelected) darkGreen else darkBlue
                    val notCompletedColor = if (isSelected) lightGreen else paleGrey
                    val fillFrac = completionCount.toFloat() / habit.completionsPerDay.toFloat()

                    drawArcSegment(
                        color = completedColor,
                        center = effectiveCenter,
                        startAngle = dayStartAngle,
                        sweepAngle = -segmentAngle,
                        innerRadius = innerRadius - effectiveCenterOffsetRadius,
                        outerRadius = outerRadius - effectiveCenterOffsetRadius,
                        fillFrac = fillFrac,
                        backColor = notCompletedColor
                    )
                }
                else -> {
                    // Binary and Time-based habits use existing logic
                    val segmentColor = if (isSelected) {
                        when (completion?.isCompleted) {
                            true -> darkGreen
                            else -> lightGreen
                        }
                    } else {
                        when (completion?.isCompleted) {
                            null -> paleGrey
                            true -> darkBlue
                            false -> paleBlue
                        }
                    }

                    drawArcSegment(
                        color = segmentColor,
                        center = effectiveCenter,
                        startAngle = dayStartAngle,
                        sweepAngle = -segmentAngle,
                        innerRadius = innerRadius - effectiveCenterOffsetRadius,
                        outerRadius = outerRadius - effectiveCenterOffsetRadius
                    )
                }
            }
        }
    }
}

fun DrawScope.drawPartiallyFilledCircle(
    color: Color,
    center: Offset,
    radius: Float,
    fillFrac: Float,
    backgroundColor: Color? = null
) {
    // Draw background circle if specified
    backgroundColor?.let {
        drawCircle(
            color = it,
            radius = radius,
            center = center
        )
    }

    // Calculate the y-position of the fill line
    // fillFrac=0 -> bottom of circle (cy + r)
    // fillFrac=0.5 -> center (cy)
    // fillFrac=1 -> top of circle (cy - r)
    val fillHeight = 2 * radius * fillFrac
    val clipBottom = center.y + radius
    val clipTop = clipBottom - fillHeight

    // Clip and draw the filled portion
    clipRect(
        left = center.x - radius,
        top = clipTop,
        right = center.x + radius,
        bottom = clipBottom
    ) {
        drawCircle(
            color = color,
            radius = radius,
            center = center
        )
    }
}
