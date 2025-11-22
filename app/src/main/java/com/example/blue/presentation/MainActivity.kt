// MainActivity.kt
package com.example.blue.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import java.time.LocalDate

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
                    HabitTrackerDisplay(
                        onNavigateToManagement = { navController.navigate("manage") }
                    )
                }

                composable("manage") {
                    val habitData = loadHabitData(context, LocalDate.now(), 10)

                    HabitManagementScreen(
                        habitData = habitData,
                        onAddHabit = {
                            navController.navigate("edit/-1")
                        },
                        onEditHabit = { habit ->
                            navController.navigate("edit/${habit.id}")
                        }
                    )
                }

                composable("edit/{habitId}") { backStackEntry ->
                    val habitIdString = backStackEntry.arguments?.getString("habitId") ?: "-1"
                    val habitId = habitIdString.toIntOrNull() ?: -1

                    val habitData = loadHabitData(context, LocalDate.now(), 10)
                    val existingHabit = if (habitId >= 0) {
                        habitData.habits.find { it.id == habitId }
                    } else {
                        null
                    }

                    HabitEditScreen(
                        existingHabit = existingHabit,
                        onSave = { name, abbreviation, type ->
                            if (existingHabit != null) {
                                // Update existing habit
                                updateHabit(context, LocalDate.now(), 10, habitId, name, abbreviation, type)
                            } else {
                                // Create new habit
                                createHabit(context, LocalDate.now(), 10, name, abbreviation, type)
                            }
                            navController.popBackStack()
                        },
                        onDelete = if (existingHabit != null) {
                            {
                                deleteHabit(context, LocalDate.now(), 10, habitId)
                                navController.popBackStack()
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}