// HabitManagementScreen.kt
package com.example.blue.presentation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun HabitManagementScreen(
    habitData: HabitData,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onToggleEnabled: (Habit, Boolean) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onReorderHabits: (Int, Int) -> Unit = { _, _ -> }
) {
    // Sort habits by displayIndex and maintain local state for instant UI updates
    var sortedHabits by remember(habitData.habits) {
        mutableStateOf(habitData.habits.sortedBy { it.displayIndex })
    }

    val context = LocalContext.current

    // Drag-drop state with local swap callback
    val dragDropState = rememberDragDropState { fromIndex, toIndex ->
        // Immediately swap in local list for smooth UI
        if (fromIndex != toIndex && fromIndex in sortedHabits.indices && toIndex in sortedHabits.indices) {
            sortedHabits = sortedHabits.toMutableList().apply {
                val item = removeAt(fromIndex)
                add(toIndex, item)
            }
            // Persist the change to repository
            onReorderHabits(fromIndex, toIndex)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = dragDropState.listState,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            ListHeader {
                Text(
                    text = "Habits",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        itemsIndexed(sortedHabits, key = { _, habit -> habit.id }) { _, habit ->
            HabitItem(
                habit = habit,
                dragDropState = dragDropState,
                context = context,
                onEditHabit = onEditHabit,
                onToggleEnabled = onToggleEnabled
            )
        }

        item {
            Chip(
                label = {
                    Text(
                        text = "+",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                onClick = onAddHabit,
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        item {
            Chip(
                label = {
                    Text(
                        text = "Settings",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                onClick = onNavigateToSettings,
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun HabitItem(
    habit: Habit,
    dragDropState: DragDropState,
    context: Context,
    onEditHabit: (Habit) -> Unit,
    onToggleEnabled: (Habit, Boolean) -> Unit
) {
    val isEnabled = when (habit) {
        is Habit.BinaryHabit -> habit.enabled
        is Habit.TimeBasedHabit -> habit.enabled
        is Habit.MultipleHabit -> habit.enabled
    }

    val isDragging = dragDropState.draggingItemKey == habit.id
    val offsetY = if (isDragging) dragDropState.draggingItemOffset else 0f

    SplitToggleChip(
        label = {
            Text(
                text = habit.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        checked = isEnabled,
        onCheckedChange = { newEnabled ->
            if (!isDragging) {
                onToggleEnabled(habit, newEnabled)
            }
        },
        onClick = {
            if (!isDragging) {
                onEditHabit(habit)
            }
        },
        toggleControl = {
            Icon(
                imageVector = ToggleChipDefaults.switchIcon(checked = isEnabled),
                contentDescription = if (isEnabled) "Enabled" else "Disabled"
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = offsetY
                shadowElevation = if (isDragging) 8f else 0f
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .dragContainer(
                dragDropState = dragDropState,
                onDragStart = {
                    // Vibrate for tactile feedback
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    vibrator?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            it.vibrate(50)
                        }
                    }
                    dragDropState.onDragStart(habit.id)
                }
            )
    )
}
