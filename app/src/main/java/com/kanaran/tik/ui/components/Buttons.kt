package com.kanaran.tik.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kanaran.tik.ui.theme.Ink

/** The loud, primary call-to-action: solid accent fill, thick ink border, hard offset shadow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    // Text/border sitting directly on the accent fill stays literal ink — yellow+near-black
    // reads fine in both themes, unlike structural borders which must flip with the theme.
    val onSurface = MaterialTheme.colorScheme.onSurface
    val fill = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (enabled) Ink else onSurface.copy(alpha = 0.3f)
    val contentColor = if (enabled) Ink else onSurface.copy(alpha = 0.35f)
    val shadowColor = if (enabled) onSurface else onSurface.copy(alpha = 0.25f)

    HardShadow(modifier = modifier.fillMaxWidth(), offset = 5.dp, shadowColor = shadowColor) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(fill)
                .border(BorderStroke(3.dp, borderColor))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    icon?.invoke()
                    Text(
                        text.uppercase(),
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** A quieter action: paper fill, same thick border, smaller hard shadow — both theme-reactive. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    HardShadow(modifier = modifier, offset = 3.dp) {
        Box(
            modifier = Modifier
                .height(46.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(2.5.dp, onSurface))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompositionLocalProvider(LocalContentColor provides onSurface) {
                    icon?.invoke()
                    Text(text, color = onSurface, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
