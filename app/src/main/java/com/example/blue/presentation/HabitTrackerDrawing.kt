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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.asin
import kotlin.math.sqrt
import kotlin.math.abs


fun Rad(deg: Float): Double {
    return Math.toRadians(deg.toDouble())
}


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


fun DrawScope.drawSimpleRounded(
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    r_c: Float,
    color: Color,
    fillFrac: Float = 1.0f,
    backColor: Color? = null,
    arcCenter: Offset? = null
) {
    // Use center as default if arcCenter is null
    val arcCenter_ = arcCenter ?: center

    // Convert input angles from degrees to radians
    val startAngle_rad = Rad(startAngle)
    val sweepAngle_rad = Rad(sweepAngle)

    val cc_r = innerRadius + r_c

    val delta_theta = 2 * asin(r_c / (2 * cc_r))

    val theta_c1 = startAngle_rad - delta_theta
    val theta_c2 = startAngle_rad + sweepAngle_rad + delta_theta

    val c1_x = center.x + cc_r * cos(theta_c1).toFloat()
    val c1_y = center.y + cc_r * sin(theta_c1).toFloat()

    val c2_x = center.x + cc_r * cos(theta_c2).toFloat()
    val c2_y = center.y + cc_r * sin(theta_c2).toFloat()

    val theta_c1_deg = Math.toDegrees(theta_c1).toFloat()
    val theta_c2_deg = Math.toDegrees(theta_c2).toFloat()
    val sweep_deg = theta_c2_deg - theta_c1_deg

    val outerRadius = innerRadius + 2*r_c

    drawArcSegment(
        color = color,
        center = arcCenter_,
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
        radius = r_c,
        fillFrac = fillFrac,
        backgroundColor = backColor,
        maskCircleCenter = center
    )

    drawPartiallyFilledCircle(
        color = color,
        center = Offset(c2_x, c2_y),
        radius = r_c,
        fillFrac = fillFrac,
        backgroundColor = backColor,
        maskCircleCenter = center
    )

}
////
////
fun DrawScope.drawTriangularRounded(
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    halfThickness: Float,
    r_c: Float,
    color: Color,
    fillFrac: Float = 1.0f,
    backColor: Color? = null,
    arcCenter: Offset? = null
) {
    val startAngle_rad = Math.toRadians(startAngle.toDouble())
    val sweepAngle_rad = Math.toRadians(sweepAngle.toDouble())

    val r = innerRadius + r_c
    val theta_c_centre = startAngle_rad + sweepAngle_rad/2

    val c_x = (center.x + r*cos(theta_c_centre)).toFloat()
    val c_y = (center.y + r*sin(theta_c_centre)).toFloat()

    drawArcSegment(
        color = color,
        center = center,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        innerRadius = innerRadius + r_c,
        outerRadius = innerRadius + 2*halfThickness - r_c,
        fillFrac = fillFrac,
        backColor = backColor
    )

    drawPartiallyFilledCircle(
        color = color,
        center = Offset(c_x, c_y),
        radius = r_c,
        fillFrac = fillFrac,
        backgroundColor = backColor,
        maskCircleCenter = center
    )

    drawSimpleRounded(
        center = center,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        innerRadius = innerRadius + 2*halfThickness - 2*r_c,
        r_c = r_c,
        color = color,
        fillFrac = fillFrac,
        backColor = backColor,
        arcCenter = arcCenter
    )

}


fun DrawScope.drawRectangularRounded(
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    halfThickness: Float,
    r_c: Float,
    color: Color,
    fillFrac: Float = 1.0f,
    backColor: Color? = null,
    arcCenter: Offset? = null
) {

    drawArcSegment(
        color = color,
        center = center,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        innerRadius = innerRadius + r_c,
        outerRadius = innerRadius + 2*halfThickness - r_c,
        fillFrac = fillFrac,
        backColor = backColor
    )

    drawSimpleRounded(
        center = center,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        innerRadius = innerRadius + 2*halfThickness - 2*r_c,
        r_c = r_c,
        color = color,
        fillFrac = fillFrac,
        backColor = backColor,
        arcCenter = arcCenter
    )

    drawSimpleRounded(
        center = center,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        innerRadius = innerRadius,
        r_c = r_c,
        color = color,
        fillFrac = fillFrac,
        backColor = backColor,
        arcCenter = arcCenter
    )

}


