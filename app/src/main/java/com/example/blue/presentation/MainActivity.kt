// MainActivity.kt
package com.example.blue.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.currentBackStackEntryAsState
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import java.time.LocalDate
import kotlin.math.roundToInt

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
        val navController = rememberSwipeDismissableNavController()
        val context = LocalContext.current
        var previousRoute by remember { mutableStateOf<String?>(null) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "tracker"
            ) {
                composable("tracker") {
                    val density = LocalDensity.current
                    val offsetX = remember { Animatable(0f) }
                    val wasFromManage = remember { previousRoute == "manage" }

                    LaunchedEffect(Unit) {
                        if (wasFromManage) {
                            // Animate in from left when coming back from manage
                            val screenWidth = with(density) { 300.dp.toPx() }
                            offsetX.snapTo(-screenWidth)
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 300)
                            )
                        } else {
                            // No animation, just ensure we're at position 0
                            offsetX.snapTo(0f)
                        }
                        previousRoute = "tracker"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    ) {
                        HabitTrackerDisplay(
                            onNavigateToManagement = {
                                navController.navigate("manage")
                            },
                            onNavigateToTimeEntry = { habitId, dayIndex ->
                                navController.navigate("time-entry/$habitId/$dayIndex")
                            }
                        )
                    }
                }

                composable("manage") {
                    val density = LocalDensity.current
                    val offsetX = remember { Animatable(0f) }
                    val wasFromTracker = remember { previousRoute == "tracker" }

                    LaunchedEffect(Unit) {
                        if (wasFromTracker) {
                            // Animate in from right
                            val screenWidth = with(density) { 300.dp.toPx() }
                            offsetX.snapTo(screenWidth)
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 300)
                            )
                        } else {
                            // No animation, just ensure we're at position 0
                            offsetX.snapTo(0f)
                        }
                        previousRoute = "manage"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    ) {
                        var habitData by remember { mutableStateOf(loadHabitData(context, LocalDate.now())) }

                        HabitManagementScreen(
                            habitData = habitData,
                            onAddHabit = {
                                navController.navigate("edit/-1")
                            },
                            onEditHabit = { habit ->
                                navController.navigate("edit/${habit.id}")
                            },
                            onToggleEnabled = { habit, newEnabled ->
                                val habitType = when (habit) {
                                    is Habit.BinaryHabit -> HabitType.BINARY
                                    is Habit.TimeBasedHabit -> HabitType.TIME_BASED
                                    is Habit.MultipleHabit -> HabitType.MULTIPLE
                                }
                                val completionsPerDay = if (habit is Habit.MultipleHabit) habit.completionsPerDay else 3
                                val targetTime = if (habit is Habit.TimeBasedHabit) habit.targetTime else "12:00"

                                updateHabit(
                                    context,
                                    LocalDate.now(),
                                    habit.id,
                                    habit.name,
                                    habit.abbreviation,
                                    habitType,
                                    completionsPerDay,
                                    targetTime,
                                    newEnabled
                                )

                                // Reload the data to refresh the UI
                                habitData = loadHabitData(context, LocalDate.now())
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onReorderHabits = { fromIndex, toIndex ->
                                habitData = reorderHabits(context, LocalDate.now(), fromIndex, toIndex)
                            }
                        )
                    }
                }

                composable("edit/{habitId}") { backStackEntry ->
                    val habitIdString = backStackEntry.arguments?.getString("habitId") ?: "-1"
                    val habitId = habitIdString.toIntOrNull() ?: -1

                    val habitData = loadHabitData(context, LocalDate.now())
                    val existingHabit = if (habitId >= 0) {
                        habitData.habits.find { it.id == habitId }
                    } else {
                        null
                    }

                    HabitEditScreen(
                        existingHabit = existingHabit,
                        onSave = { name, abbreviation, type, completionsPerDay, targetTime ->
                            if (existingHabit != null) {
                                // Update existing habit - preserve enabled state
                                val currentEnabled = when (existingHabit) {
                                    is Habit.BinaryHabit -> existingHabit.enabled
                                    is Habit.TimeBasedHabit -> existingHabit.enabled
                                    is Habit.MultipleHabit -> existingHabit.enabled
                                }
                                updateHabit(context, LocalDate.now(), habitId, name, abbreviation, type, completionsPerDay, targetTime, currentEnabled)
                            } else {
                                // Create new habit (enabled defaults to true)
                                createHabit(context, LocalDate.now(), name, abbreviation, type, completionsPerDay, targetTime)
                            }
                            navController.popBackStack()
                        },
                        onDelete = if (existingHabit != null) {
                            {
                                deleteHabit(context, LocalDate.now(), habitId)
                                navController.popBackStack()
                            }
                        } else {
                            null
                        }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        onNavigateToNumberOfDays = {
                            navController.navigate("number-of-days")
                        },
                        onReset = {
                            navController.navigate("reset-confirmation")
                        }
                    )
                }

                composable("number-of-days") {
                    val habitData = loadHabitData(context, LocalDate.now())
                    NumberOfDaysScreen(
                        currentNoDays = habitData.settings.noDays,
                        onSave = { newDays ->
                            navController.popBackStack()
                        }
                    )
                }

                composable("reset-confirmation") {
                    ResetConfirmationScreen(
                        onConfirm = {
                            // Delete the saved data file
                            resetHabitData(context)
                            // Navigate back to tracker (will reload from initial data)
                            navController.navigate("tracker") {
                                // Clear the back stack so user can't go back
                                popUpTo("tracker") { inclusive = true }
                            }
                        },
                        onCancel = {
                            navController.popBackStack()
                        }
                    )
                }

                composable("time-entry/{habitId}/{dayIndex}") { backStackEntry ->
                    val habitIdString = backStackEntry.arguments?.getString("habitId") ?: "0"
                    val dayIndexString = backStackEntry.arguments?.getString("dayIndex") ?: "0"
                    val habitId = habitIdString.toIntOrNull() ?: 0
                    val dayIndex = dayIndexString.toIntOrNull() ?: 0

                    val habitData = loadHabitData(context, LocalDate.now())
                    val habit = habitData.habits.find { it.id == habitId }
                    val currentCompletion = habitData.completions.find {
                        it.habitId == habitId && it.dayIndex == dayIndex
                    }

                    if (habit != null) {
                        TimeEntryScreen(
                            habit = habit,
                            dayIndex = dayIndex,
                            currentCompletionTime = currentCompletion?.completionTime,
                            onTimeSaved = { time ->
                                // Save the completion with time
                                val updatedCompletions = habitData.completions.toMutableList()
                                val existingIndex = updatedCompletions.indexOfFirst {
                                    it.habitId == habitId && it.dayIndex == dayIndex
                                }

                                val newCompletion = HabitCompletion(
                                    habitId = habitId,
                                    dayIndex = dayIndex,
                                    isCompleted = true,
                                    completionTime = time
                                )

                                if (existingIndex >= 0) {
                                    updatedCompletions[existingIndex] = newCompletion
                                } else {
                                    updatedCompletions.add(newCompletion)
                                }

                                val updatedHabitData = HabitData(habitData.habits, updatedCompletions, habitData.settings)
                                saveHabitDataToFile(context, updatedHabitData, LocalDate.now())

                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}