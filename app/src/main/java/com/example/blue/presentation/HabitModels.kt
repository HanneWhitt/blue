// HabitModels.kt
package com.example.blue.presentation

import androidx.compose.ui.graphics.Color

// Data classes
data class HabitCompletion(
    val habitId: Int,
    val dayIndex: Int, // 0-9 for 10 days
    val isCompleted: Boolean?  // null = no data, true = completed, false = not completed
)

sealed class Habit {
    abstract val id: Int
    abstract val name: String
    abstract val abbreviation: String

    data class BinaryHabit(
        override val id: Int,
        override val name: String,
        override val abbreviation: String,
        val color: Color
    ) : Habit()
}

data class HabitData(
    val habits: List<Habit>,
    val completions: List<HabitCompletion>
)
