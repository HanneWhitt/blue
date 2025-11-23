// TimeEntryScreen.kt
package com.example.blue.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import java.time.LocalTime

@Composable
fun TimeEntryScreen(
    habit: Habit,
    dayIndex: Int,
    currentCompletionTime: String?,
    onTimeSaved: (String) -> Unit
) {
    // Parse current time or use current local time as default
    val defaultTime = if (currentCompletionTime != null) {
        val parts = currentCompletionTime.split(":")
        LocalTime.of(parts[0].toInt(), parts[1].toInt())
    } else {
        LocalTime.now()
    }

    val hourState = rememberPickerState(
        initialNumberOfOptions = 24,
        initiallySelectedOption = defaultTime.hour
    )

    val minuteState = rememberPickerState(
        initialNumberOfOptions = 60,
        initiallySelectedOption = defaultTime.minute
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
                text = habit.name,
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

            // Save button
            Button(
                onClick = {
                    val timeString = String.format(
                        "%02d:%02d",
                        hourState.selectedOption,
                        minuteState.selectedOption
                    )
                    onTimeSaved(timeString)
                }
            ) {
                Text("✓")
            }
        }
    }
}