fun DrawScope.drawAdaptiveRoundedSegment(
    more_than_two: Boolean,
    innermost: Boolean,
    center: Offset,
    startAngle: Float,
    sweepAngle: Float,
    innerRadius: Float,
    halfThickness: Float,
    r_c: Float,
    color: Color,
    fillFrac: Float = 1.0f,
    backColor: Color? = null,
    arcCenter: Offset? = null
) {

    val outerRadius = innerRadius + 2 * halfThickness

    if (more_than_two) {
        // When thickness is large, use simple arc without rounded end caps
        if (innermost) {
            drawTriangularRounded(
                center = center,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                innerRadius = innerRadius,
                halfThickness = halfThickness,
                r_c = r_c,
                color = color,
                fillFrac = fillFrac,
                backColor = backColor,
                arcCenter = arcCenter
            )
        } else {
            drawRectangularRounded(
                center = center,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                innerRadius = innerRadius,
                halfThickness = halfThickness,
                r_c = r_c,
                color = color,
                fillFrac = fillFrac,
                backColor = backColor,
                arcCenter = arcCenter
            )
        }


    } else {

        drawSimpleRounded(
            center = center,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            innerRadius = innerRadius,
            r_c = r_c,
            color = color,
            fillFrac = fillFrac,
            backColor = backColor,
            arcCenter = arcCenter
        )
    }
}


