// HabitTrackerUI.kt
package com.example.blue.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.wear.compose.material.Text
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitTrackerDisplay(
    onNavigateToManagement: () -> Unit = {}
) {
    // Current date tracking
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    // Selection state
    var selectedDayIndex by remember { mutableStateOf(0) }
    var selectedHabitIndex by remember { mutableStateOf(0) }

    var scrollAccumulator by remember { mutableStateOf(0f) }
    val scrollThreshold = 25f  // Adjust this value to control rotary scroll sensitivity

    var touchScrollAccumulator by remember { mutableStateOf(0f) }
    val touchScrollThreshold = 50f  // Adjust this value to control touch scroll sensitivity

    // Derived date information for selected day
    val selectedDate by remember { derivedStateOf { currentDate.minusDays(selectedDayIndex.toLong()) } }
    val selectedDayOfMonth by remember { derivedStateOf { selectedDate.dayOfMonth.toString() } }
    val selectedDayOfWeek by remember { derivedStateOf {
        selectedDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } }

    // Spacing configuration variables
    val outerMarginPx = 30f      // Distance from screen edge to outer habit layer
    val innerMarginPx = 40f      // Distance from screen center to inner habit layer

    // Define your three colors
    val darkBlue = Color(0xFF1565C0)      // Dark blue for completed
    val paleBlue = Color(0xFFBBDEFB)      // Pale blue for not completed
    val paleGrey = Color(0xFFE0E0E0)      // Pale grey for no data

    // Selection colors
    val lightGreen = Color(0xFF81C784)    // Light green for selected (no data or not completed)
    val darkGreen = Color(0xFF2E7D32)     // Dark green for selected (completed)

    val context = LocalContext.current
    val numDays = 10

    // Load habit data - reloads when currentDate changes
    var habitData by remember { mutableStateOf(loadHabitData(context, currentDate, numDays)) }

    // Mutable completions state
    var completions by remember { mutableStateOf(habitData.completions) }

    // Derived habit abbreviation for selected habit
    val selectedHabitAbbreviation by remember { derivedStateOf {
        habitData.habits.getOrNull(selectedHabitIndex)?.abbreviation ?: ""
    } }

    // Reload data when date changes
    LaunchedEffect(currentDate) {
        habitData = loadHabitData(context, currentDate, numDays)
        completions = habitData.completions
    }

    val configuration = LocalConfiguration.current
    val screenSize = minOf(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
    val focusRequester = remember { FocusRequester() }

    val numHabits = habitData.habits.size

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Monitor lifecycle events to check for date changes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                val newDate = LocalDate.now()
                if (newDate != currentDate) {
                    println("DATE HAS CHANGED!")
                    currentDate = newDate
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Save data whenever completions change
    LaunchedEffect(completions) {
        saveHabitDataToFile(
            context,
            HabitData(
                habits = habitData.habits,
                completions = completions
            ),
            currentDate,
            numDays
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(screenSize),
            contentAlignment = Alignment.Center
        ) {
        Canvas(
            modifier = Modifier
                .size(screenSize)
                .onRotaryScrollEvent { event ->

                    // Accumulate scroll delta
                    scrollAccumulator += event.verticalScrollPixels

                    // Check if we've accumulated enough to trigger a selection change
                    if (scrollAccumulator >= scrollThreshold) {
                        // Scroll up: increase habit index
                        if (selectedHabitIndex < 0) {
                            // From settings, go back to first habit
                            selectedHabitIndex = 0
                        } else if (selectedHabitIndex < numHabits - 1) {
                            selectedHabitIndex++
                        } else {
                            // Wrapped to next day
                            selectedHabitIndex = 0
                            if (selectedDayIndex < numDays - 1) {
                                selectedDayIndex++
                            }
                        }
                        scrollAccumulator = 0f
                    } else if (scrollAccumulator <= -scrollThreshold) {
                        // Scroll down: decrease habit index
                        if (selectedHabitIndex > 0) {
                            selectedHabitIndex--
                        } else if (selectedDayIndex == 0 && selectedHabitIndex == 0) {
                            // At first position, go to settings
                            selectedHabitIndex = -1
                        } else {
                            // Wrapped to previous day
                            selectedHabitIndex = numHabits - 1
                            if (selectedDayIndex > 0) {
                                selectedDayIndex--
                            }
                        }
                        scrollAccumulator = 0f
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable()
                .pointerInput("drag") {
                    detectDragGestures { _, dragAmount ->
                        // Accumulate vertical drag (negative dragAmount.y = drag up, positive = drag down)
                        touchScrollAccumulator += dragAmount.y

                        // Check if we've accumulated enough to trigger a selection change
                        if (touchScrollAccumulator >= touchScrollThreshold) {
                            // Drag down: decrease habit index (same as scroll down)
                            if (selectedHabitIndex > 0) {
                                selectedHabitIndex--
                            } else if (selectedDayIndex == 0 && selectedHabitIndex == 0) {
                                // At first position, go to settings
                                selectedHabitIndex = -1
                            } else {
                                // Wrapped to previous day
                                selectedHabitIndex = numHabits - 1
                                if (selectedDayIndex > 0) {
                                    selectedDayIndex--
                                }
                            }
                            touchScrollAccumulator = 0f
                        } else if (touchScrollAccumulator <= -touchScrollThreshold) {
                            // Drag up: increase habit index (same as scroll up)
                            if (selectedHabitIndex < 0) {
                                // From settings, go back to first habit
                                selectedHabitIndex = 0
                            } else if (selectedHabitIndex < numHabits - 1) {
                                selectedHabitIndex++
                            } else {
                                // Wrapped to next day
                                selectedHabitIndex = 0
                                if (selectedDayIndex < numDays - 1) {
                                    selectedDayIndex++
                                }
                            }
                            touchScrollAccumulator = 0f
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // Tap upper half: advance forward (increase habit index)
                            // Tap lower half: go backward (decrease habit index)
                            if (offset.y < size.height / 2) {
                                // Upper half - advance forward
                                if (selectedHabitIndex < 0) {
                                    // From settings, go back to first habit
                                    selectedHabitIndex = 0
                                } else if (selectedHabitIndex < numHabits - 1) {
                                    selectedHabitIndex++
                                } else {
                                    // Wrapped to next day
                                    selectedHabitIndex = 0
                                    if (selectedDayIndex < numDays - 1) {
                                        selectedDayIndex++
                                    }
                                }
                            } else {
                                // Lower half - go backward
                                if (selectedHabitIndex > 0) {
                                    selectedHabitIndex--
                                } else if (selectedDayIndex == 0 && selectedHabitIndex == 0) {
                                    // At first position, go to settings
                                    selectedHabitIndex = -1
                                } else {
                                    // Wrapped to previous day
                                    selectedHabitIndex = numHabits - 1
                                    if (selectedDayIndex > 0) {
                                        selectedDayIndex--
                                    }
                                }
                            }
                        },
                        onLongPress = {
                            // If in settings mode, navigate to management screen
                            if (selectedHabitIndex == -1) {
                                onNavigateToManagement()
                            } else {
                                // Find the selected habit's ID
                                val selectedHabit = habitData.habits.getOrNull(selectedHabitIndex)
                                if (selectedHabit != null) {
                                    // Check if habit type is implemented
                                    when (selectedHabit) {
                                        is Habit.TimeBasedHabit -> {
                                            throw NotImplementedError("Time-based habit completion not yet implemented")
                                        }
                                        is Habit.MultipleHabit -> {
                                            throw NotImplementedError("Multiple habit completion not yet implemented")
                                        }
                                        is Habit.BinaryHabit -> {
                                            // Find existing completion
                                            val existingCompletion = completions.find {
                                                it.habitId == selectedHabit.id && it.dayIndex == selectedDayIndex
                                            }

                                            // Toggle completion status
                                            val newCompletions = completions.toMutableList()
                                            if (existingCompletion != null) {
                                                newCompletions.remove(existingCompletion)
                                                val newStatus = when (existingCompletion.isCompleted) {
                                                    true -> false    // Completed -> Not completed
                                                    else -> true     // No data or not completed -> Completed
                                                }

                                                // Vibrate when marking habit as completed
                                                if (newStatus == true) {
                                                    vibrate(context)
                                                }

                                                newCompletions.add(
                                                    existingCompletion.copy(isCompleted = newStatus)
                                                )
                                            } else {
                                                // No existing entry, create new one as completed
                                                vibrate(context)
                                                newCompletions.add(
                                                    HabitCompletion(
                                                        habitId = selectedHabit.id,
                                                        dayIndex = selectedDayIndex,
                                                        isCompleted = true
                                                    )
                                                )
                                            }
                                            completions = newCompletions
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.width / 2f - outerMarginPx

            drawHabitTracker(
                center = center,
                maxRadius = maxRadius,
                innerMargin = innerMarginPx,
                habits = habitData.habits,
                completions = completions,
                darkBlue = darkBlue,
                paleBlue = paleBlue,
                paleGrey = paleGrey,
                lightGreen = lightGreen,
                darkGreen = darkGreen,
                selectedDayIndex = selectedDayIndex,
                selectedHabitIndex = selectedHabitIndex
            )
        }

        // Date display in center (only show when not in settings)
        if (selectedHabitIndex >= 0) {
            Text(
                text = "$selectedDayOfWeek\n$selectedDayOfMonth",
                fontSize = 10.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Habit abbreviation at top, right of center
        // Calculate position: vertically at middle of habit stack
        val stackRadiusDp = screenSize / 2 - outerMarginPx.dp - innerMarginPx.dp
        val middleOfHabitStackDp = innerMarginPx.dp + (stackRadiusDp * (numHabits + 2) / (2 * numHabits))
        val habitTextYOffset = - middleOfHabitStackDp
        val habitTextXOffset = screenSize / 2 + 2.dp

        if (selectedHabitIndex >= 0) {
            Text(
                text = selectedHabitAbbreviation,
                fontSize = 10.sp,
                textAlign = TextAlign.Left,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = habitTextXOffset, y = habitTextYOffset)
            )
        }

        // Settings text when in settings mode
        if (selectedHabitIndex == -1) {
            Text(
                text = "Settings",
                fontSize = 10.sp,
                textAlign = TextAlign.Left,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = habitTextXOffset, y = habitTextYOffset)
            )
        }
        }
    }
}
