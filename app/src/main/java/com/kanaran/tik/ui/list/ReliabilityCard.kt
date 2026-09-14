package com.kanaran.tik.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanaran.tik.ui.components.HardShadow
import com.kanaran.tik.ui.components.SecondaryButton
import com.kanaran.tik.ui.theme.Ink

/** Everything the "keep reminders on time" card needs; null means don't show it. */
data class ReliabilityCard(
    val isVivo: Boolean,
    val steps: List<String>,
    val canOpenAutostart: Boolean,
    val onOpenAppSettings: () -> Unit,
    val onOpenAutostart: () -> Unit,
    val onDismiss: () -> Unit
)

/**
 * Shown on phones whose battery management stops background apps (see BackgroundReliability).
 * The app can't lift those limits itself — this tells the user which settings to change and
 * takes them there.
 */
@Composable
fun ReliabilityCardView(card: ReliabilityCard, modifier: Modifier = Modifier) {
    HardShadow(modifier = modifier, offset = 4.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.secondary)
                        .border(BorderStroke(2.dp, Ink)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.BatteryAlert, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                }
                Text(
                    "KEEP REMINDERS ON TIME",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                if (card.isVivo) {
                    "Vivo phones pause apps in the background to save battery, so reminders can arrive late or not at all. These settings fix it:"
                } else {
                    "Your phone pauses apps in the background to save battery, which can delay reminders. These settings fix it:"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            card.steps.forEachIndexed { index, step ->
                Row(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "TIK SETTINGS", onClick = card.onOpenAppSettings, modifier = Modifier.weight(1f))
                if (card.canOpenAutostart) {
                    SecondaryButton(text = "AUTOSTART", onClick = card.onOpenAutostart, modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SecondaryButton(text = "DONE — HIDE THIS", onClick = card.onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
