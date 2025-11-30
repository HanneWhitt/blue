// SettingsScreen.kt
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
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text

@Composable
fun SettingsScreen(
    onNavigateToNumberOfDays: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ListHeader {
                Text(
                    text = "Settings",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Chip(
                label = {
                    Text(
                        text = "Number of days displayed",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                onClick = onNavigateToNumberOfDays,
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
                        text = "Reset",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                onClick = onReset,
                colors = ChipDefaults.primaryChipColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
