package com.securenotes.app.presentation.screens.lock

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 4

/**
 * Shared PIN pad used for: app unlock, app PIN setup, vault unlock and vault setup.
 */
@Composable
fun PinScreen(
    emoji: String,
    title: String,
    subtitle: String,
    showBiometric: Boolean = false,
    onBiometric: () -> Unit = {},
    onSecondaryAction: (() -> Unit)? = null,
    secondaryLabel: String = "",
    onPinEntered: (String) -> Boolean,
    onSuccess: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val shake by animateFloatAsState(
        targetValue = if (error) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "shake",
    )

    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) {
            delay(90)
            if (onPinEntered(pin)) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onSuccess()
            } else {
                error = true
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(520)
                pin = ""
                error = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = emoji, fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(36.dp))

        // Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(horizontal = (shake * 6).dp),
        ) {
            repeat(PIN_LENGTH) { index ->
                val filled = index < pin.length
                Box(
                    Modifier
                        .size(if (filled) 20.dp else 16.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                error -> MaterialTheme.colorScheme.error
                                filled -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = if (error) "Incorrect PIN, try again 😅" else " ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(Modifier.height(20.dp))

        Keypad(
            onDigit = { digit ->
                if (pin.length < PIN_LENGTH) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    pin += digit
                }
            },
            onBackspace = { pin = pin.dropLast(1) },
            showBiometric = showBiometric,
            onBiometric = onBiometric,
        )

        if (onSecondaryAction != null) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onSecondaryAction) { Text(secondaryLabel) }
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    showBiometric: Boolean,
    onBiometric: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { digit -> KeypadKey(digit) { onDigit(digit) } }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (showBiometric) {
                    IconButton(onClick = onBiometric, modifier = Modifier.size(64.dp)) {
                        Icon(
                            Icons.Rounded.Fingerprint,
                            contentDescription = "Unlock with biometrics",
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            KeypadKey("0") { onDigit("0") }
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = onBackspace, modifier = Modifier.size(64.dp)) {
                    Icon(
                        Icons.Rounded.Backspace,
                        contentDescription = "Delete",
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(digit: String, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "keyScale",
    )

    LaunchedEffect(pressed) {
        if (pressed) {
            delay(120)
            pressed = false
        }
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable {
                pressed = true
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(0.dp),
        )
    }
}
