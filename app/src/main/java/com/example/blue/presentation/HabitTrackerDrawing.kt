// HabitTrackerDrawing.kt
package com.example.blue.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.asin
import kotlin.math.sqrt
import kotlin.math.min
import kotlin.math.abs

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

fun DrawScope.drawPartiallyFilledCircle(
    color: Color,
    center: Offset,
    radius: Float,
    fillFrac: Float,
    backgroundColor: Color? = null,
    maskCircleCenter: Offset? = null
) {
    // Draw background circle if specified
    backgroundColor?.let {
        drawCircle(
            color = it,
            radius = radius,
            center = center
        )
    }

    if (maskCircleCenter != null) {
        // Curved fill using another circle as mask
        // Calculate distance between centers
        val d = sqrt(
            (maskCircleCenter.x - center.x).pow(2) +
                    (maskCircleCenter.y - center.y).pow(2)
        )

        // Calculate mask circle radius so it penetrates by fillFrac * diameter
        // fillFrac=0: mask circle just touches edge (R2 = d - R1)
        // fillFrac=0.5: mask circle passes through center (R2 = d)
        // fillFrac=1.0: mask circle completely covers main circle (R2 = d + R1)
        val maskRadius = d + radius * (2 * fillFrac - 1)

        // Clip to the mask circle and draw the main circle
        clipPath(
            path = Path().apply {
                addOval(
                    Rect(
                        left = maskCircleCenter.x - maskRadius,
                        top = maskCircleCenter.y - maskRadius,
                        right = maskCircleCenter.x + maskRadius,
                        bottom = maskCircleCenter.y + maskRadius
                    )
                )
            }
        ) {
            drawCircle(
                color = color,
                radius = radius,
                center = center
            )
        }
    } else {
        // Original straight horizontal fill
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
}


fun DrawScope.drawRoundedArcSegment(
    color: Color,
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    outerRadius: Float,
    fillFrac: Float = 1.0f,
    backColor: Color? = null
) {
    // Convert input angles from degrees to radians
    val startAngle_rad = Math.toRadians(startAngle.toDouble())
    val sweepAngle_rad = Math.toRadians(sweepAngle.toDouble())

    val halfThickness = (outerRadius - innerRadius) / 2
    val c_rad = halfThickness
    val cc_r = innerRadius + halfThickness

    val delta_theta = 2 * asin(c_rad / (2 * cc_r))

    val theta_c1 = startAngle_rad - delta_theta
    val theta_c2 = startAngle_rad + sweepAngle_rad + delta_theta

    val c1_x = center.x + cc_r * cos(theta_c1).toFloat()
    val c1_y = center.y + cc_r * sin(theta_c1).toFloat()

    val c2_x = center.x + cc_r * cos(theta_c2).toFloat()
    val c2_y = center.y + cc_r * sin(theta_c2).toFloat()

    val theta_c1_deg = Math.toDegrees(theta_c1).toFloat()
    val theta_c2_deg = Math.toDegrees(theta_c2).toFloat()
    val sweep_deg = theta_c2_deg - theta_c1_deg

    drawArcSegment(
        color = color,
        center = center,
        startAngle = theta_c1_deg,
        sweepAngle = sweep_deg,
        innerRadius = innerRadius,
        outerRadius = outerRadius,
        fillFrac = fillFrac,
        backColor = backColor
    )

    // Draw both end cap circles with partial fill using curved mask
    drawPartiallyFilledCircle(
        color = color,
        center = Offset(c1_x, c1_y),
        radius = c_rad,
        fillFrac = fillFrac,
        backgroundColor = backColor,
        maskCircleCenter = center
    )

    drawPartiallyFilledCircle(
        color = color,
        center = Offset(c2_x, c2_y),
        radius = c_rad,
        fillFrac = fillFrac,
        backgroundColor = backColor,
        maskCircleCenter = center
    )

}


fun DrawScope.drawTriangularRoundedSegment(
    color: Color,
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    outerRadius: Float,
    fillFrac: Float = 1.0f,
    backColor: Color? = null
) {
    val startAngle_rad = Math.toRadians(startAngle.toDouble())
    val sweepAngle_rad = Math.toRadians(sweepAngle.toDouble())

    val r = innerRadius / (1 - 2*sin(abs(sweepAngle_rad)/4))

    println(r)

    val c_rad = (r - innerRadius).toFloat()

    println(c_rad)


    val theta_c_centre = startAngle_rad + sweepAngle_rad/2

    val c_x = (center.x + r*cos(theta_c_centre)).toFloat()
    val c_y = (center.y + r*sin(theta_c_centre)).toFloat()

    drawPartiallyFilledCircle(
        color = color,
        center = Offset(c_x, c_y),
        radius = c_rad,
        fillFrac = fillFrac,
        backgroundColor = backColor,
        maskCircleCenter = center
    )

    val r_outer_inner = outerRadius - c_rad*2

    drawRoundedArcSegment(
        color,
        center,
        startAngle,
        sweepAngle,
        r_outer_inner,
        outerRadius,
        fillFrac,
        backColor
    )

    val r_mid_outer = outerRadius - c_rad
    val r_min_inner = innerRadius + c_rad

    drawArcSegment(
        color = color,
        center = center,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        innerRadius = r_min_inner,
        outerRadius = r_mid_outer,
        fillFrac = fillFrac,
        backColor = backColor
    )

}

fun DrawScope.drawAdaptiveRoundedSegment(
    color: Color,
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    outerRadius: Float,
    fillFrac: Float = 1.0f,
    backColor: Color? = null
) {
    // Convert input angles from degrees to radians
    val startAngle_rad = Math.toRadians(startAngle.toDouble())
    val sweepAngle_rad = Math.toRadians(sweepAngle.toDouble())

    val halfThickness = (outerRadius - innerRadius) / 2
    val cc_r = innerRadius + halfThickness

    val max_c_rad = 2*cc_r*sin(abs(sweepAngle_rad)/4).toFloat()

    if (halfThickness < max_c_rad) {
        drawRoundedArcSegment(
            color,
            center,
            startAngle,
            sweepAngle,
            innerRadius,
            outerRadius,
            fillFrac,
            backColor
        )
    } else {
        drawTriangularRoundedSegment(
            color,
            center,
            startAngle,
            sweepAngle,
            innerRadius,
            outerRadius,
            fillFrac,
            backColor
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

                    drawAdaptiveRoundedSegment(
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

                    drawAdaptiveRoundedSegment(
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