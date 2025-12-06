// HabitModels.kt
package com.example.blue.presentation

import androidx.compose.ui.graphics.Color

// Data classes
data class HabitCompletion(
    val habitId: Int,
    val dayIndex: Int, // 0-9 for 10 days
    val isCompleted: Boolean?,  // null = no data, true = completed, false = not completed (for Binary/Time-based habits)
    val completionCount: Int? = null,  // For Multiple habits - number of times completed
    val completionTime: String? = null  // For Time-based habits - time when completed (HH:mm format)
)

sealed class Habit {
    abstract val id: Int
    abstract val name: String
    abstract val abbreviation: String
    abstract val displayIndex: Int

    data class BinaryHabit(
        override val id: Int,
        override val name: String,
        override val abbreviation: String,
        val color: Color,
        val enabled: Boolean = true,
        override val displayIndex: Int = id
    ) : Habit()

    data class TimeBasedHabit(
        override val id: Int,
        override val name: String,
        override val abbreviation: String,
        val color: Color,
        val targetTime: String = "12:00",  // Target time in HH:mm format
        val enabled: Boolean = true,
        override val displayIndex: Int = id
    ) : Habit()

    data class MultipleHabit(
        override val id: Int,
        override val name: String,
        override val abbreviation: String,
        val color: Color,
        val completionsPerDay: Int = 3,  // Default to 3 completions per day
        val enabled: Boolean = true,
        override val displayIndex: Int = id
    ) : Habit()
}

data class AppSettings(
    val noDays: Int = 10
)

data class HabitData(
    val habits: List<Habit>,
    val completions: List<HabitCompletion>,
    val settings: AppSettings = AppSettings()
)
