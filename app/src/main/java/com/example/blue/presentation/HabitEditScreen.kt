// HabitEditScreen.kt
package com.example.blue.presentation

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper

enum class HabitType {
    BINARY,
    TIME_BASED,
    MULTIPLE;

    override fun toString(): String {
        return when (this) {
            BINARY -> "Binary"
            TIME_BASED -> "Time-based"
            MULTIPLE -> "Multiple"
        }
    }

    fun toJsonString(): String {
        return when (this) {
            BINARY -> "Binary"
            TIME_BASED -> "Time-based"
            MULTIPLE -> "Multiple"
        }
    }

    companion object {
        fun fromJsonString(value: String): HabitType {
            return when (value) {
                "Binary" -> BINARY
                "Time-based" -> TIME_BASED
                "Multiple" -> MULTIPLE
                else -> BINARY
            }
        }
    }
}

@Composable
fun HabitEditScreen(
    existingHabit: Habit? = null,
    onSave: (name: String, abbreviation: String, type: HabitType, completionsPerDay: Int, targetTime: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditMode = existingHabit != null

    var habitName by remember { mutableStateOf(existingHabit?.name ?: "") }
    var habitAbbreviation by remember { mutableStateOf(existingHabit?.abbreviation ?: "") }
    var habitType by remember {
        mutableStateOf(
            when (existingHabit) {
                is Habit.BinaryHabit -> HabitType.BINARY
                is Habit.TimeBasedHabit -> HabitType.TIME_BASED
                is Habit.MultipleHabit -> HabitType.MULTIPLE
                null -> HabitType.BINARY
            }
        )
    }
    var completionsPerDay by remember {
        mutableStateOf(
            when (existingHabit) {
                is Habit.MultipleHabit -> existingHabit.completionsPerDay
                else -> 3
            }
        )
    }
    var targetTime by remember {
        mutableStateOf(
            when (existingHabit) {
                is Habit.TimeBasedHabit -> existingHabit.targetTime
                else -> "12:00"
            }
        )
    }
    var showTypeSelector by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Remote input launcher for habit name
    val nameInputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results: Bundle = RemoteInput.getResultsFromIntent(data)
            val newText: CharSequence? = results.getCharSequence("habit_name_input")
            habitName = newText?.toString() ?: habitName
        }
    }

    // Remote input launcher for abbreviation
    val abbreviationInputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.let { data ->
            val results: Bundle = RemoteInput.getResultsFromIntent(data)
            val newText: CharSequence? = results.getCharSequence("habit_abbr_input")
            habitAbbreviation = newText?.toString() ?: habitAbbreviation
        }
    }

    if (showTimePicker) {
        // Time picker screen for target time using Picker components
        val timeParts = targetTime.split(":")

        val hourState = rememberPickerState(
            initialNumberOfOptions = 24,
            initiallySelectedOption = timeParts[0].toInt()
        )

        val minuteState = rememberPickerState(
            initialNumberOfOptions = 60,
            initiallySelectedOption = timeParts[1].toInt()
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Target Time",
                    style = MaterialTheme.typography.caption1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour picker
                    Picker(
                        state = hourState,
                        modifier = Modifier
                            .size(48.dp, 100.dp)
                            .weight(1f)
                    ) { hour ->
                        Text(
                            text = String.format("%02d", hour),
                            style = MaterialTheme.typography.display3,
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.display3,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Minute picker
                    Picker(
                        state = minuteState,
                        modifier = Modifier
                            .size(48.dp, 100.dp)
                            .weight(1f)
                    ) { minute ->
                        Text(
                            text = String.format("%02d", minute),
                            style = MaterialTheme.typography.display3,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done button
                Button(
                    onClick = {
                        targetTime = String.format(
                            "%02d:%02d",
                            hourState.selectedOption,
                            minuteState.selectedOption
                        )
                        showTimePicker = false
                    }
                ) {
                    Text("✓")
                }
            }
        }
    } else if (showTypeSelector) {
        // Type selector screen
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Select Type",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Chip(
                    label = { Text("Binary") },
                    onClick = {
                        habitType = HabitType.BINARY
                        showTypeSelector = false
                    },
                    colors = if (habitType == HabitType.BINARY) {
                        ChipDefaults.primaryChipColors()
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item {
                Chip(
                    label = { Text("Time-based") },
                    onClick = {
                        habitType = HabitType.TIME_BASED
                        showTypeSelector = false
                    },
                    colors = if (habitType == HabitType.TIME_BASED) {
                        ChipDefaults.primaryChipColors()
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            item {
                Chip(
                    label = { Text("Multiple") },
                    onClick = {
                        habitType = HabitType.MULTIPLE
                        showTypeSelector = false
                    },
                    colors = if (habitType == HabitType.MULTIPLE) {
                        ChipDefaults.primaryChipColors()
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    } else {
        // Main edit screen
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        text = if (isEditMode) "Edit Habit" else "Create Habit",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Name input
            item {
                Chip(
                    label = {
                        Text(
                            text = if (habitName.isEmpty()) "Set Name" else habitName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        val remoteInputs = listOf(
                            RemoteInput.Builder("habit_name_input")
                                .setLabel("Habit Name")
                                .build()
                        )
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                        nameInputLauncher.launch(intent)
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Abbreviation input
            item {
                Chip(
                    label = {
                        Text(
                            text = if (habitAbbreviation.isEmpty()) "Set Abbreviation" else habitAbbreviation,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        val remoteInputs = listOf(
                            RemoteInput.Builder("habit_abbr_input")
                                .setLabel("Abbreviation")
                                .build()
                        )
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
                        abbreviationInputLauncher.launch(intent)
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Type selector
            item {
                Chip(
                    label = {
                        Text(
                            text = "Type: ${habitType}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        showTypeSelector = true
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Target time (only show for Time-based type)
            if (habitType == HabitType.TIME_BASED) {
                item {
                    Chip(
                        label = {
                            Text(
                                text = "Target: $targetTime",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            showTimePicker = true
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Completions per day (only show for Multiple type)
            if (habitType == HabitType.MULTIPLE) {
                item {
                    Chip(
                        label = {
                            Text(
                                text = "Per day: $completionsPerDay",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            // Cycle through 1-5 completions per day
                            completionsPerDay = if (completionsPerDay >= 5) 1 else completionsPerDay + 1
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Save/Create button
            item {
                Chip(
                    label = {
                        Text(
                            text = if (isEditMode) "Apply" else "Create Habit",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        if (habitName.isNotEmpty() && habitAbbreviation.isNotEmpty()) {
                            onSave(habitName, habitAbbreviation, habitType, completionsPerDay, targetTime)
                        }
                    },
                    colors = ChipDefaults.primaryChipColors(),
                    enabled = habitName.isNotEmpty() && habitAbbreviation.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Delete button (only in edit mode)
            if (isEditMode && onDelete != null) {
                item {
                    Chip(
                        label = {
                            Text(
                                text = "Delete Habit",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = onDelete,
                        colors = ChipDefaults.chipColors(
                            backgroundColor = Color.Red,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
