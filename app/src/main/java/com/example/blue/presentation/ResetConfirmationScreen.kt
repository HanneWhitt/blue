// ResetConfirmationScreen.kt
package com.example.blue.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text

@Composable
fun ResetConfirmationScreen(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Are you sure you want to reset Blue? This will delete all your habit data, and cannot be reversed.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // Cancel button (cross)
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier.width(60.dp)
            ) {
                Text("✗", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Confirm button (tick)
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.primaryButtonColors(),
                modifier = Modifier.width(60.dp)
            ) {
                Text("✓", fontSize = 20.sp)
            }
        }
    }
}
