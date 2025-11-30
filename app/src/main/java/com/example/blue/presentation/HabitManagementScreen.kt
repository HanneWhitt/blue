// HabitManagementScreen.kt
package com.example.blue.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun HabitManagementScreen(
    habitData: HabitData,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onToggleEnabled: (Habit, Boolean) -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ListHeader {
                Text(
                    text = "Manage Habits",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Chip(
                label = {
                    Text(
                        text = "Add Habit",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                onClick = onAddHabit,
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        items(habitData.habits.size) { index ->
            val habit = habitData.habits[index]
            val isEnabled = when (habit) {
                is Habit.BinaryHabit -> habit.enabled
                is Habit.TimeBasedHabit -> habit.enabled
                is Habit.MultipleHabit -> habit.enabled
            }

            SplitToggleChip(
                label = {
                    Text(
                        text = habit.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                secondaryLabel = {
                    Text(
                        text = habit.abbreviation,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                checked = isEnabled,
                onCheckedChange = { newEnabled ->
                    onToggleEnabled(habit, newEnabled)
                },
                onClick = {
                    onEditHabit(habit)
                },
                toggleControl = {
                    Icon(
                        imageVector = ToggleChipDefaults.switchIcon(checked = isEnabled),
                        contentDescription = if (isEnabled) "Enabled" else "Disabled"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
