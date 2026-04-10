package com.example.d_tracker_android.feature.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.d_tracker_android.domain.model.TrackerData

@Composable
fun LastDataCard(data: TrackerData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Last Collected Data",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Battery: ${"%.0f".format(data.power)}%")
            Text(text = "Location: ${"%.5f".format(data.latitude)}, ${"%.5f".format(data.longitude)}")
            Text(text = "Steps: ${data.steps}")
        }
    }
}
