package com.securenotes.app.presentation.screens.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import com.securenotes.app.presentation.screens.lock.PinScreen
import com.securenotes.app.presentation.screens.notes.NotesScreen

/**
 * The Private Vault: gated by its own PIN, and visually distinguished from the
 * regular note list with a warmer tint and its own header.
 */
@Composable
fun VaultScreen(
    unlocked: Boolean,
    hasVaultPin: Boolean,
    onVerifyPin: (String) -> Boolean,
    onCreatePin: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        when {
            !hasVaultPin -> PinScreen(
                emoji = "🗝️",
                title = "Create a vault PIN",
                subtitle = "This is a second password, separate from your app lock.",
                onPinEntered = { pin -> onCreatePin(pin); true },
                onSuccess = {},
            )

            !unlocked -> PinScreen(
                emoji = "🔐",
                title = "Private Vault",
                subtitle = "Enter your vault PIN to reveal your most sensitive notes.",
                onPinEntered = onVerifyPin,
                onSuccess = {},
            )

            else -> NotesScreen(
                title = "Private Vault 🗝️",
                vaultMode = true,
                onOpenNote = onOpenNote,
                onNewNote = onNewNote,
                onOpenSearch = onOpenSearch,
                onOpenDrawer = onOpenDrawer,
            )
        }
    }
}
