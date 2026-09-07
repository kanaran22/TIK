package com.kanaran.tik.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kanaran.tik.ui.theme.Ink

/**
 * A raw, offset drop shadow (no blur) — the signature brutalist "sticker" effect.
 * Draws a solid block behind [content], shifted down-right.
 */
@Composable
fun HardShadow(
    modifier: Modifier = Modifier,
    shadowColor: Color = MaterialTheme.colorScheme.onBackground,
    offset: Dp = 5.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = offset, y = offset)
                .background(shadowColor)
        )
        content()
    }
}

/** A small bordered square that stands in for a checkbox — no rounded Material ripple, just ink and accent. */
@Composable
fun BrutalistCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(30.dp)
            .background(if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
            .border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
        }
    }
}

/** A blunt, borderable toggle used for repeat types, weekdays, and trigger choices — no soft Material pill. */
@Composable
fun BrutalistToggleTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Selected content sits on the accent fill and stays literal ink; unselected content
    // sits on the plain surface and must follow the theme so it survives dark mode.
    val contentColor = if (selected) Ink else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .background(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
            .border(BorderStroke(2.5.dp, contentColor))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon?.invoke()
                Text(label.uppercase(), color = contentColor, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