data class DisplayGeometry(
    val center: Offset,
    val maxRadius: Float,
    val innerMargin: Float,
    val gapSize: Float,
    val numDays: Int,
    val numHabits: Int,
    val startAngle: Float = -90f,
    val gapAngle: Float = 45f
) {
    val totalArcAngle: Float
    val segmentAngle: Float
    val availableRadius: Float
    val habitLayerThickness: Float
    val halfThickness: Float
    val effectiveCenterRadius: Float
    val r_intermed: Float
    val c_rad: Float
    val r_c: Float
    val more_than_two: Boolean

    // Pre-calculated arrays for all segments
    val dayStartAngles: FloatArray
    val habitInnerRadii: FloatArray
    val habitOuterRadii: FloatArray
    val effectiveCenters: Array<Offset>

    init {
        totalArcAngle = 360f - gapAngle  // Available degrees for the days
        segmentAngle = totalArcAngle / numDays // Degrees per day

        availableRadius = maxRadius - innerMargin
        habitLayerThickness = (availableRadius - (numHabits - 1) * gapSize) / numHabits
        halfThickness = habitLayerThickness / 2

        effectiveCenterRadius = gapSize / (2 * sin(Rad(segmentAngle) / 2).toFloat())
        if (effectiveCenterRadius > innerMargin || effectiveCenterRadius < 0) {
            throw IllegalArgumentException("effectiveCenterRadius is wrong")
        }

        val effectiveCenterInnerMargin = innerMargin - effectiveCenterRadius

        r_intermed = (effectiveCenterInnerMargin / (1 - 2 * sin(Rad(segmentAngle) / 4))).toFloat()
        c_rad = r_intermed - effectiveCenterInnerMargin

        println(halfThickness)
        println(c_rad)

        more_than_two = (halfThickness > c_rad)

        r_c = kotlin.math.min(c_rad, halfThickness)

        // Pre-calculate day start angles (anticlockwise from startAngle)
        dayStartAngles = FloatArray(numDays) { dayIndex ->
            startAngle - (dayIndex * segmentAngle)
        }

        // Pre-calculate habit layer radii (habit 0 is innermost, highest index is outermost)
        // Each layer is separated by gapSize
        habitOuterRadii = FloatArray(numHabits) { habitIndex ->
            val reversedIndex = numHabits - 1 - habitIndex
            maxRadius - reversedIndex * (habitLayerThickness + gapSize)
        }

        habitInnerRadii = FloatArray(numHabits) { habitIndex ->
            habitOuterRadii[habitIndex] - habitLayerThickness
        }

        // Pre-calculate effective centers for each day (creates visual spacing)
        effectiveCenters = Array(numDays) { dayIndex ->
            val dayStartAngle = dayStartAngles[dayIndex]
            val effectiveCenterAngle = Math.toRadians((dayStartAngle - segmentAngle / 2).toDouble())
            val effectiveCenter_x = center.x + effectiveCenterRadius * cos(effectiveCenterAngle).toFloat()
            val effectiveCenter_y = center.y + effectiveCenterRadius * sin(effectiveCenterAngle).toFloat()
            Offset(effectiveCenter_x, effectiveCenter_y)
        }
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
    selectedHabitIndex: Int,
    numDays: Int
) {
    val geometry = DisplayGeometry(
        center = center,
        maxRadius = maxRadius,
        innerMargin = innerMargin,
        gapSize = 5f,
        numDays = numDays,
        numHabits = habits.size,
        startAngle = -90f,
        gapAngle = 45f
    )

    // Draw each habit layer (habit 0 is innermost, highest index is outermost)
    habits.forEachIndexed { habitIndex, habit ->
        val innerRadius = geometry.habitInnerRadii[habitIndex]
        val outerRadius = geometry.habitOuterRadii[habitIndex]

        // Draw each day segment for this habit
        for (dayIndex in 0 until geometry.numDays) {
            val dayStartAngle = geometry.dayStartAngles[dayIndex]
            val effectiveCenter = geometry.effectiveCenters[dayIndex]

            // Find completion status for this habit/day
            val completion = completions.find {
                it.habitId == habit.id && it.dayIndex == dayIndex
            }

            // Check if this is the selected segment
            val isSelected = (habitIndex == selectedHabitIndex && dayIndex == selectedDayIndex)
            val innermost = (habitIndex == 0)

            // Draw based on habit type
            when (habit) {
                is Habit.MultipleHabit -> {
                    val completionCount = completion?.completionCount ?: 0
                    val completedColor = if (isSelected) darkGreen else darkBlue
                    val notCompletedColor = if (isSelected) lightGreen else paleGrey
                    val fillFrac = completionCount.toFloat() / habit.completionsPerDay.toFloat()

                    drawAdaptiveRoundedSegment(
                        more_than_two = geometry.more_than_two,
                        innermost = innermost,
                        center = effectiveCenter,
                        startAngle = dayStartAngle,
                        sweepAngle = -geometry.segmentAngle,
                        innerRadius = innerRadius - geometry.effectiveCenterRadius,
                        halfThickness = geometry.halfThickness,
                        r_c = geometry.r_c,
                        color = completedColor,
                        fillFrac = fillFrac,
                        backColor = notCompletedColor,
                        arcCenter = effectiveCenter
                    )
                }
                is Habit.TimeBasedHabit -> {
                    // Time-based habits: color based on completion time vs target time
                    val segmentColor = if (completion?.completionTime != null) {
                        // Has a completion time recorded
                        val completionTime = completion.completionTime
                        val targetTime = habit.targetTime

                        // Compare times (HH:mm format)
                        val isOnTime = completionTime <= targetTime

                        if (isSelected) {
                            if (isOnTime) darkGreen else lightGreen
                        } else {
                            if (isOnTime) darkBlue else paleBlue
                        }
                    } else {
                        // No completion time recorded
                        if (isSelected) lightGreen else paleGrey
                    }

                    drawAdaptiveRoundedSegment(
                        more_than_two = geometry.more_than_two,
                        innermost = innermost,
                        center = effectiveCenter,
                        startAngle = dayStartAngle,
                        sweepAngle = -geometry.segmentAngle,
                        innerRadius = innerRadius - geometry.effectiveCenterRadius,
                        halfThickness = geometry.halfThickness,
                        r_c = geometry.r_c,
                        color = segmentColor,
                        arcCenter = effectiveCenter
                    )

                    // Draw completion time text if this habit is selected and has a time
                    if (habitIndex == selectedHabitIndex && completion?.completionTime != null) {
                        val time = completion.completionTime
                        val midAngle = dayStartAngle - (geometry.segmentAngle / 2f)
                        val angleRad = Math.toRadians(midAngle.toDouble())
                        val midRadius = ((innerRadius + outerRadius) / 2f)

                        // Calculate text rotation (perpendicular to radial line)
                        var textRotation = midAngle + 90f
                        // Normalize angle to 0-360 range
                        textRotation = ((textRotation % 360f) + 360f) % 360f
                        // Flip text 180° if it's upside down (bottom half of screen)
                        if (textRotation > 90f && textRotation < 270f) {
                            textRotation += 180f
                        }

                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 14f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }

                            // Position at center of segment
                            val centerX = center.x + midRadius * cos(angleRad).toFloat()
                            val centerY = center.y + midRadius * sin(angleRad).toFloat()

                            // Calculate offset to center text vertically at this point
                            val fontMetrics = paint.fontMetrics
                            val verticalCenter = (fontMetrics.ascent + fontMetrics.descent) / 2f

                            // Save canvas state, rotate around the center point, draw text with offset, restore
                            canvas.nativeCanvas.save()
                            canvas.nativeCanvas.rotate(textRotation, centerX, centerY)
                            // Draw text with y-offset so vertical center is at centerY
                            canvas.nativeCanvas.drawText(time, centerX, centerY - verticalCenter, paint)
                            canvas.nativeCanvas.restore()
                        }
                    }
                }
                is Habit.BinaryHabit -> {
                    // Binary habits use isCompleted flag
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
                        more_than_two = geometry.more_than_two,
                        innermost = innermost,
                        center = effectiveCenter,
                        startAngle = dayStartAngle,
                        sweepAngle = -geometry.segmentAngle,
                        innerRadius = innerRadius - geometry.effectiveCenterRadius,
                        halfThickness = geometry.halfThickness,
                        r_c = geometry.r_c,
                        color = segmentColor,
                        arcCenter = effectiveCenter
                    )
                }
            }
        }
    }
}