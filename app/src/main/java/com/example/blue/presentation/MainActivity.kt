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
                    val context = LocalContext.current
                    val habitData = loadHabitData(context, LocalDate.now(), 10)

                    HabitManagementScreen(
                        habitData = habitData,
                        onAddHabit = {
                            // TODO: Navigate to add habit screen
                        },
                        onEditHabit = { habit ->
                            // TODO: Navigate to edit habit screen
                        }
                    )
                }
            }
        }
    }
}