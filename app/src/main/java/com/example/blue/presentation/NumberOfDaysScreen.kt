// NumberOfDaysScreen.kt
package com.example.blue.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import java.time.LocalDate

@Composable
fun NumberOfDaysScreen(
    currentNoDays: Int,
    onSave: (Int) -> Unit
) {
    val context = LocalContext.current

    // Create a list of options from 3 to 30 days
    val dayOptions = (3..30).toList()
    val initialIndex = dayOptions.indexOf(currentNoDays).coerceAtLeast(0)

    val pickerState = rememberPickerState(
        initialNumberOfOptions = dayOptions.size,
        initiallySelectedOption = initialIndex
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Number of Days",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Picker(
            state = pickerState,
            modifier = Modifier.weight(1f)
        ) { optionIndex ->
            Text(
                text = dayOptions[optionIndex].toString(),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = {
                val selectedDays = dayOptions[pickerState.selectedOption]

                // Load current data and update settings
                val habitData = loadHabitData(context, LocalDate.now())
                val updatedSettings = habitData.settings.copy(noDays = selectedDays)
                val updatedHabitData = habitData.copy(settings = updatedSettings)

                // Save with new settings
                saveHabitDataToFile(context, updatedHabitData, LocalDate.now())

                onSave(selectedDays)
            },
            colors = ButtonDefaults.primaryButtonColors(),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Save")
        }
    }
}
